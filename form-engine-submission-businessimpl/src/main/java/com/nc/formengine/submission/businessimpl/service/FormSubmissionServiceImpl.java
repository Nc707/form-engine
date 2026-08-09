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
        return formSubmissionDao.save(formSubmissionDTO);
    }

    @Override
    public FormSubmissionDTO update(Long id, FormSubmissionDTO formSubmissionDTO) {
        // Existence check: throws if the id is unknown.
        formSubmissionDao.findById(id)
                .orElseThrow(() -> new FormSubmissionNotFoundException(id));
        
        formSubmissionDTO.setId(id);
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
    public List<FormSubmissionDTO> findBySubmittedBy(String submittedBy) {
        return formSubmissionDao.findBySubmittedBy(submittedBy);
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
