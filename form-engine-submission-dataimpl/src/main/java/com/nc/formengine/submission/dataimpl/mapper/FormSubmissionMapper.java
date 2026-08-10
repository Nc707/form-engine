package com.nc.formengine.submission.dataimpl.mapper;

import com.nc.formengine.submission.dataimpl.entity.FieldSubmission;
import com.nc.formengine.submission.dataimpl.entity.FormSubmission;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Maps {@link FormSubmission} to its DTO, together with the answers it owns.
 *
 * <p><b>Saying nothing changes nothing.</b> An incoming DTO whose {@code fieldSubmissions} is null
 * or empty is read as "this request is not about the answers", and the stored ones are kept. Only a
 * non-empty list is authoritative: then it is the whole truth, and an answer missing from it is
 * deleted.
 *
 * <p>Empty has to mean silence rather than "delete them all", because
 * {@link FormSubmissionDTO#getFieldSubmissions()} is a {@code @Builder.Default} empty list — a
 * caller who never mentions answers is indistinguishable from one asking for all of them to go. The
 * representation cannot tell the two apart, so the destructive reading would be a guess, and the
 * cost of guessing wrong is somebody's filled-in form. Clearing every answer is therefore not
 * something {@code save} can express; it would need a call that says so.
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
        if (dto.getFieldSubmissions() != null && !dto.getFieldSubmissions().isEmpty()) {
            applyAnswers(dto.getFieldSubmissions(), entity);
        }
    }

    /**
     * Brings the stored answers in line with the ones the DTO carries.
     *
     * <p>The answers a request already knows are updated where they stand rather than deleted and
     * rewritten, so a second "save draft" edits the rows it created instead of churning through new
     * ids. Answers the DTO no longer mentions are dropped from the collection, which
     * {@code orphanRemoval} turns into deletes.
     *
     * <p>The collection is edited in place. Assigning a new list would leave Hibernate holding a
     * collection it no longer owns, which it refuses outright.
     */
    private void applyAnswers(List<FieldSubmissionDTO> answers, FormSubmission entity) {
        Map<Long, FieldSubmission> storedById = entity.getFieldSubmissions().stream()
                .filter(stored -> stored.getId() != null)
                .collect(Collectors.toMap(FieldSubmission::getId, stored -> stored, (first, second) -> first));

        List<FieldSubmission> wanted = new ArrayList<>();
        for (FieldSubmissionDTO answer : answers) {
            if (answer == null) {
                continue;
            }
            FieldSubmission stored = answer.getId() == null ? null : storedById.get(answer.getId());
            if (stored != null) {
                fieldSubmissionMapper.updateEntity(answer, stored);
                wanted.add(stored);
            } else {
                FieldSubmission created = fieldSubmissionMapper.toEntity(answer);
                fieldSubmissionMapper.setFormSubmission(created, entity);
                wanted.add(created);
            }
        }

        // Identity, not equals: a not-yet-persisted answer has a null id, and the id-based equals
        // every entity here uses reports those as unequal to everything, itself included.
        entity.getFieldSubmissions().removeIf(stored -> wanted.stream().noneMatch(kept -> kept == stored));
        for (FieldSubmission answer : wanted) {
            if (entity.getFieldSubmissions().stream().noneMatch(present -> present == answer)) {
                entity.getFieldSubmissions().add(answer);
            }
        }
    }
}
