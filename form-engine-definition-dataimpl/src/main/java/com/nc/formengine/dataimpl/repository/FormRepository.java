package com.nc.formengine.dataimpl.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.model.enums.FormDefinitionStatus;

public interface FormRepository extends JpaRepository<FormDefinition, Long> {

	/**
	 * The newest version carrying this code, whatever its status. This is what {@code findByCode}
	 * means once a code spans several versions: the head of that code's history.
	 */
	Optional<FormDefinition> findFirstByCodeOrderByVersionDesc(String code);

	Optional<FormDefinition> findByCodeAndVersion(String code, Integer version);

	/**
	 * The live version for a code. At most one row can match, because publishing archives the
	 * version it replaces; the ordering is a safety net, not a tie-break we expect to need.
	 */
	Optional<FormDefinition> findFirstByCodeAndStatusOrderByVersionDesc(String code, FormDefinitionStatus status);

	List<FormDefinition> findByCodeOrderByVersionAsc(String code);

	List<FormDefinition> findByStatusOrderByCodeAsc(FormDefinitionStatus status);

	boolean existsByCode(String code);

	boolean existsByCodeAndVersion(String code, Integer version);
}
