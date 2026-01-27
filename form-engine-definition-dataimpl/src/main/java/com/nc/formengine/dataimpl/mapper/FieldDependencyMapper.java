package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.data.entity.FieldDefinition;
import com.nc.formengine.data.entity.FieldDependency;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import org.springframework.stereotype.Component;

@Component
public class FieldDependencyMapper {

    public FieldDependencyDTO toDTO(FieldDependency entity) {
        if (entity == null) {
            return null;
        }

        return FieldDependencyDTO.builder()
                .id(entity.getId())
                .dependentFieldId(entity.getDependentField() != null ? 
                    entity.getDependentField().getId() : null)
                .triggerFieldId(entity.getTriggerField() != null ? 
                    entity.getTriggerField().getId() : null)
                .action(entity.getAction() != null ? 
                    com.nc.formengine.model.enums.DependencyAction.valueOf(entity.getAction().name()) : null)
                .triggerValue(entity.getTriggerValue())
                .build();
    }

    public FieldDependency toEntity(FieldDependencyDTO dto) {
        if (dto == null) {
            return null;
        }

        FieldDependency entity = new FieldDependency();
        entity.setId(dto.getId());
        entity.setAction(dto.getAction() != null ? 
            com.nc.formengine.data.enums.DependencyAction.valueOf(dto.getAction().name()) : null);
        entity.setTriggerValue(dto.getTriggerValue());

        return entity;
    }

    public void updateEntity(FieldDependencyDTO dto, FieldDependency entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setAction(dto.getAction() != null ? 
            com.nc.formengine.data.enums.DependencyAction.valueOf(dto.getAction().name()) : null);
        entity.setTriggerValue(dto.getTriggerValue());
    }

    public void setDependentField(FieldDependency entity, FieldDefinition dependentField) {
        if (entity != null) {
            entity.setDependentField(dependentField);
        }
    }

    public void setTriggerField(FieldDependency entity, FieldDefinition triggerField) {
        if (entity != null) {
            entity.setTriggerField(triggerField);
        }
    }
}
