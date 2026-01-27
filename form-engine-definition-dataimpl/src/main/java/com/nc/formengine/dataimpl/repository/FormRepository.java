package com.nc.formengine.dataimpl.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nc.formengine.data.entity.FormDefinition;

public interface FormRepository extends JpaRepository<FormDefinition, Long> {
	Optional<FormDefinition> findByCode(String code);
	
	boolean existsByCode(String code);
}
