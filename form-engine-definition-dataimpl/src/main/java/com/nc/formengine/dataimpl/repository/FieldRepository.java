package com.nc.formengine.dataimpl.repository;

import com.nc.formengine.data.entity.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldRepository extends JpaRepository<FieldDefinition, Long> {

    List<FieldDefinition> findByFormDefinitionIdOrderByOrderIndexAsc(Long formDefinitionId);
    
    void deleteByFormDefinitionId(Long formDefinitionId);
}