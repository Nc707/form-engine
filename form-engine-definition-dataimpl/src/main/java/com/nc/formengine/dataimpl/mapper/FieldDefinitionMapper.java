package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.data.entity.FieldDefinition;
import com.nc.formengine.data.entity.FormDefinition;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import org.springframework.stereotype.Component;

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
                .minLength(entity.getMinLength())
                .maxLength(entity.getMaxLength())
                .minValue(entity.getMinValue())
                .maxValue(entity.getMaxValue())
                .regexPattern(entity.getRegexPattern())
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
        entity.setMinLength(dto.getMinLength());
        entity.setMaxLength(dto.getMaxLength());
        entity.setMinValue(dto.getMinValue());
        entity.setMaxValue(dto.getMaxValue());
        entity.setRegexPattern(dto.getRegexPattern());

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
        entity.setMinLength(dto.getMinLength());
        entity.setMaxLength(dto.getMaxLength());
        entity.setMinValue(dto.getMinValue());
        entity.setMaxValue(dto.getMaxValue());
        entity.setRegexPattern(dto.getRegexPattern());
    }

    public void setFormDefinition(FieldDefinition entity, FormDefinition formDefinition) {
        if (entity != null) {
            entity.setFormDefinition(formDefinition);
        }
    }
}
