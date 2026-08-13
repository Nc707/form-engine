package com.nc.formengine.submission.businessimpl.service;

import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.data.dao.FormSubmissionDao;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.dto.SubmissionFilter;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.nc.formengine.submission.model.exception.FormSubmissionNotFoundException;
import com.nc.formengine.submission.model.exception.InvalidSubmissionStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reading and writing submissions, without their lifecycle.
 *
 * <p>The plain CRUD view. It can change what a submission <em>says</em>, never where it is: moving
 * between states is {@code FormSubmissionWorkflowService}'s alone, because that is the only path that
 * validates and that checks the form still accepts answers.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FormSubmissionServiceImpl implements FormSubmissionService {

    private final FormSubmissionDao formSubmissionDao;

    @Override
    public FormSubmissionDTO create(FormSubmissionDTO formSubmissionDTO) {
        if (formSubmissionDTO.getId() != null) {
            throw new IllegalArgumentException("New form submission should not have an ID");
        }
        // Every submission starts as a draft, whatever the caller asked for. Honouring a requested
        // state let a client store a SUBMITTED row with no answers, against a form nobody had
        // published — defeating the one guarantee submitting exists to give.
        formSubmissionDTO.setStatus(SubmissionStatus.DRAFT);
        formSubmissionDTO.setSubmittedAt(null);
        return formSubmissionDao.save(formSubmissionDTO);
    }

    @Override
    public FormSubmissionDTO update(Long id, FormSubmissionDTO formSubmissionDTO) {
        FormSubmissionDTO stored = formSubmissionDao.findById(id)
                .orElseThrow(() -> new FormSubmissionNotFoundException(id));

        formSubmissionDTO.setId(id);
        // The state, and the moment of sending, belong to the lifecycle. Editing them here would let a
        // caller un-void a response or claim one was submitted when it never was.
        formSubmissionDTO.setStatus(stored.getStatus());
        formSubmissionDTO.setSubmittedAt(stored.getSubmittedAt());
        return formSubmissionDao.save(formSubmissionDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormSubmissionDTO> findById(Long id) {
        return formSubmissionDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormSubmissionDTO> findByFormDefinitionId(Long formDefinitionId) {
        return formSubmissionDao.findByFormDefinitionId(formDefinitionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormSubmissionDTO> findByFormCode(String formCode) {
        return formSubmissionDao.findByFormCode(formCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormSubmissionDTO> findByAuthor(String author) {
        return formSubmissionDao.findByAuthor(author);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormSubmissionDTO> findByStatus(String status) {
        // The DAO parses this back into the enum, so reject unknown values here where we can
        // still say what would have been accepted.
        return formSubmissionDao.findByStatus(parseStatus(status).name());
    }

    private SubmissionStatus parseStatus(String status) {
        try {
            return SubmissionStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidSubmissionStatusException(status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormSubmissionDTO> findAll() {
        return formSubmissionDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FormSubmissionDTO> findAll(Pageable pageable) {
        return formSubmissionDao.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FormSubmissionDTO> findAll(SubmissionFilter filter, Pageable pageable) {
        return formSubmissionDao.findAll(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<SubmissionStatus, Long> countByStatus(SubmissionFilter filter) {
        return formSubmissionDao.countByStatus(filter);
    }

    @Override
    public void deleteById(Long id) {
        formSubmissionDao.deleteById(id);
    }
}
