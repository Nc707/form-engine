package com.nc.formengine.business.service;

import com.nc.formengine.model.dto.FormDefinitionDTO;

import java.util.List;
import java.util.Optional;

public interface FormDefinitionService {
    
    FormDefinitionDTO create(FormDefinitionDTO formDefinitionDTO);
    
    FormDefinitionDTO update(Long id, FormDefinitionDTO formDefinitionDTO);
    
    Optional<FormDefinitionDTO> findById(Long id);
    
    Optional<FormDefinitionDTO> findByCode(String code);
    
    List<FormDefinitionDTO> findAll();
    
    void deleteById(Long id);
    
    boolean existsByCode(String code);
}
