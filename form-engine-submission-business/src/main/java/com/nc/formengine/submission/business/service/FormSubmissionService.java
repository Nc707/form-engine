package com.nc.formengine.submission.business.service;

import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface FormSubmissionService {
    
    FormSubmissionDTO create(FormSubmissionDTO formSubmissionDTO);
    
    FormSubmissionDTO update(Long id, FormSubmissionDTO formSubmissionDTO);
    
    Optional<FormSubmissionDTO> findById(Long id);
    
    List<FormSubmissionDTO> findByFormDefinitionId(Long formDefinitionId);
    
    List<FormSubmissionDTO> findByFormCode(String formCode);
    
    List<FormSubmissionDTO> findBySubmittedBy(String submittedBy);
    
    List<FormSubmissionDTO> findByStatus(String status);
    
    List<FormSubmissionDTO> findAll();
    
    Page<FormSubmissionDTO> findAll(Pageable pageable);
    
    void deleteById(Long id);
}
