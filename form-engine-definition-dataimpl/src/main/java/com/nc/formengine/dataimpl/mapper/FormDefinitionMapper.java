package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class FormDefinitionMapper {

    private final FieldDefinitionMapper fieldDefinitionMapper;
    private final FormLayoutMapper formLayoutMapper;

    public FormDefinitionMapper(FieldDefinitionMapper fieldDefinitionMapper, FormLayoutMapper formLayoutMapper) {
        this.fieldDefinitionMapper = fieldDefinitionMapper;
        this.formLayoutMapper = formLayoutMapper;
    }

    public FormDefinitionDTO toDTO(FormDefinition entity) {
        if (entity == null) {
            return null;
        }

        return FormDefinitionDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .version(entity.getVersion())
                .fields(entity.getFields() != null ? 
                    entity.getFields().stream()
                        .map(fieldDefinitionMapper::toDTO)
                        .collect(Collectors.toList()) : null)
                .layouts(entity.getLayouts() != null ?
                    entity.getLayouts().stream()
                        .map(formLayoutMapper::toDTO)
                        .collect(Collectors.toList()) : null)
                .build();
    }

    public FormDefinition toEntity(FormDefinitionDTO dto) {
        if (dto == null) {
            return null;
        }

        FormDefinition entity = new FormDefinition();
        entity.setId(dto.getId());
        entity.setCode(dto.getCode());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setVersion(dto.getVersion());

        return entity;
    }

    public void updateEntity(FormDefinitionDTO dto, FormDefinition entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setCode(dto.getCode());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setVersion(dto.getVersion());
    }
}
