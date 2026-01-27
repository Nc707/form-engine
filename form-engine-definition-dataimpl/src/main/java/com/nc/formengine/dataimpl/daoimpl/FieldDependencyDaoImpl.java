package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FieldDependencyDao;
import com.nc.formengine.data.entity.FieldDefinition;
import com.nc.formengine.data.entity.FieldDependency;
import com.nc.formengine.dataimpl.mapper.FieldDependencyMapper;
import com.nc.formengine.dataimpl.repository.FieldDependencyRepository;
import com.nc.formengine.dataimpl.repository.FieldRepository;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FieldDependencyDaoImpl implements FieldDependencyDao {

    private final FieldDependencyRepository jpaRepository;
    private final FieldRepository fieldRepository;
    private final FieldDependencyMapper mapper;

    @Override
    public FieldDependencyDTO save(FieldDependencyDTO fieldDependencyDTO) {
        FieldDependency entity = mapper.toEntity(fieldDependencyDTO);
        
        if (fieldDependencyDTO.getDependentFieldId() != null) {
            FieldDefinition dependentField = fieldRepository.findById(fieldDependencyDTO.getDependentFieldId())
                    .orElseThrow(() -> new RuntimeException("Dependent field not found"));
            mapper.setDependentField(entity, dependentField);
        }
        
        if (fieldDependencyDTO.getTriggerFieldId() != null) {
            FieldDefinition triggerField = fieldRepository.findById(fieldDependencyDTO.getTriggerFieldId())
                    .orElseThrow(() -> new RuntimeException("Trigger field not found"));
            mapper.setTriggerField(entity, triggerField);
        }
        
        FieldDependency saved = jpaRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public Optional<FieldDependencyDTO> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public List<FieldDependencyDTO> findByTriggerFieldId(Long triggerFieldId) {
        return jpaRepository.findByTriggerFieldId(triggerFieldId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FieldDependencyDTO> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
