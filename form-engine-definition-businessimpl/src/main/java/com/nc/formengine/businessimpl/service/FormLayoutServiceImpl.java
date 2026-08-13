package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FormLayoutService;
import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FieldLayout;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.entity.FormLayout;
import com.nc.formengine.dataimpl.mapper.FieldLayoutMapper;
import com.nc.formengine.dataimpl.mapper.FormLayoutMapper;
import com.nc.formengine.dataimpl.repository.FieldRepository;
import com.nc.formengine.dataimpl.repository.FormLayoutRepository;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.exception.FieldDefinitionNotFoundException;
import com.nc.formengine.model.exception.FormDefinitionNotFoundException;
import com.nc.formengine.model.exception.FormLayoutNotFoundException;
import com.nc.formengine.model.exception.ValidationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class FormLayoutServiceImpl implements FormLayoutService {

    private final FormLayoutRepository formLayoutRepository;
    private final FormRepository formRepository;
    private final FieldRepository fieldRepository;
    private final FormLayoutMapper formLayoutMapper;
    private final FieldLayoutMapper fieldLayoutMapper;

    public FormLayoutServiceImpl(FormLayoutRepository formLayoutRepository,
                                 FormRepository formRepository,
                                 FieldRepository fieldRepository,
                                 FormLayoutMapper formLayoutMapper,
                                 FieldLayoutMapper fieldLayoutMapper) {
        this.formLayoutRepository = formLayoutRepository;
        this.formRepository = formRepository;
        this.fieldRepository = fieldRepository;
        this.formLayoutMapper = formLayoutMapper;
        this.fieldLayoutMapper = fieldLayoutMapper;
    }

    @Override
    public FormLayoutDTO createLayout(FormLayoutDTO layoutDTO) {
        validateLayout(layoutDTO);

        FormDefinition formDefinition = formRepository.findById(layoutDTO.getFormDefinitionId())
                .orElseThrow(() -> new FormDefinitionNotFoundException(layoutDTO.getFormDefinitionId()));

        FormLayout formLayout = formLayoutMapper.toEntity(layoutDTO);
        formLayout.setFormDefinition(formDefinition);

        if (layoutDTO.getFieldLayouts() != null) {
            List<FieldLayout> fieldLayouts = new ArrayList<>();
            for (FieldLayoutDTO fieldLayoutDTO : layoutDTO.getFieldLayouts()) {
                FieldDefinition fieldDefinition = fieldRepository.findById(fieldLayoutDTO.getFieldDefinitionId())
                        .orElseThrow(() -> new FieldDefinitionNotFoundException(fieldLayoutDTO.getFieldDefinitionId()));

                FieldLayout fieldLayout = fieldLayoutMapper.toEntity(fieldLayoutDTO);
                fieldLayout.setFormLayout(formLayout);
                fieldLayout.setFieldDefinition(fieldDefinition);
                fieldLayouts.add(fieldLayout);
            }
            formLayout.setFieldLayouts(fieldLayouts);
        }

        FormLayout saved = formLayoutRepository.save(formLayout);
        log.info("Created layout {} for form {} with device type {}", 
                saved.getId(), layoutDTO.getFormDefinitionId(), layoutDTO.getDeviceType());
        
        return formLayoutMapper.toDTO(saved);
    }

    @Override
    public FormLayoutDTO updateLayout(Long layoutId, FormLayoutDTO layoutDTO) {
        validateLayout(layoutDTO);

        FormLayout existingLayout = formLayoutRepository.findById(layoutId)
                .orElseThrow(() -> new FormLayoutNotFoundException(layoutId));

        existingLayout.setDeviceType(layoutDTO.getDeviceType());

        existingLayout.getFieldLayouts().clear();

        if (layoutDTO.getFieldLayouts() != null) {
            for (FieldLayoutDTO fieldLayoutDTO : layoutDTO.getFieldLayouts()) {
                FieldDefinition fieldDefinition = fieldRepository.findById(fieldLayoutDTO.getFieldDefinitionId())
                        .orElseThrow(() -> new FieldDefinitionNotFoundException(fieldLayoutDTO.getFieldDefinitionId()));

                FieldLayout fieldLayout = fieldLayoutMapper.toEntity(fieldLayoutDTO);
                fieldLayout.setFormLayout(existingLayout);
                fieldLayout.setFieldDefinition(fieldDefinition);
                existingLayout.getFieldLayouts().add(fieldLayout);
            }
        }

        FormLayout updated = formLayoutRepository.save(existingLayout);
        log.info("Updated layout {}", layoutId);
        
        return formLayoutMapper.toDTO(updated);
    }

    @Override
    public void deleteLayout(Long layoutId) {
        formLayoutRepository.deleteById(layoutId);
        log.info("Deleted layout {}", layoutId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormLayoutDTO> getLayoutById(Long layoutId) {
        return formLayoutRepository.findById(layoutId)
                .map(formLayoutMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormLayoutDTO> getLayoutsByFormDefinition(Long formDefinitionId) {
        return formLayoutRepository.findByFormDefinitionId(formDefinitionId).stream()
                .map(formLayoutMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormLayoutDTO> getLayoutByFormAndDevice(Long formDefinitionId, DeviceType deviceType) {
        if (deviceType == null) {
            return formLayoutRepository.findByFormDefinitionIdAndDeviceTypeIsNull(formDefinitionId)
                    .map(formLayoutMapper::toDTO);
        }
        return formLayoutRepository.findByFormDefinitionIdAndDeviceType(formDefinitionId, deviceType)
                .map(formLayoutMapper::toDTO);
    }

    /**
     * Refuses a layout that places fields of another form.
     *
     * <p>A layout that says nothing about some of the form's fields is <em>not</em> a violation: the
     * renderer appends whatever the layout forgot, on purpose, so that a field can never become
     * unanswerable by omission. Only fields that are not the form's at all are a broken layout.
     */
    private void validateLayout(FormLayoutDTO layoutDTO) {
        if (layoutDTO.getFormDefinitionId() == null) {
            throw new IllegalArgumentException("Form definition ID is required");
        }

        FormDefinition formDefinition = formRepository.findById(layoutDTO.getFormDefinitionId())
                .orElseThrow(() -> new FormDefinitionNotFoundException(layoutDTO.getFormDefinitionId()));

        if (layoutDTO.getFieldLayouts() != null && !layoutDTO.getFieldLayouts().isEmpty()) {
            Set<Long> formFieldIds = formDefinition.getFields().stream()
                    .map(FieldDefinition::getId)
                    .collect(Collectors.toSet());

            Set<Long> layoutFieldIds = layoutDTO.getFieldLayouts().stream()
                    .map(FieldLayoutDTO::getFieldDefinitionId)
                    .collect(Collectors.toSet());

            Set<Long> invalidFields = new HashSet<>(layoutFieldIds);
            invalidFields.removeAll(formFieldIds);

            if (!invalidFields.isEmpty()) {
                // Syntactically valid request, but the fields belong to a different form: 422.
                List<String> reasons = invalidFields.stream()
                        .sorted()
                        .map(fieldId -> "Field " + fieldId + " does not belong to form "
                                + layoutDTO.getFormDefinitionId())
                        .toList();
                throw new ValidationFailedException(
                        "Layout contains invalid field IDs: " + invalidFields, reasons);
            }
        }
    }
}
