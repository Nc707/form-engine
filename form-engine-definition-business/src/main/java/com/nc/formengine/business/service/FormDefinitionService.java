package com.nc.formengine.business.service;

import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Reads and edits form definitions, and moves them through their lifecycle.
 *
 * <p>A form is identified by its {@code code} across every version of it, and by
 * {@code (code, version)} as a single row. Editing is only ever allowed while a version is a
 * {@link FormDefinitionStatus#DRAFT}; once published it is frozen, and the way to change it is
 * {@link #createNewVersion(Long)}.
 */
public interface FormDefinitionService {

    /** Creates version 1 of a new code, as a DRAFT. Fails if the code already exists. */
    FormDefinitionDTO create(FormDefinitionDTO formDefinitionDTO);

    /**
     * Edits a draft in place.
     *
     * @throws com.nc.formengine.model.exception.FormDefinitionNotEditableException
     *         if the stored definition is no longer a DRAFT
     */
    FormDefinitionDTO update(Long id, FormDefinitionDTO formDefinitionDTO);

    Optional<FormDefinitionDTO> findById(Long id);

    /** The newest version carrying this code, whatever its status. */
    Optional<FormDefinitionDTO> findByCode(String code);

    Optional<FormDefinitionDTO> findByCodeAndVersion(String code, Integer version);

    /**
     * The version of this code that is currently live. This is what a renderer should ask for: it is
     * the only version that accepts submissions.
     */
    Optional<FormDefinitionDTO> findLatestPublishedByCode(String code);

    /** Every version of this code, oldest first. */
    List<FormDefinitionDTO> findAllVersionsByCode(String code);

    List<FormDefinitionDTO> findByStatus(FormDefinitionStatus status);

    List<FormDefinitionDTO> findAll();

    /**
     * Publishes a draft, archiving whichever version of the same code was published before it, so
     * that exactly one version per code is ever live.
     *
     * @throws com.nc.formengine.model.exception.InvalidFormDefinitionTransitionException
     *         if the definition is not a DRAFT
     */
    FormDefinitionDTO publish(Long id);

    /**
     * Deep-copies a definition into a new DRAFT version of the same code, one past the highest
     * version that code has. The source is left untouched, so published versions and the submissions
     * that reference them are unaffected.
     */
    FormDefinitionDTO createNewVersion(Long id);

    /**
     * Deletes a draft.
     *
     * @throws com.nc.formengine.model.exception.FormDefinitionNotEditableException
     *         if the definition has been published, since submissions may reference it
     */
    void deleteById(Long id);

    boolean existsByCode(String code);
}
