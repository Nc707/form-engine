package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FormLayoutService;
import com.nc.formengine.data.dao.FormLayoutDao;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.exception.FormLayoutNotFoundException;
import com.nc.formengine.model.exception.ValidationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class FormLayoutServiceImpl implements FormLayoutService {

    private final FormLayoutDao formLayoutDao;
    private final DefinitionMutationGuard guard;

    public FormLayoutServiceImpl(FormLayoutDao formLayoutDao, DefinitionMutationGuard guard) {
        this.formLayoutDao = formLayoutDao;
        this.guard = guard;
    }

    @Override
    public FormLayoutDTO createLayout(FormLayoutDTO layoutDTO) {
        FormDefinitionDTO form = guard.requireDraft(layoutDTO.getFormDefinitionId());
        validateLayout(form, layoutDTO);

        FormLayoutDTO saved = formLayoutDao.save(layoutDTO);
        log.info("Created layout {} for form {} with device type {}",
                saved.getId(), layoutDTO.getFormDefinitionId(), layoutDTO.getDeviceType());

        return saved;
    }

    @Override
    public FormLayoutDTO updateLayout(Long layoutId, FormLayoutDTO layoutDTO) {
        FormLayoutDTO existingLayout = formLayoutDao.findById(layoutId)
                .orElseThrow(() -> new FormLayoutNotFoundException(layoutId));
        guard.requireDraft(existingLayout.getFormDefinitionId());
        FormDefinitionDTO form = guard.requireDraft(layoutDTO.getFormDefinitionId());
        validateLayout(form, layoutDTO);

        layoutDTO.setId(layoutId);

        FormLayoutDTO updated = formLayoutDao.save(layoutDTO);
        log.info("Updated layout {}", layoutId);

        return updated;
    }

    @Override
    public void deleteLayout(Long layoutId) {
        formLayoutDao.findById(layoutId)
                .ifPresent(layout -> guard.requireDraft(layout.getFormDefinitionId()));
        formLayoutDao.deleteById(layoutId);
        log.info("Deleted layout {}", layoutId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormLayoutDTO> getLayoutById(Long layoutId) {
        return formLayoutDao.findById(layoutId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormLayoutDTO> getLayoutsByFormDefinition(Long formDefinitionId) {
        return formLayoutDao.findByFormDefinitionId(formDefinitionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormLayoutDTO> getLayoutByFormAndDevice(Long formDefinitionId, DeviceType deviceType) {
        return formLayoutDao.findByFormDefinitionIdAndDeviceType(formDefinitionId, deviceType);
    }

    /**
     * Refuses a layout that places fields of another form.
     *
     * <p>A layout that says nothing about some of the form's fields is <em>not</em> a violation: the
     * renderer appends whatever the layout forgot, on purpose, so that a field can never become
     * unanswerable by omission. Only fields that are not the form's at all are a broken layout.
     */
    private void validateLayout(FormDefinitionDTO formDefinition, FormLayoutDTO layoutDTO) {
        if (layoutDTO.getFieldLayouts() != null && !layoutDTO.getFieldLayouts().isEmpty()) {
            Set<Long> formFieldIds = formDefinition.getFields().stream()
                    .map(FieldDefinitionDTO::getId)
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
