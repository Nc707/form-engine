package com.nc.formengine.submission.data.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nc.formengine.submission.model.dto.FormSubmissionDTO;

import java.util.List;
import java.util.Optional;

public interface FormSubmissionDao {
    
    FormSubmissionDTO save(FormSubmissionDTO formSubmissionDTO);
    
    Optional<FormSubmissionDTO> findById(Long id);
    
    List<FormSubmissionDTO> findByFormDefinitionId(Long formDefinitionId);
    
    List<FormSubmissionDTO> findByFormCode(String formCode);
    
    List<FormSubmissionDTO> findBySubmittedBy(String submittedBy);
    
    List<FormSubmissionDTO> findByStatus(String status);
    
    List<FormSubmissionDTO> findAll();
    
    Page<FormSubmissionDTO> findAll(Pageable pageable);
    
    void deleteById(Long id);
}
