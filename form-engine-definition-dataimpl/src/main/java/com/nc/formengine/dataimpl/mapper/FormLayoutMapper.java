package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.dataimpl.entity.FormLayout;
import com.nc.formengine.model.dto.FormLayoutDTO;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class FormLayoutMapper {

    private final FieldLayoutMapper fieldLayoutMapper;

    public FormLayoutMapper(FieldLayoutMapper fieldLayoutMapper) {
        this.fieldLayoutMapper = fieldLayoutMapper;
    }

    public FormLayoutDTO toDTO(FormLayout entity) {
        if (entity == null) {
            return null;
        }

        return FormLayoutDTO.builder()
                .id(entity.getId())
                .formDefinitionId(entity.getFormDefinition() != null ? entity.getFormDefinition().getId() : null)
                .deviceType(entity.getDeviceType())
                .customDeviceName(entity.getCustomDeviceName())
                .fieldLayouts(entity.getFieldLayouts() != null ?
                    entity.getFieldLayouts().stream()
                        .map(fieldLayoutMapper::toDTO)
                        .collect(Collectors.toList()) : null)
                .build();
    }

    public FormLayout toEntity(FormLayoutDTO dto) {
        if (dto == null) {
            return null;
        }

        FormLayout entity = new FormLayout();
        entity.setId(dto.getId());
        entity.setDeviceType(dto.getDeviceType());
        entity.setCustomDeviceName(dto.getCustomDeviceName());

        return entity;
    }
}
