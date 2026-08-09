package com.nc.formengine.submission.dataimpl.daoimpl;

import com.nc.formengine.submission.data.dao.FormSubmissionDao;
import com.nc.formengine.submission.dataimpl.entity.FormSubmission;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.nc.formengine.submission.dataimpl.mapper.FormSubmissionMapper;
import com.nc.formengine.submission.dataimpl.repository.FormSubmissionRepository;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FormSubmissionDaoImpl implements FormSubmissionDao {

    private final FormSubmissionRepository jpaRepository;
    private final FormSubmissionMapper mapper;

    /**
     * Creates the submission, or updates the stored one when the DTO names it.
     *
     * <p>An update reads the stored entity and applies the DTO onto it rather than merging a
     * detached one built from scratch, for the same reason {@code FormDaoImpl} does: a submission
     * owns its answers with {@code orphanRemoval}, and {@link FormSubmissionMapper#toEntity} does
     * not carry them, so merging would delete every answer the submission has. That path is not
     * hypothetical — {@code cancel} is exactly a status change on a submission full of answers.
     */
    @Override
    public FormSubmissionDTO save(FormSubmissionDTO formSubmissionDTO) {
        FormSubmission stored = formSubmissionDTO.getId() != null
                ? jpaRepository.findById(formSubmissionDTO.getId()).orElse(null)
                : null;

        FormSubmission entity;
        if (stored != null) {
            mapper.updateEntity(formSubmissionDTO, stored);
            entity = stored;
        } else {
            entity = mapper.toEntity(formSubmissionDTO);
        }

        FormSubmission saved = jpaRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public Optional<FormSubmissionDTO> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public List<FormSubmissionDTO> findByFormDefinitionId(Long formDefinitionId) {
        return jpaRepository.findByFormDefinitionId(formDefinitionId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormSubmissionDTO> findByFormCode(String formCode) {
        return jpaRepository.findByFormCode(formCode).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormSubmissionDTO> findBySubmittedBy(String submittedBy) {
        return jpaRepository.findBySubmittedBy(submittedBy).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormSubmissionDTO> findByStatus(String status) {
        return jpaRepository.findByStatus(SubmissionStatus.valueOf(status)).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormSubmissionDTO> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<FormSubmissionDTO> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
