package com.nc.formengine.submission.businessimpl.service;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.business.service.FormValidationService;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.validation.ValidationMode;
import com.nc.formengine.model.validation.ValidationReport;
import com.nc.formengine.submission.business.service.FormSubmissionWorkflowService;
import com.nc.formengine.submission.business.service.SubmissionResult;
import com.nc.formengine.submission.data.dao.FormSubmissionDao;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.nc.formengine.submission.model.exception.FormNotAcceptingSubmissionsException;
import com.nc.formengine.submission.model.exception.FormSubmissionNotFoundException;
import com.nc.formengine.submission.model.exception.IllegalSubmissionTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class FormSubmissionWorkflowServiceImpl implements FormSubmissionWorkflowService {

    private final FormSubmissionDao formSubmissionDao;
    private final FormValidationService formValidationService;
    private final FormDefinitionService formDefinitionService;
    private final FieldDefinitionService fieldDefinitionService;

    @Override
    public SubmissionResult saveDraft(FormSubmissionDTO submission) {
        requireTransition(submission.getId(), SubmissionStatus.DRAFT);

        ValidationReport report = validate(submission, ValidationMode.DRAFT);

        submission.setStatus(SubmissionStatus.DRAFT);
        return SubmissionResult.persisted(formSubmissionDao.save(submission), report);
    }

    @Override
    public SubmissionResult submit(FormSubmissionDTO submission) {
        requireTransition(submission.getId(), SubmissionStatus.SUBMITTED);
        requireFormAcceptsSubmissions(submission.getFormDefinitionId());

        ValidationReport report = validate(submission, ValidationMode.SUBMIT);
        if (!report.valid()) {
            // Nothing is written. The whole point of validating at submit time is that a stored
            // SUBMITTED row is one its definition would accept, so a failed check cannot be allowed
            // to leave a partial trace.
            return SubmissionResult.rejected(submission, report);
        }

        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());
        return SubmissionResult.persisted(formSubmissionDao.save(submission), report);
    }

    @Override
    public FormSubmissionDTO discard(Long id) {
        return moveTo(id, SubmissionStatus.DISCARDED, SubmissionStatus.DRAFT);
    }

    @Override
    public FormSubmissionDTO voidSubmission(Long id) {
        return moveTo(id, SubmissionStatus.VOIDED, SubmissionStatus.SUBMITTED);
    }

    /**
     * Flips a submission's state, having checked it was in one the move is allowed from.
     *
     * <p>The stored row is read and modified rather than replaced, because saving a submission built
     * from scratch would arrive with no answers and take the stored ones with it.
     */
    private FormSubmissionDTO moveTo(Long id, SubmissionStatus target, SubmissionStatus... allowedFrom) {
        FormSubmissionDTO stored = formSubmissionDao.findById(id)
                .orElseThrow(() -> new FormSubmissionNotFoundException(id));

        if (!isAnyOf(stored.getStatus(), allowedFrom)) {
            throw new IllegalSubmissionTransitionException(id, stored.getStatus(), target);
        }

        stored.setStatus(target);
        return formSubmissionDao.save(stored);
    }

    /**
     * A submission may only be written while it is a draft. A brand-new submission (no id) has no
     * stored state to contradict, so it passes.
     *
     * <p>{@code target} is what the caller asked for, not what the guard wants: reporting the move the
     * user tried to make is the only way the message can mean anything. Reporting the required state
     * instead produced "cannot go from SUBMITTED to DRAFT" for someone who had asked to submit.
     */
    private void requireTransition(Long id, SubmissionStatus target) {
        if (id == null) {
            return;
        }
        FormSubmissionDTO stored = formSubmissionDao.findById(id)
                .orElseThrow(() -> new FormSubmissionNotFoundException(id));
        if (stored.getStatus() != SubmissionStatus.DRAFT) {
            throw new IllegalSubmissionTransitionException(id, stored.getStatus(), target);
        }
    }

    private static boolean isAnyOf(SubmissionStatus status, SubmissionStatus... candidates) {
        for (SubmissionStatus candidate : candidates) {
            if (status == candidate) {
                return true;
            }
        }
        return false;
    }

    /**
     * Only a published definition takes submissions. A draft is still being written, and an archived
     * one has been superseded; answering either would produce a record nobody intends to read.
     */
    private void requireFormAcceptsSubmissions(Long formDefinitionId) {
        FormDefinitionDTO definition = formDefinitionService.findById(formDefinitionId)
                .orElseThrow(() -> new FormNotAcceptingSubmissionsException(formDefinitionId, "missing"));

        if (definition.getStatus() != FormDefinitionStatus.PUBLISHED) {
            throw new FormNotAcceptingSubmissionsException(
                    formDefinitionId, String.valueOf(definition.getStatus()));
        }
    }

    private ValidationReport validate(FormSubmissionDTO submission, ValidationMode mode) {
        return formValidationService.validate(
                submission.getFormDefinitionId(), answersByFieldName(submission), mode);
    }

    /**
     * Turns the submitted rows into the {@code name -> value} map the validator reads.
     *
     * <p>{@code fieldName} is denormalised and callers legitimately omit it, so a missing one is
     * looked up from the field definition rather than treated as an error. A row naming a field that
     * no longer exists is dropped: it cannot be keyed, and the validator would ignore an unknown key
     * anyway.
     */
    private Map<String, Object> answersByFieldName(FormSubmissionDTO submission) {
        if (submission.getFieldSubmissions() == null) {
            return Map.of();
        }

        Map<Long, String> resolvedNames = new HashMap<>();
        Map<String, Object> answers = new LinkedHashMap<>();
        for (FieldSubmissionDTO fieldSubmission : submission.getFieldSubmissions()) {
            String name = fieldSubmission.getFieldName();
            if (name == null || name.isBlank()) {
                name = resolvedNames.computeIfAbsent(
                        fieldSubmission.getFieldDefinitionId(), this::lookUpFieldName);
            }
            if (name != null) {
                answers.put(name, fieldSubmission.getValue());
            }
        }
        return answers;
    }

    private String lookUpFieldName(Long fieldDefinitionId) {
        if (fieldDefinitionId == null) {
            return null;
        }
        return fieldDefinitionService.findById(fieldDefinitionId)
                .map(field -> field.getName())
                .orElse(null);
    }
}
