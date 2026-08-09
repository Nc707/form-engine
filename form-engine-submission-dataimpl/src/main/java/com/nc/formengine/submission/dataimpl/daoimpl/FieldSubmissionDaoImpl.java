package com.nc.formengine.submission.dataimpl.daoimpl;

import com.nc.formengine.submission.data.dao.FieldSubmissionDao;
import com.nc.formengine.submission.dataimpl.entity.FieldSubmission;
import com.nc.formengine.submission.dataimpl.entity.FormSubmission;
import com.nc.formengine.submission.dataimpl.mapper.FieldSubmissionMapper;
import com.nc.formengine.submission.dataimpl.repository.FieldSubmissionRepository;
import com.nc.formengine.submission.dataimpl.repository.FormSubmissionRepository;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.exception.FormSubmissionNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FieldSubmissionDaoImpl implements FieldSubmissionDao {

    private final FieldSubmissionRepository jpaRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final FieldSubmissionMapper mapper;

    @Override
    public FieldSubmissionDTO save(FieldSubmissionDTO fieldSubmissionDTO) {
        FieldSubmission entity = mapper.toEntity(fieldSubmissionDTO);

        if (fieldSubmissionDTO.getFormSubmissionId() != null) {
            FormSubmission formSubmission = formSubmissionRepository.findById(fieldSubmissionDTO.getFormSubmissionId())
                    .orElseThrow(() -> new FormSubmissionNotFoundException(
                            fieldSubmissionDTO.getFormSubmissionId()));
            mapper.setFormSubmission(entity, formSubmission);
        }

        FieldSubmission saved = jpaRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public Optional<FieldSubmissionDTO> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public List<FieldSubmissionDTO> findByFormSubmissionId(Long formSubmissionId) {
        return jpaRepository.findByFormSubmissionId(formSubmissionId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FieldSubmissionDTO> findByFieldDefinitionId(Long fieldDefinitionId) {
        return jpaRepository.findByFieldDefinitionId(fieldDefinitionId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FieldSubmissionDTO> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByFormSubmissionId(Long formSubmissionId) {
        jpaRepository.deleteByFormSubmissionId(formSubmissionId);
    }
}
