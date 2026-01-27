package com.nc.formengine.submission.data.dao;

import java.util.List;
import java.util.Optional;

import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;

public interface FieldSubmissionDao {
    
    FieldSubmissionDTO save(FieldSubmissionDTO fieldSubmissionDTO);
    
    Optional<FieldSubmissionDTO> findById(Long id);
    
    List<FieldSubmissionDTO> findByFormSubmissionId(Long formSubmissionId);
    
    List<FieldSubmissionDTO> findByFieldDefinitionId(Long fieldDefinitionId);
    
    List<FieldSubmissionDTO> findAll();
    
    void deleteById(Long id);
    
    void deleteByFormSubmissionId(Long formSubmissionId);
}
