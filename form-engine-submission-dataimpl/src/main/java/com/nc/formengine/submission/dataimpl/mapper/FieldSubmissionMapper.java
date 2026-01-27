package com.nc.formengine.submission.dataimpl.mapper;

import com.nc.formengine.submission.data.entity.FieldSubmission;
import com.nc.formengine.submission.data.entity.FormSubmission;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;

import org.springframework.stereotype.Component;

@Component
public class FieldSubmissionMapper {

    public FieldSubmissionDTO toDTO(FieldSubmission entity) {
        if (entity == null) {
            return null;
        }

        return FieldSubmissionDTO.builder()
                .id(entity.getId())
                .formSubmissionId(entity.getFormSubmission() != null ?
                        entity.getFormSubmission().getId() : null)
                .fieldDefinitionId(entity.getFieldDefinitionId())
                .fieldName(entity.getFieldName())
                .value(entity.getValue())
                .build();
    }

    public FieldSubmission toEntity(FieldSubmissionDTO dto) {
        if (dto == null) {
            return null;
        }

        FieldSubmission entity = new FieldSubmission();
        entity.setId(dto.getId());
        entity.setFieldDefinitionId(dto.getFieldDefinitionId());
        entity.setFieldName(dto.getFieldName());
        entity.setValue(dto.getValue());

        return entity;
    }

    public void updateEntity(FieldSubmissionDTO dto, FieldSubmission entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setFieldDefinitionId(dto.getFieldDefinitionId());
        entity.setFieldName(dto.getFieldName());
        entity.setValue(dto.getValue());
    }

    public void setFormSubmission(FieldSubmission entity, FormSubmission formSubmission) {
        if (entity != null) {
            entity.setFormSubmission(formSubmission);
        }
    }
}
