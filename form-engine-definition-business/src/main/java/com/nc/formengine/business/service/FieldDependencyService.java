package com.nc.formengine.business.service;

import com.nc.formengine.model.dto.FieldDependencyDTO;

import java.util.List;
import java.util.Optional;

public interface FieldDependencyService {
    
    FieldDependencyDTO create(FieldDependencyDTO fieldDependencyDTO);
    
    FieldDependencyDTO update(Long id, FieldDependencyDTO fieldDependencyDTO);
    
    Optional<FieldDependencyDTO> findById(Long id);
    
    List<FieldDependencyDTO> findByTriggerFieldId(Long triggerFieldId);
    
    List<FieldDependencyDTO> findAll();
    
    void deleteById(Long id);
}
