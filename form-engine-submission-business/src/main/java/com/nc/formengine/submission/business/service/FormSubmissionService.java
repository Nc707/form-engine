package com.nc.formengine.submission.business.service;

import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.dto.SubmissionFilter;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FormSubmissionService {
    
    FormSubmissionDTO create(FormSubmissionDTO formSubmissionDTO);
    
    FormSubmissionDTO update(Long id, FormSubmissionDTO formSubmissionDTO);
    
    Optional<FormSubmissionDTO> findById(Long id);
    
    List<FormSubmissionDTO> findByFormDefinitionId(Long formDefinitionId);
    
    List<FormSubmissionDTO> findByFormCode(String formCode);
    
    List<FormSubmissionDTO> findByAuthor(String author);
    
    List<FormSubmissionDTO> findByStatus(String status);
    
    List<FormSubmissionDTO> findAll();
    
    Page<FormSubmissionDTO> findAll(Pageable pageable);

    /**
     * The paged read that narrowing screens need: the single-criterion {@code findBy*} methods above
     * each return the whole list, so combining two of them means loading both and intersecting in
     * memory. A {@code null} component of the filter does not narrow anything.
     */
    Page<FormSubmissionDTO> findAll(SubmissionFilter filter, Pageable pageable);

    /** How many submissions the filter matches in each state; every state is present, zeroes included. */
    Map<SubmissionStatus, Long> countByStatus(SubmissionFilter filter);

    void deleteById(Long id);
}
