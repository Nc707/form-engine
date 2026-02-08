package com.nc.formengine.dataimpl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nc.formengine.dataimpl.entity.FieldDefinition;

import java.util.List;

@Repository
public interface FieldRepository extends JpaRepository<FieldDefinition, Long> {

    List<FieldDefinition> findByFormDefinitionIdOrderByOrderIndexAsc(Long formDefinitionId);
    
    void deleteByFormDefinitionId(Long formDefinitionId);
}