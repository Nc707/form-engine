package com.nc.formengine.submission.businessimpl.service;

import com.nc.formengine.submission.business.service.FieldSubmissionService;
import com.nc.formengine.submission.data.dao.FieldSubmissionDao;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.exception.FieldSubmissionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FieldSubmissionServiceImpl implements FieldSubmissionService {

    private final FieldSubmissionDao fieldSubmissionDao;

    @Override
    public FieldSubmissionDTO create(FieldSubmissionDTO fieldSubmissionDTO) {
        if (fieldSubmissionDTO.getId() != null) {
            throw new IllegalArgumentException("New field submission should not have an ID");
        }
        return fieldSubmissionDao.save(fieldSubmissionDTO);
    }

    @Override
    public FieldSubmissionDTO update(Long id, FieldSubmissionDTO fieldSubmissionDTO) {
        // Existence check: throws if the id is unknown.
        fieldSubmissionDao.findById(id)
                .orElseThrow(() -> new FieldSubmissionNotFoundException(id));
        
        fieldSubmissionDTO.setId(id);
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
        fieldSubmissionDao.deleteById(id);
    }

    @Override
    public void deleteByFormSubmissionId(Long formSubmissionId) {
        fieldSubmissionDao.deleteByFormSubmissionId(formSubmissionId);
    }
}
