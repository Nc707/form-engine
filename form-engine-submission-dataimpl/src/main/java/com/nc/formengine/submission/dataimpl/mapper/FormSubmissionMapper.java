package com.nc.formengine.submission.dataimpl.mapper;

import com.nc.formengine.submission.dataimpl.entity.FieldSubmission;
import com.nc.formengine.submission.dataimpl.entity.FormSubmission;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Maps {@link FormSubmission} to its DTO, together with the answers it owns.
 *
 * <p><b>Absent is not empty.</b> A null {@code fieldSubmissions} on an incoming DTO means "this
 * request says nothing about the answers", so what is stored is kept; an empty list means "there are
 * none", so what is stored is deleted.
 */
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
                .status(entity.getStatus())
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
        // status is NOT NULL and defaults to SUBMITTED on the entity; only overwrite it when the
        // DTO actually carries a value, otherwise a create without a status violates the constraint.
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        // toDTO maps the answers, so toEntity must map them back. Dropping them here meant a
        // submission saved from a round-tripped DTO kept only its header row, and, since the
        // collection is cascaded with orphanRemoval, that saving it again deleted whatever answers
        // had been written before.
        if (dto.getFieldSubmissions() != null) {
            List<FieldSubmission> answers = dto.getFieldSubmissions().stream()
                    .filter(Objects::nonNull)
                    .map(fieldSubmissionMapper::toEntity)
                    .collect(Collectors.toList());
            answers.forEach(answer -> fieldSubmissionMapper.setFormSubmission(answer, entity));
            entity.setFieldSubmissions(answers);
        }

        return entity;
    }

    public void updateEntity(FormSubmissionDTO dto, FormSubmission entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setFormDefinitionId(dto.getFormDefinitionId());
        entity.setFormCode(dto.getFormCode());
        entity.setSubmittedBy(dto.getSubmittedBy());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getSubmittedAt() != null) {
            entity.setSubmittedAt(dto.getSubmittedAt());
        }
    }
}
