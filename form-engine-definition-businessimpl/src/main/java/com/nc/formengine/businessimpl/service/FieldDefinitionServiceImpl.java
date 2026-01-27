package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FieldDefinitionServiceImpl implements FieldDefinitionService {

    private final FieldDefinitionDao fieldDefinitionDao;

    @Override
    public FieldDefinitionDTO create(FieldDefinitionDTO fieldDefinitionDTO) {
        if (fieldDefinitionDTO.getId() != null) {
            throw new IllegalArgumentException("New field should not have an ID");
        }
        return fieldDefinitionDao.save(fieldDefinitionDTO);
    }

    @Override
    public FieldDefinitionDTO update(Long id, FieldDefinitionDTO fieldDefinitionDTO) {
        FieldDefinitionDTO existing = fieldDefinitionDao.findById(id)
                .orElseThrow(() -> new RuntimeException("Field not found with id: " + id));
        
        fieldDefinitionDTO.setId(id);
        return fieldDefinitionDao.save(fieldDefinitionDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FieldDefinitionDTO> findById(Long id) {
        return fieldDefinitionDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDefinitionDTO> findByFormDefinitionId(Long formDefinitionId) {
        return fieldDefinitionDao.findByFormDefinitionId(formDefinitionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDefinitionDTO> findAll() {
        return fieldDefinitionDao.findAll();
    }

    @Override
    public void deleteById(Long id) {
        fieldDefinitionDao.deleteById(id);
    }

    @Override
    public void deleteByFormDefinitionId(Long formDefinitionId) {
        fieldDefinitionDao.deleteByFormDefinitionId(formDefinitionId);
    }
}
