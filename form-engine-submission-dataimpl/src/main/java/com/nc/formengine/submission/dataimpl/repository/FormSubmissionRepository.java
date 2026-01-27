package com.nc.formengine.submission.dataimpl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nc.formengine.submission.data.entity.FormSubmission;

import java.util.List;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {
    
    List<FormSubmission> findByFormDefinitionId(Long formDefinitionId);
    
    List<FormSubmission> findByFormCode(String formCode);
    
    List<FormSubmission> findBySubmittedBy(String submittedBy);
    
    List<FormSubmission> findByStatus(String status);
}
