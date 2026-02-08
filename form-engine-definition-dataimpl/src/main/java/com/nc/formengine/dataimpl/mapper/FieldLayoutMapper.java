package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.dataimpl.entity.FieldLayout;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FieldLayoutMapper {

    public FieldLayoutDTO toDTO(FieldLayout entity) {
        if (entity == null) {
            return null;
        }

        Map<String, Object> customProps = new HashMap<>();
        if (entity.getCustomProperties() != null) {
            entity.getCustomProperties().forEach((key, value) -> {
                customProps.put(key, value);
            });
        }

        return FieldLayoutDTO.builder()
                .id(entity.getId())
                .formLayoutId(entity.getFormLayout() != null ? entity.getFormLayout().getId() : null)
                .fieldDefinitionId(entity.getFieldDefinition() != null ? entity.getFieldDefinition().getId() : null)
                .row(entity.getRow())
                .column(entity.getColumn())
                .colspan(entity.getColspan())
                .rowspan(entity.getRowspan())
                .componentType(entity.getComponentType())
                .customProperties(customProps)
                .visible(entity.getVisible())
                .build();
    }

    public FieldLayout toEntity(FieldLayoutDTO dto) {
        if (dto == null) {
            return null;
        }

        Map<String, String> customProps = new HashMap<>();
        if (dto.getCustomProperties() != null) {
            dto.getCustomProperties().forEach((key, value) -> {
                customProps.put(key, value != null ? value.toString() : null);
            });
        }

        FieldLayout entity = new FieldLayout();
        entity.setId(dto.getId());
        entity.setRow(dto.getRow());
        entity.setColumn(dto.getColumn());
        entity.setColspan(dto.getColspan());
        entity.setRowspan(dto.getRowspan());
        entity.setComponentType(dto.getComponentType());
        entity.setCustomProperties(customProps);
        entity.setVisible(dto.getVisible());

        return entity;
    }
}
