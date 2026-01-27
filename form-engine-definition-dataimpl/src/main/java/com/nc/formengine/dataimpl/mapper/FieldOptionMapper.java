package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.data.entity.FieldDefinition;
import com.nc.formengine.data.entity.FieldOption;
import com.nc.formengine.model.dto.FieldOptionDTO;
import org.springframework.stereotype.Component;

@Component
public class FieldOptionMapper {

    public FieldOptionDTO toDTO(FieldOption entity) {
        if (entity == null) {
            return null;
        }

        return FieldOptionDTO.builder()
                .id(entity.getId())
                .fieldDefinitionId(entity.getFieldDefinition() != null ? 
                    entity.getFieldDefinition().getId() : null)
                .label(entity.getLabel())
                .value(entity.getValue())
                .orderIndex(entity.getOrderIndex())
                .build();
    }

    public FieldOption toEntity(FieldOptionDTO dto) {
        if (dto == null) {
            return null;
        }

        FieldOption entity = new FieldOption();
        entity.setId(dto.getId());
        entity.setLabel(dto.getLabel());
        entity.setValue(dto.getValue());
        entity.setOrderIndex(dto.getOrderIndex());

        return entity;
    }

    public void updateEntity(FieldOptionDTO dto, FieldOption entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setLabel(dto.getLabel());
        entity.setValue(dto.getValue());
        entity.setOrderIndex(dto.getOrderIndex());
    }

    public void setFieldDefinition(FieldOption entity, FieldDefinition fieldDefinition) {
        if (entity != null) {
            entity.setFieldDefinition(fieldDefinition);
        }
    }
}
