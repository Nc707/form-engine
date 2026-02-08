package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.mapper.FieldDefinitionMapper;
import com.nc.formengine.dataimpl.repository.FieldRepository;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FieldDefinitionDaoImpl implements FieldDefinitionDao {

    private final FieldRepository jpaRepository;
    private final FormRepository formRepository;
    private final FieldDefinitionMapper mapper;

    @Override
    public FieldDefinitionDTO save(FieldDefinitionDTO fieldDefinitionDTO) {
        FieldDefinition entity = mapper.toEntity(fieldDefinitionDTO);
        
        if (fieldDefinitionDTO.getFormDefinitionId() != null) {
            FormDefinition formDefinition = formRepository.findById(fieldDefinitionDTO.getFormDefinitionId())
                    .orElseThrow(() -> new RuntimeException("Form not found"));
            mapper.setFormDefinition(entity, formDefinition);
        }
        
        FieldDefinition saved = jpaRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public Optional<FieldDefinitionDTO> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public List<FieldDefinitionDTO> findByFormDefinitionId(Long formDefinitionId) {
        return jpaRepository.findByFormDefinitionIdOrderByOrderIndexAsc(formDefinitionId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FieldDefinitionDTO> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByFormDefinitionId(Long formDefinitionId) {
        jpaRepository.deleteByFormDefinitionId(formDefinitionId);
    }
}
