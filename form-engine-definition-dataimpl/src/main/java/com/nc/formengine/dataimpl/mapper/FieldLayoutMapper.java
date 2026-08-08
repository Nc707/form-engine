package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.dataimpl.entity.FieldLayout;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps {@link FieldLayout} to its DTO.
 *
 * <p>{@code customProperties} is persisted as a {@code Map<String, String>} but exposed as a
 * {@code Map<String, Object>}, so each value is serialized to JSON individually. That is what lets
 * a structured value such as {@code {"toolbar": ["bold", "italic"]}} survive a round trip.
 */
@Component
public class FieldLayoutMapper {

    private final ObjectMapper objectMapper;

    public FieldLayoutMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FieldLayoutDTO toDTO(FieldLayout entity) {
        if (entity == null) {
            return null;
        }

        Map<String, Object> customProps = new HashMap<>();
        if (entity.getCustomProperties() != null) {
            entity.getCustomProperties().forEach((key, value) -> {
                try {
                    customProps.put(key, objectMapper.readValue(value, Object.class));
                } catch (JacksonException e) {
                    customProps.put(key, value);
                }
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
                try {
                    customProps.put(key, objectMapper.writeValueAsString(value));
                } catch (JacksonException e) {
                    customProps.put(key, String.valueOf(value));
                }
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
