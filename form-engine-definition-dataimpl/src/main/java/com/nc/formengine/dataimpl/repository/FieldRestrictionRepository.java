package com.nc.formengine.dataimpl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nc.formengine.dataimpl.entity.FieldRestriction;

import java.util.List;

@Repository
public interface FieldRestrictionRepository extends JpaRepository<FieldRestriction, Long> {

    List<FieldRestriction> findByFieldDefinitionIdOrderByOrderIndexAsc(Long fieldDefinitionId);
}
