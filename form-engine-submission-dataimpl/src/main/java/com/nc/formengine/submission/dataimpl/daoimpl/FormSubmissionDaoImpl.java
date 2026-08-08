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

    @Override
    public FormSubmissionDTO save(FormSubmissionDTO formSubmissionDTO) {
        FormSubmission entity = mapper.toEntity(formSubmissionDTO);
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
