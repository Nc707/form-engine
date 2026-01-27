package com.nc.formengine.data.dao;

import com.nc.formengine.model.dto.FieldOptionDTO;
import java.util.List;
import java.util.Optional;

public interface FieldOptionDao {
    
    FieldOptionDTO save(FieldOptionDTO fieldOptionDTO);
    
    Optional<FieldOptionDTO> findById(Long id);
    
    List<FieldOptionDTO> findByFieldDefinitionId(Long fieldDefinitionId);
    
    List<FieldOptionDTO> findAll();
    
    void deleteById(Long id);
}
