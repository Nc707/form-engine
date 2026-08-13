package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FieldRestriction;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps {@link FieldRestriction} to its DTO.
 *
 * <p>{@code parameters} is persisted as a {@code Map<String, String>} but exposed as a
 * {@code Map<String, Object>}, so each value is serialized to JSON individually. That is what keeps
 * {@code {"minLength": 5}} an integer across a round trip instead of turning it into the string
 * {@code "5"}.
 */
@Component
public class FieldRestrictionMapper {

    private final ObjectMapper objectMapper;

    public FieldRestrictionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FieldRestrictionDTO toDTO(FieldRestriction entity) {
        if (entity == null) {
            return null;
        }

        Map<String, Object> parameters = new HashMap<>();
        if (entity.getParameters() != null) {
            entity.getParameters().forEach((key, value) -> {
                try {
                    parameters.put(key, objectMapper.readValue(value, Object.class));
                } catch (JacksonException e) {
                    parameters.put(key, value);
                }
            });
        }

        return FieldRestrictionDTO.builder()
                .id(entity.getId())
                .fieldDefinitionId(entity.getFieldDefinition() != null
                    ? entity.getFieldDefinition().getId() : null)
                .restrictionType(entity.getType())
                .parameters(parameters)
                .errorMessage(entity.getErrorMessage())
                .orderIndex(entity.getOrderIndex())
                .build();
    }

    public FieldRestriction toEntity(FieldRestrictionDTO dto) {
        if (dto == null) {
            return null;
        }

        FieldRestriction entity = new FieldRestriction();
        entity.setId(dto.getId());
        updateEntity(dto, entity);

        return entity;
    }

    public void updateEntity(FieldRestrictionDTO dto, FieldRestriction entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setType(dto.getRestrictionType());
        entity.setErrorMessage(dto.getErrorMessage());
        entity.setOrderIndex(dto.getOrderIndex());
        entity.setParameters(toStoredParameters(dto.getParameters()));
    }

    public void setFieldDefinition(FieldRestriction entity, FieldDefinition fieldDefinition) {
        if (entity != null) {
            entity.setFieldDefinition(fieldDefinition);
        }
    }

    private Map<String, String> toStoredParameters(Map<String, Object> parameters) {
        Map<String, String> stored = new HashMap<>();
        if (parameters == null) {
            return stored;
        }

        parameters.forEach((key, value) -> {
            try {
                stored.put(key, objectMapper.writeValueAsString(value));
            } catch (JacksonException e) {
                stored.put(key, String.valueOf(value));
            }
        });
        return stored;
    }
}
