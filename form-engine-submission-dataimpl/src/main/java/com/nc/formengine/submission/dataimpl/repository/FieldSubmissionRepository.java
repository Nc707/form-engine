package com.nc.formengine.submission.dataimpl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nc.formengine.submission.dataimpl.entity.FieldSubmission;

import java.util.List;

@Repository
public interface FieldSubmissionRepository extends JpaRepository<FieldSubmission, Long> {
    
    List<FieldSubmission> findByFormSubmissionId(Long formSubmissionId);
    
    List<FieldSubmission> findByFieldDefinitionId(Long fieldDefinitionId);
    
    void deleteByFormSubmissionId(Long formSubmissionId);
}
