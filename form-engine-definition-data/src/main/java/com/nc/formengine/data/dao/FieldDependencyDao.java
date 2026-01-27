package com.nc.formengine.data.dao;

import com.nc.formengine.model.dto.FieldDependencyDTO;
import java.util.List;
import java.util.Optional;

public interface FieldDependencyDao {
    
    FieldDependencyDTO save(FieldDependencyDTO fieldDependencyDTO);
    
    Optional<FieldDependencyDTO> findById(Long id);
    
    List<FieldDependencyDTO> findByTriggerFieldId(Long triggerFieldId);
    
    List<FieldDependencyDTO> findAll();
    
    void deleteById(Long id);
}
