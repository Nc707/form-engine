package com.nc.formengine.dataimpl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nc.formengine.dataimpl.entity.FieldDependency;

import java.util.List;

@Repository
public interface FieldDependencyRepository extends JpaRepository<FieldDependency, Long> {

    List<FieldDependency> findByTriggerFieldId(Long triggerFieldId);
}