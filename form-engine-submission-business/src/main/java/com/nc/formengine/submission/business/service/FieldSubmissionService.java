package com.nc.formengine.submission.business.service;

import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;

import java.util.List;
import java.util.Optional;

public interface FieldSubmissionService {
    
    FieldSubmissionDTO create(FieldSubmissionDTO fieldSubmissionDTO);
    
    FieldSubmissionDTO update(Long id, FieldSubmissionDTO fieldSubmissionDTO);
    
    Optional<FieldSubmissionDTO> findById(Long id);
    
    List<FieldSubmissionDTO> findByFormSubmissionId(Long formSubmissionId);
    
    List<FieldSubmissionDTO> findByFieldDefinitionId(Long fieldDefinitionId);
    
    List<FieldSubmissionDTO> findAll();
    
    void deleteById(Long id);
    
    void deleteByFormSubmissionId(Long formSubmissionId);
}
