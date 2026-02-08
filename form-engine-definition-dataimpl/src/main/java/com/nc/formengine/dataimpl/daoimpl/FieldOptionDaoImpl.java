package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FieldOptionDao;
import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FieldOption;
import com.nc.formengine.dataimpl.mapper.FieldOptionMapper;
import com.nc.formengine.dataimpl.repository.FieldOptionRepository;
import com.nc.formengine.dataimpl.repository.FieldRepository;
import com.nc.formengine.model.dto.FieldOptionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FieldOptionDaoImpl implements FieldOptionDao {

    private final FieldOptionRepository jpaRepository;
    private final FieldRepository fieldRepository;
    private final FieldOptionMapper mapper;

    @Override
    public FieldOptionDTO save(FieldOptionDTO fieldOptionDTO) {
        FieldOption entity = mapper.toEntity(fieldOptionDTO);
        
        if (fieldOptionDTO.getFieldDefinitionId() != null) {
            FieldDefinition fieldDefinition = fieldRepository.findById(fieldOptionDTO.getFieldDefinitionId())
                    .orElseThrow(() -> new RuntimeException("Field not found"));
            mapper.setFieldDefinition(entity, fieldDefinition);
        }
        
        FieldOption saved = jpaRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public Optional<FieldOptionDTO> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public List<FieldOptionDTO> findByFieldDefinitionId(Long fieldDefinitionId) {
        return jpaRepository.findByFieldDefinitionIdOrderByOrderIndexAsc(fieldDefinitionId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FieldOptionDTO> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
