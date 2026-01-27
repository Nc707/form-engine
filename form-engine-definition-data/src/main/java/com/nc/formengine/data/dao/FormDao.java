package com.nc.formengine.data.dao;

import com.nc.formengine.model.dto.FormDefinitionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface FormDao {
    
    FormDefinitionDTO save(FormDefinitionDTO formDTO);
    
    Optional<FormDefinitionDTO> findById(Long id);
    
    Optional<FormDefinitionDTO> findByCode(String code);
    
    List<FormDefinitionDTO> findAll();
    
    Page<FormDefinitionDTO> findAll(Pageable pageable);
    
    void deleteById(Long id);
    
    boolean existsByCode(String code);
}