package com.nc.formengine.submission.businessimpl.service;

import com.nc.formengine.submission.data.dao.FieldSubmissionDao;
import com.nc.formengine.submission.data.dao.FormSubmissionDao;
import com.nc.formengine.submission.business.service.FieldSubmissionService;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.nc.formengine.submission.model.exception.FieldSubmissionNotFoundException;
import com.nc.formengine.submission.model.exception.FormSubmissionNotFoundException;
import com.nc.formengine.submission.model.exception.IllegalSubmissionTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The individual answers inside a submission.
 *
 * <p>Answers are part of a submission, not a thing of their own, so they can only be written while the
 * submission they belong to is still a draft. Without that, {@code SUBMITTED} meant nothing: a response
 * was validated in the transaction that stored it and could then be edited answer by answer, so the
 * guarantee that a stored response is one its own form would accept only held for an instant.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FieldSubmissionServiceImpl implements FieldSubmissionService {

    private final FieldSubmissionDao fieldSubmissionDao;
    private final FormSubmissionDao formSubmissionDao;

    @Override
    public FieldSubmissionDTO create(FieldSubmissionDTO fieldSubmissionDTO) {
        if (fieldSubmissionDTO.getId() != null) {
            throw new IllegalArgumentException("New field submission should not have an ID");
        }
        requireDraft(fieldSubmissionDTO.getFormSubmissionId());
        return fieldSubmissionDao.save(fieldSubmissionDTO);
    }

    @Override
    public FieldSubmissionDTO update(Long id, FieldSubmissionDTO fieldSubmissionDTO) {
        FieldSubmissionDTO stored = fieldSubmissionDao.findById(id)
                .orElseThrow(() -> new FieldSubmissionNotFoundException(id));
        requireDraft(stored.getFormSubmissionId());

        fieldSubmissionDTO.setId(id);
        // An answer belongs to the submission it was given in, and cannot be moved to another.
        fieldSubmissionDTO.setFormSubmissionId(stored.getFormSubmissionId());
        return fieldSubmissionDao.save(fieldSubmissionDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FieldSubmissionDTO> findById(Long id) {
        return fieldSubmissionDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldSubmissionDTO> findByFormSubmissionId(Long formSubmissionId) {
        return fieldSubmissionDao.findByFormSubmissionId(formSubmissionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldSubmissionDTO> findByFieldDefinitionId(Long fieldDefinitionId) {
        return fieldSubmissionDao.findByFieldDefinitionId(fieldDefinitionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldSubmissionDTO> findAll() {
        return fieldSubmissionDao.findAll();
    }

    @Override
    public void deleteById(Long id) {
        fieldSubmissionDao.findById(id)
                .ifPresent(stored -> requireDraft(stored.getFormSubmissionId()));
        fieldSubmissionDao.deleteById(id);
    }

    @Override
    public void deleteByFormSubmissionId(Long formSubmissionId) {
        requireDraft(formSubmissionId);
        fieldSubmissionDao.deleteByFormSubmissionId(formSubmissionId);
    }

    /**
     * Allows a change to an answer only while its submission is unsent.
     *
     * @throws FormSubmissionNotFoundException      if the submission does not exist
     * @throws IllegalSubmissionTransitionException if it has already been sent, or has ended
     */
    private void requireDraft(Long formSubmissionId) {
        FormSubmissionDTO submission = formSubmissionDao.findById(formSubmissionId)
                .orElseThrow(() -> new FormSubmissionNotFoundException(formSubmissionId));
        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new IllegalSubmissionTransitionException(
                    formSubmissionId, submission.getStatus(), SubmissionStatus.DRAFT);
        }
    }
}
