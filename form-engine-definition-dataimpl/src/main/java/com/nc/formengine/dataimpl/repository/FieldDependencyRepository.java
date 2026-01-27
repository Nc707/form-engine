package com.nc.formengine.dataimpl.repository;

import com.nc.formengine.data.entity.FieldDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldDependencyRepository extends JpaRepository<FieldDependency, Long> {

    List<FieldDependency> findByTriggerFieldId(Long triggerFieldId);
}