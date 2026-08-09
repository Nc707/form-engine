package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FormDao;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.mapper.FormDefinitionMapper;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FormDaoImpl implements FormDao {

    private final FormRepository jpaRepository;
    private final FormDefinitionMapper mapper;
    private final FormDefinitionVersionCopier versionCopier;

    /**
     * Creates the form, or updates the stored one when the DTO names it.
     *
     * <p>An update reads the stored entity and applies the DTO onto it rather than merging a
     * detached one built from scratch, for the same reason {@code FieldDefinitionDaoImpl} does: the
     * form owns its fields and layouts with {@code orphanRemoval}, so merging an entity whose
     * collections are empty deletes every one of them. Reading first also preserves the fields the
     * DTO does not speak for — {@code status} in particular, which the lifecycle owns and a caller's
     * update body has no business resetting.
     */
    @Override
    public FormDefinitionDTO save(FormDefinitionDTO formDTO) {
        FormDefinition stored = formDTO.getId() != null
                ? jpaRepository.findById(formDTO.getId()).orElse(null)
                : null;

        FormDefinition entity;
        if (stored != null) {
            mapper.updateEntity(formDTO, stored);
            entity = stored;
        } else {
            entity = mapper.toEntity(formDTO);
        }

        FormDefinition saved = jpaRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public Optional<FormDefinitionDTO> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public Optional<FormDefinitionDTO> findByCode(String code) {
        return jpaRepository.findFirstByCodeOrderByVersionDesc(code)
                .map(mapper::toDTO);
    }

    @Override
    public Optional<FormDefinitionDTO> findByCodeAndVersion(String code, Integer version) {
        return jpaRepository.findByCodeAndVersion(code, version)
                .map(mapper::toDTO);
    }

    @Override
    public Optional<FormDefinitionDTO> findLatestPublishedByCode(String code) {
        return jpaRepository
                .findFirstByCodeAndStatusOrderByVersionDesc(code, FormDefinitionStatus.PUBLISHED)
                .map(mapper::toDTO);
    }

    @Override
    public List<FormDefinitionDTO> findAllVersionsByCode(String code) {
        return jpaRepository.findByCodeOrderByVersionAsc(code).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FormDefinitionDTO> copyAsNewVersion(Long sourceId) {
        return jpaRepository.findById(sourceId)
                .map(source -> versionCopier.copy(source, nextVersionFor(source.getCode())))
                .map(mapper::toDTO);
    }

    /**
     * One past the highest version of this code. Read from the store rather than from the source
     * being copied, so branching off an old version still lands at the head instead of colliding
     * with a version that already exists.
     */
    private Integer nextVersionFor(String code) {
        return jpaRepository.findFirstByCodeOrderByVersionDesc(code)
                .map(latest -> latest.getVersion() + 1)
                .orElse(1);
    }

    @Override
    public List<FormDefinitionDTO> findByStatus(FormDefinitionStatus status) {
        return jpaRepository.findByStatusOrderByCodeAsc(status).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormDefinitionDTO> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<FormDefinitionDTO> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndVersion(String code, Integer version) {
        return jpaRepository.existsByCodeAndVersion(code, version);
    }
}
