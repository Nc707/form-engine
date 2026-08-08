package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.specification.RestrictionTypeRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class FieldDefinitionMapper {

    public FieldDefinitionDTO toDTO(FieldDefinition entity) {
        if (entity == null) {
            return null;
        }

        return FieldDefinitionDTO.builder()
                .id(entity.getId())
                .formDefinitionId(entity.getFormDefinition() != null ? 
                    entity.getFormDefinition().getId() : null)
                .name(entity.getName())
                .label(entity.getLabel())
                .type(entity.getType() != null ? 
                    com.nc.formengine.model.enums.FieldType.valueOf(entity.getType().name()) : null)
                .orderIndex(entity.getOrderIndex())
                .required(entity.getRequired())
                .restrictions(toRestrictions(entity))
                .build();
    }

    public FieldDefinition toEntity(FieldDefinitionDTO dto) {
        if (dto == null) {
            return null;
        }

        FieldDefinition entity = new FieldDefinition();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setLabel(dto.getLabel());
        entity.setType(dto.getType() != null ? 
            com.nc.formengine.data.enums.FieldType.valueOf(dto.getType().name()) : null);
        entity.setOrderIndex(dto.getOrderIndex());
        entity.setRequired(dto.getRequired());
        applyRestrictions(dto.getRestrictions(), entity);

        return entity;
    }

    public void updateEntity(FieldDefinitionDTO dto, FieldDefinition entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setLabel(dto.getLabel());
        entity.setType(dto.getType() != null ? 
            com.nc.formengine.data.enums.FieldType.valueOf(dto.getType().name()) : null);
        entity.setOrderIndex(dto.getOrderIndex());
        entity.setRequired(dto.getRequired());
        applyRestrictions(dto.getRestrictions(), entity);
    }

    public void setFormDefinition(FieldDefinition entity, FormDefinition formDefinition) {
        if (entity != null) {
            entity.setFormDefinition(formDefinition);
        }
    }

    /**
     * Expands the inline validation columns of the entity into restriction DTOs.
     * Parameter keys follow the convention used by the specification implementations.
     */
    private List<FieldRestrictionDTO> toRestrictions(FieldDefinition entity) {
        List<FieldRestrictionDTO> restrictions = new ArrayList<>();

        addRestriction(restrictions, entity, RestrictionType.MIN_LENGTH, "minLength", entity.getMinLength());
        addRestriction(restrictions, entity, RestrictionType.MAX_LENGTH, "maxLength", entity.getMaxLength());
        addRestriction(restrictions, entity, RestrictionType.MIN_VALUE, "minValue", entity.getMinValue());
        addRestriction(restrictions, entity, RestrictionType.MAX_VALUE, "maxValue", entity.getMaxValue());
        addRestriction(restrictions, entity, RestrictionType.PATTERN, "pattern", entity.getRegexPattern());

        return restrictions;
    }

    private void addRestriction(List<FieldRestrictionDTO> restrictions, FieldDefinition entity,
                                RestrictionType type, String parameterName, Object value) {
        if (value == null) {
            return;
        }

        restrictions.add(FieldRestrictionDTO.builder()
                .fieldDefinitionId(entity.getId())
                .restrictionType(type)
                .parameters(Map.of(parameterName, value))
                .applicableFieldTypes(RestrictionTypeRegistry.getApplicableFieldTypes(type))
                .orderIndex(restrictions.size())
                .build());
    }

    /**
     * Collapses restriction DTOs back into the inline validation columns of the entity.
     * Restrictions absent from the list clear their corresponding column.
     */
    private void applyRestrictions(List<FieldRestrictionDTO> restrictions, FieldDefinition entity) {
        entity.setMinLength(null);
        entity.setMaxLength(null);
        entity.setMinValue(null);
        entity.setMaxValue(null);
        entity.setRegexPattern(null);

        if (restrictions == null) {
            return;
        }

        for (FieldRestrictionDTO restriction : restrictions) {
            if (restriction == null || restriction.getRestrictionType() == null) {
                continue;
            }

            Map<String, Object> parameters = restriction.getParameters();
            switch (restriction.getRestrictionType()) {
                case MIN_LENGTH -> entity.setMinLength(intParameter(parameters, "minLength"));
                case MAX_LENGTH -> entity.setMaxLength(intParameter(parameters, "maxLength"));
                case MIN_VALUE -> entity.setMinValue(doubleParameter(parameters, "minValue"));
                case MAX_VALUE -> entity.setMaxValue(doubleParameter(parameters, "maxValue"));
                case PATTERN -> entity.setRegexPattern(stringParameter(parameters, "pattern"));
                default -> {
                    // NOT_NULL, NOT_EMPTY and EMAIL have no dedicated column on the entity
                }
            }
        }
    }

    private Integer intParameter(Map<String, Object> parameters, String name) {
        Object value = parameters != null ? parameters.get(name) : null;
        return value instanceof Number number ? number.intValue() : null;
    }

    private Double doubleParameter(Map<String, Object> parameters, String name) {
        Object value = parameters != null ? parameters.get(name) : null;
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private String stringParameter(Map<String, Object> parameters, String name) {
        Object value = parameters != null ? parameters.get(name) : null;
        return value != null ? value.toString() : null;
    }
}
