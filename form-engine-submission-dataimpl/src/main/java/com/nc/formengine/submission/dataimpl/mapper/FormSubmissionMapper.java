package com.nc.formengine.submission.dataimpl.mapper;

import com.nc.formengine.submission.data.entity.FormSubmission;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class FormSubmissionMapper {

    private final FieldSubmissionMapper fieldSubmissionMapper;

    public FormSubmissionMapper(FieldSubmissionMapper fieldSubmissionMapper) {
        this.fieldSubmissionMapper = fieldSubmissionMapper;
    }

    public FormSubmissionDTO toDTO(FormSubmission entity) {
        if (entity == null) {
            return null;
        }

        return FormSubmissionDTO.builder()
                .id(entity.getId())
                .formDefinitionId(entity.getFormDefinitionId())
                .formCode(entity.getFormCode())
                .submittedBy(entity.getSubmittedBy())
                .submittedAt(entity.getSubmittedAt())
                .status(entity.getStatus() != null ? 
                    com.nc.formengine.submission.model.enums.SubmissionStatus.valueOf(entity.getStatus().name()) : null)
                .fieldSubmissions(entity.getFieldSubmissions() != null ?
                        entity.getFieldSubmissions().stream()
                                .map(fieldSubmissionMapper::toDTO)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    public FormSubmission toEntity(FormSubmissionDTO dto) {
        if (dto == null) {
            return null;
        }

        FormSubmission entity = new FormSubmission();
        entity.setId(dto.getId());
        entity.setFormDefinitionId(dto.getFormDefinitionId());
        entity.setFormCode(dto.getFormCode());
        entity.setSubmittedBy(dto.getSubmittedBy());
        entity.setSubmittedAt(dto.getSubmittedAt());
        entity.setStatus(dto.getStatus() != null ? 
            com.nc.formengine.submission.data.enums.SubmissionStatus.valueOf(dto.getStatus().name()) : null);

        return entity;
    }

    public void updateEntity(FormSubmissionDTO dto, FormSubmission entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setFormDefinitionId(dto.getFormDefinitionId());
        entity.setFormCode(dto.getFormCode());
        entity.setSubmittedBy(dto.getSubmittedBy());
        entity.setStatus(dto.getStatus() != null ? 
            com.nc.formengine.submission.data.enums.SubmissionStatus.valueOf(dto.getStatus().name()) : null);
        if (dto.getSubmittedAt() != null) {
            entity.setSubmittedAt(dto.getSubmittedAt());
        }
    }
}
