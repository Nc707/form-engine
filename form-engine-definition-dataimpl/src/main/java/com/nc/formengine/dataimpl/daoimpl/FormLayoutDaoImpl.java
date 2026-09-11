package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FormLayoutDao;
import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FieldLayout;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.entity.FormLayout;
import com.nc.formengine.dataimpl.mapper.FieldLayoutMapper;
import com.nc.formengine.dataimpl.mapper.FormLayoutMapper;
import com.nc.formengine.dataimpl.repository.FieldRepository;
import com.nc.formengine.dataimpl.repository.FormLayoutRepository;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.exception.FieldDefinitionNotFoundException;
import com.nc.formengine.model.exception.FormDefinitionNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FormLayoutDaoImpl implements FormLayoutDao {

    private final FormLayoutRepository jpaRepository;
    private final FormRepository formRepository;
    private final FieldRepository fieldRepository;
    private final FormLayoutMapper mapper;
    private final FieldLayoutMapper fieldLayoutMapper;

    /**
     * Creates the layout, or updates the stored one when the DTO names it.
     *
     * <p>An update reads the stored entity and applies the DTO onto it rather than merging a
     * detached one built from scratch, for the same reason {@code FormDaoImpl} does: the layout owns
     * its field placements with {@code orphanRemoval}, so handing Hibernate a fresh entity whose
     * collection starts out empty would delete every placement before the new ones were even added.
     * Unlike a field's restrictions and options, {@link FormLayoutDTO#getFieldLayouts()} is never
     * partial — the DTO always speaks for the complete set of placements — so there is no "the DTO
     * said nothing about this one" case to preserve; every save replaces the whole collection.
     */
    @Override
    public FormLayoutDTO save(FormLayoutDTO layoutDTO) {
        FormLayout stored = layoutDTO.getId() != null
                ? jpaRepository.findById(layoutDTO.getId()).orElse(null)
                : null;

        FormLayout entity;
        if (stored != null) {
            stored.setDeviceType(layoutDTO.getDeviceType());
            entity = stored;
        } else {
            entity = mapper.toEntity(layoutDTO);
        }

        if (layoutDTO.getFormDefinitionId() != null) {
            FormDefinition formDefinition = formRepository.findById(layoutDTO.getFormDefinitionId())
                    .orElseThrow(() -> new FormDefinitionNotFoundException(layoutDTO.getFormDefinitionId()));
            entity.setFormDefinition(formDefinition);
        }

        applyFieldLayouts(layoutDTO.getFieldLayouts(), entity);

        FormLayout saved = jpaRepository.save(entity);
        return mapper.toDTO(saved);
    }

    /**
     * Resolving which {@code FieldDefinition} each placement points at needs {@link #fieldRepository},
     * which the mapper does not have — every other DAO resolves its entity's foreign references here
     * for the same reason. Whether those fields actually belong to this layout's form is a question
     * for the service: refusing a layout that places another form's fields is a domain rule (422),
     * not a "does this row exist" lookup (404), and the service is where the other DAOs' callers make
     * that same distinction.
     */
    private void applyFieldLayouts(List<FieldLayoutDTO> fieldLayoutDTOs, FormLayout entity) {
        List<FieldLayout> placements = new ArrayList<>();
        if (fieldLayoutDTOs != null) {
            for (FieldLayoutDTO dto : fieldLayoutDTOs) {
                if (dto == null) {
                    continue;
                }
                FieldDefinition fieldDefinition = fieldRepository.findById(dto.getFieldDefinitionId())
                        .orElseThrow(() -> new FieldDefinitionNotFoundException(dto.getFieldDefinitionId()));

                FieldLayout placement = fieldLayoutMapper.toEntity(dto);
                placement.setFormLayout(entity);
                placement.setFieldDefinition(fieldDefinition);
                placements.add(placement);
            }
        }

        // Mutated in place rather than replaced: orphanRemoval only recognises rows dropped from the
        // very collection instance it tracks, so handing the entity a brand new list would leave
        // Hibernate unable to tell what was removed.
        if (entity.getFieldLayouts() == null) {
            entity.setFieldLayouts(placements);
        } else {
            entity.getFieldLayouts().clear();
            entity.getFieldLayouts().addAll(placements);
        }
    }

    @Override
    public Optional<FormLayoutDTO> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public List<FormLayoutDTO> findByFormDefinitionId(Long formDefinitionId) {
        return jpaRepository.findByFormDefinitionId(formDefinitionId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FormLayoutDTO> findByFormDefinitionIdAndDeviceType(Long formDefinitionId, DeviceType deviceType) {
        // The generic layout is stored with a NULL device_type column, and SQL equality never
        // matches NULL, so the derived finder for that case has to be a separate "is null" query.
        Optional<FormLayout> found = deviceType == null
                ? jpaRepository.findByFormDefinitionIdAndDeviceTypeIsNull(formDefinitionId)
                : jpaRepository.findByFormDefinitionIdAndDeviceType(formDefinitionId, deviceType);
        return found.map(mapper::toDTO);
    }

    /**
     * Deletes a layout, taking it out of its form's collection as well.
     *
     * <p>Both halves are needed, for the same reason {@code FieldDefinitionDaoImpl.deleteById} keeps
     * them: a form owns its layouts with {@code cascade = ALL}, so deleting the row while a loaded
     * parent still lists the layout makes the cascade write it straight back at flush.
     */
    @Override
    public void deleteById(Long id) {
        jpaRepository.findById(id).ifPresent(layout -> {
            FormDefinition owner = layout.getFormDefinition();
            if (owner != null && owner.getLayouts() != null) {
                owner.getLayouts().removeIf(sibling -> id.equals(sibling.getId()));
            }
            jpaRepository.delete(layout);
        });
    }
}
