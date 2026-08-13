package com.nc.formengine.submission.dataimpl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.nc.formengine.submission.dataimpl.entity.FormSubmission;
import com.nc.formengine.submission.model.enums.SubmissionStatus;

import java.util.List;

@Repository
public interface FormSubmissionRepository
        extends JpaRepository<FormSubmission, Long>, JpaSpecificationExecutor<FormSubmission> {
    
    List<FormSubmission> findByFormDefinitionId(Long formDefinitionId);
    
    List<FormSubmission> findByFormCode(String formCode);
    
    List<FormSubmission> findByAuthor(String author);
    
    List<FormSubmission> findByStatus(SubmissionStatus status);
}
