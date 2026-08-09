package com.nc.formengine.data.dao;

import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface FormDao {

    FormDefinitionDTO save(FormDefinitionDTO formDTO);

    /**
     * The newest version carrying this code, whatever its status.
     *
     * <p>A code names a form across every version of it, so it no longer picks out a single row.
     * "Newest" is the reading callers almost always want when they name a form by code without
     * saying which version; {@link #findLatestPublishedByCode(String)} is the one to use when what
     * is wanted is the version that is actually live.
     */
    Optional<FormDefinitionDTO> findByCode(String code);

    Optional<FormDefinitionDTO> findByCodeAndVersion(String code, Integer version);

    /** The single {@link FormDefinitionStatus#PUBLISHED} version for this code, if there is one. */
    Optional<FormDefinitionDTO> findLatestPublishedByCode(String code);

    /** Every version of this code, oldest first. */
    List<FormDefinitionDTO> findAllVersionsByCode(String code);

    /**
     * Deep-copies the given definition into a new DRAFT version of the same code, numbered one past
     * the highest version that code currently has.
     *
     * <p>Fields, restrictions, options, layouts and dependencies are all recreated, with the
     * references between them repointed at the copies. The source is left untouched.
     *
     * @param sourceId the definition to copy from
     * @return the new version, or empty if {@code sourceId} does not exist
     */
    Optional<FormDefinitionDTO> copyAsNewVersion(Long sourceId);

    List<FormDefinitionDTO> findByStatus(FormDefinitionStatus status);

    List<FormDefinitionDTO> findAll();

    Page<FormDefinitionDTO> findAll(Pageable pageable);

    void deleteById(Long id);

    Optional<FormDefinitionDTO> findById(Long id);

    boolean existsByCode(String code);

    boolean existsByCodeAndVersion(String code, Integer version);
}
