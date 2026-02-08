package com.nc.formengine.dataimpl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nc.formengine.dataimpl.entity.FieldOption;

import java.util.List;

@Repository
public interface FieldOptionRepository extends JpaRepository<FieldOption, Long> {
    
    List<FieldOption> findByFieldDefinitionIdOrderByOrderIndexAsc(Long fieldDefinitionId);
}