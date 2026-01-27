package com.nc.formengine.business.service;

import com.nc.formengine.model.dto.FieldOptionDTO;

import java.util.List;
import java.util.Optional;

public interface FieldOptionService {
    
    FieldOptionDTO create(FieldOptionDTO fieldOptionDTO);
    
    FieldOptionDTO update(Long id, FieldOptionDTO fieldOptionDTO);
    
    Optional<FieldOptionDTO> findById(Long id);
    
    List<FieldOptionDTO> findByFieldDefinitionId(Long fieldDefinitionId);
    
    List<FieldOptionDTO> findAll();
    
    void deleteById(Long id);
}
