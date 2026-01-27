package com.nc.formengine.business.service;

import com.nc.formengine.model.dto.FieldDefinitionDTO;

import java.util.List;
import java.util.Optional;

public interface FieldDefinitionService {
    
    FieldDefinitionDTO create(FieldDefinitionDTO fieldDefinitionDTO);
    
    FieldDefinitionDTO update(Long id, FieldDefinitionDTO fieldDefinitionDTO);
    
    Optional<FieldDefinitionDTO> findById(Long id);
    
    List<FieldDefinitionDTO> findByFormDefinitionId(Long formDefinitionId);
    
    List<FieldDefinitionDTO> findAll();
    
    void deleteById(Long id);
    
    void deleteByFormDefinitionId(Long formDefinitionId);
}
