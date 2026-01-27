package com.nc.formengine.data.dao;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import java.util.List;
import java.util.Optional;

public interface FieldDefinitionDao {
    
    FieldDefinitionDTO save(FieldDefinitionDTO fieldDefinitionDTO);
    
    Optional<FieldDefinitionDTO> findById(Long id);
    
    List<FieldDefinitionDTO> findByFormDefinitionId(Long formDefinitionId);
    
    List<FieldDefinitionDTO> findAll();
    
    void deleteById(Long id);
    
    void deleteByFormDefinitionId(Long formDefinitionId);
}
