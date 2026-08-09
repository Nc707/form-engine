package com.nc.formengine.submission.data.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.dto.SubmissionFilter;
import com.nc.formengine.submission.model.enums.SubmissionStatus;

import java.util.List;
import java.util.Map;
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

    Page<FormSubmissionDTO> findAll(SubmissionFilter filter, Pageable pageable);

    Map<SubmissionStatus, Long> countByStatus(SubmissionFilter filter);

    void deleteById(Long id);
}
