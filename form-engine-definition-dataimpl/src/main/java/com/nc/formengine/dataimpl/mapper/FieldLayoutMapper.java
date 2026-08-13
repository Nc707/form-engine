package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.dataimpl.entity.FieldLayout;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import org.springframework.stereotype.Component;

/**
 * Maps {@link FieldLayout} to its DTO.
 *
 * <p>A field layout says only where a field goes and whether the layout places it at all. What the
 * field looks like is decided from its {@code FieldType}, so there is nothing here to configure a
 * widget with.
 */
@Component
public class FieldLayoutMapper {

    public FieldLayoutDTO toDTO(FieldLayout entity) {
        if (entity == null) {
            return null;
        }

        return FieldLayoutDTO.builder()
                .id(entity.getId())
                .formLayoutId(entity.getFormLayout() != null ? entity.getFormLayout().getId() : null)
                .fieldDefinitionId(entity.getFieldDefinition() != null ? entity.getFieldDefinition().getId() : null)
                .row(entity.getRow())
                .column(entity.getColumn())
                .colspan(entity.getColspan())
                .rowspan(entity.getRowspan())
                .build();
    }

    public FieldLayout toEntity(FieldLayoutDTO dto) {
        if (dto == null) {
            return null;
        }

        FieldLayout entity = new FieldLayout();
        entity.setId(dto.getId());
        entity.setRow(dto.getRow());
        entity.setColumn(dto.getColumn());
        entity.setColspan(dto.getColspan());
        entity.setRowspan(dto.getRowspan());

        return entity;
    }
}
