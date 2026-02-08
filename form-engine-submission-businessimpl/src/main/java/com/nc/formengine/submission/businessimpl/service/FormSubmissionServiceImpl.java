package com.nc.formengine.submission.businessimpl.service;

import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.data.dao.FormSubmissionDao;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        FormSubmissionDTO existing = formSubmissionDao.findById(id)
                .orElseThrow(() -> new RuntimeException("Form submission not found with id: " + id));
        
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
        return formSubmissionDao.findByStatus(status);
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
    public void deleteById(Long id) {
        formSubmissionDao.deleteById(id);
    }
}
