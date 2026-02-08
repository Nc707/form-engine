package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FormDao;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.mapper.FormDefinitionMapper;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FormDefinitionDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FormDaoImpl implements FormDao {

    private final FormRepository jpaRepository;
    private final FormDefinitionMapper mapper;

    @Override
    public FormDefinitionDTO save(FormDefinitionDTO formDTO) {
        FormDefinition entity = mapper.toEntity(formDTO);
        FormDefinition saved = jpaRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public Optional<FormDefinitionDTO> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public Optional<FormDefinitionDTO> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .map(mapper::toDTO);
    }

    @Override
    public List<FormDefinitionDTO> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<FormDefinitionDTO> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }
}