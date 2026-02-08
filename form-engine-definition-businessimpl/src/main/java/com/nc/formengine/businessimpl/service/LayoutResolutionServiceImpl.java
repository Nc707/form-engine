package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.LayoutResolutionService;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.entity.FormLayout;
import com.nc.formengine.dataimpl.mapper.FormLayoutMapper;
import com.nc.formengine.dataimpl.repository.FormLayoutRepository;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class LayoutResolutionServiceImpl implements LayoutResolutionService {

    private final FormLayoutRepository formLayoutRepository;
    private final FormRepository formRepository;
    private final FormLayoutMapper formLayoutMapper;

    public LayoutResolutionServiceImpl(FormLayoutRepository formLayoutRepository,
                                       FormRepository formRepository,
                                       FormLayoutMapper formLayoutMapper) {
        this.formLayoutRepository = formLayoutRepository;
        this.formRepository = formRepository;
        this.formLayoutMapper = formLayoutMapper;
    }

    @Override
    public Optional<FormLayoutDTO> resolveLayout(Long formDefinitionId, DeviceType deviceType) {
        Optional<FormLayout> exactMatch = formLayoutRepository
                .findByFormDefinitionIdAndDeviceType(formDefinitionId, deviceType);
        
        if (exactMatch.isPresent()) {
            log.debug("Found exact layout match for form {} and device {}", formDefinitionId, deviceType);
            return exactMatch.map(formLayoutMapper::toDTO);
        }

        Optional<FormLayout> fallback = formLayoutRepository
                .findByFormDefinitionIdAndDeviceTypeIsNull(formDefinitionId);
        
        if (fallback.isPresent()) {
            log.debug("Using fallback layout for form {} (no device type)", formDefinitionId);
            return fallback.map(formLayoutMapper::toDTO);
        }

        if (deviceType != null) {
            Optional<FormLayout> closestMatch = findClosestLayout(formDefinitionId, deviceType);
            if (closestMatch.isPresent()) {
                log.debug("Using closest layout match for form {} and device {}", formDefinitionId, deviceType);
                return closestMatch.map(formLayoutMapper::toDTO);
            }
        }

        log.warn("No layout found for form {} and device {}", formDefinitionId, deviceType);
        return Optional.empty();
    }

    @Override
    public FormLayoutDTO resolveLayoutOrDefault(Long formDefinitionId, DeviceType deviceType) {
        return resolveLayout(formDefinitionId, deviceType)
                .orElseGet(() -> createDefaultLayout(formDefinitionId));
    }

    private Optional<FormLayout> findClosestLayout(Long formDefinitionId, DeviceType requestedType) {
        if (requestedType == DeviceType.TABLET) {
            return formLayoutRepository.findByFormDefinitionIdAndDeviceType(formDefinitionId, DeviceType.DESKTOP)
                    .or(() -> formLayoutRepository.findByFormDefinitionIdAndDeviceType(formDefinitionId, DeviceType.MOBILE));
        }
        
        if (requestedType == DeviceType.MOBILE) {
            return formLayoutRepository.findByFormDefinitionIdAndDeviceType(formDefinitionId, DeviceType.TABLET);
        }
        
        if (requestedType == DeviceType.DESKTOP) {
            return formLayoutRepository.findByFormDefinitionIdAndDeviceType(formDefinitionId, DeviceType.TABLET);
        }
        
        return Optional.empty();
    }

    private FormLayoutDTO createDefaultLayout(Long formDefinitionId) {
        log.info("Creating default layout for form {}", formDefinitionId);
        
        FormDefinition formDefinition = formRepository.findById(formDefinitionId)
                .orElseThrow(() -> new RuntimeException("Form definition not found: " + formDefinitionId));

        FormLayoutDTO defaultLayout = FormLayoutDTO.builder()
                .formDefinitionId(formDefinitionId)
                .fieldLayouts(new ArrayList<>())
                .build();

        int row = 0;
        for (var field : formDefinition.getFields()) {
            FieldLayoutDTO fieldLayout = FieldLayoutDTO.builder()
                    .fieldDefinitionId(field.getId())
                    .row(row)
                    .column(0)
                    .colspan(12)
                    .rowspan(1)
                    .visible(true)
                    .build();
            defaultLayout.getFieldLayouts().add(fieldLayout);
            row++;
        }

        return defaultLayout;
    }
}
