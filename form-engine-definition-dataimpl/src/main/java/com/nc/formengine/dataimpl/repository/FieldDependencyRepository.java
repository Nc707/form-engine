package com.nc.formengine.dataimpl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nc.formengine.dataimpl.entity.FieldDependency;

import java.util.List;

@Repository
public interface FieldDependencyRepository extends JpaRepository<FieldDependency, Long> {

    List<FieldDependency> findByTriggerFieldId(Long triggerFieldId);

    /**
     * Every dependency with <em>both</em> endpoints in the given form.
     * <p>
     * Nothing stops a dependency from pointing at fields of two different forms. Requiring both ends
     * keeps those rows out of the evaluator: since it keys its result by field name, and names are
     * only unique per form by convention, a foreign trigger field could otherwise be read as the
     * local field that happens to share its name.
     * <p>
     * The joins are fetched because the mapper reads both endpoints of every row.
     */
    @Query("""
            select d from FieldDependency d
              join fetch d.dependentField dep
              join fetch d.triggerField trg
             where dep.formDefinition.id = :formId
               and trg.formDefinition.id = :formId
            """)
    List<FieldDependency> findByFormDefinitionId(@Param("formId") Long formId);
}