package com.nc.formengine.dataimpl.repository;

import com.nc.formengine.data.entity.FieldOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldOptionRepository extends JpaRepository<FieldOption, Long> {
    
    List<FieldOption> findByFieldDefinitionIdOrderByOrderIndexAsc(Long fieldDefinitionId);
}