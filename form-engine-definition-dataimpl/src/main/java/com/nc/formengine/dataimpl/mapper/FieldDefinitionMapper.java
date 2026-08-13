package com.nc.formengine.dataimpl.mapper;

import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FieldOption;
import com.nc.formengine.dataimpl.entity.FieldRestriction;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps {@link FieldDefinition} to its DTO, together with the two collections it owns.
 *
 * <p><b>Absent is not empty.</b> A null {@code restrictions} or {@code options} on an incoming DTO
 * means "this request says nothing about them", so what is stored is kept; an empty list means
 * "there are none", so what is stored is deleted. Without that distinction, any client PUTting a
 * field it built by hand would silently wipe the rules attached to it.
 */
@Component
public class FieldDefinitionMapper {

    private final FieldRestrictionMapper fieldRestrictionMapper;
    private final FieldOptionMapper fieldOptionMapper;

    public FieldDefinitionMapper(FieldRestrictionMapper fieldRestrictionMapper,
                                 FieldOptionMapper fieldOptionMapper) {
        this.fieldRestrictionMapper = fieldRestrictionMapper;
        this.fieldOptionMapper = fieldOptionMapper;
    }

    public FieldDefinitionDTO toDTO(FieldDefinition entity) {
        if (entity == null) {
            return null;
        }

        return FieldDefinitionDTO.builder()
                .id(entity.getId())
                .formDefinitionId(entity.getFormDefinition() != null ?
                    entity.getFormDefinition().getId() : null)
                .name(entity.getName())
                .label(entity.getLabel())
                .type(entity.getType())
                .orderIndex(entity.getOrderIndex())
                .required(entity.getRequired())
                .requiredMessage(entity.getRequiredMessage())
                .restrictions(entity.getRestrictions() != null
                    ? entity.getRestrictions().stream()
                        .map(fieldRestrictionMapper::toDTO)
                        .collect(Collectors.toList()) : new ArrayList<>())
                .options(entity.getOptions() != null
                    ? entity.getOptions().stream()
                        .map(fieldOptionMapper::toDTO)
                        .collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    public FieldDefinition toEntity(FieldDefinitionDTO dto) {
        if (dto == null) {
            return null;
        }

        FieldDefinition entity = new FieldDefinition();
        entity.setId(dto.getId());
        entity.setRestrictions(new ArrayList<>());
        entity.setOptions(new ArrayList<>());
        updateEntity(dto, entity);

        return entity;
    }

    public void updateEntity(FieldDefinitionDTO dto, FieldDefinition entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setLabel(dto.getLabel());
        entity.setType(dto.getType());
        entity.setOrderIndex(dto.getOrderIndex());
        entity.setRequired(dto.getRequired());
        entity.setRequiredMessage(dto.getRequiredMessage());
        applyRestrictions(dto.getRestrictions(), entity);
        applyOptions(dto.getOptions(), entity);
    }

    public void setFormDefinition(FieldDefinition entity, FormDefinition formDefinition) {
        if (entity != null) {
            entity.setFormDefinition(formDefinition);
        }
    }

    /**
     * Reconciles the stored restrictions with the ones the DTO carries.
     *
     * <p>A restriction already belonging to this field is updated in place, so its row and its id
     * survive the request. The collection is then rebuilt in the order of the DTO: whatever the DTO
     * dropped is gone from it, and {@code orphanRemoval} turns that into a delete.
     */
    private void applyRestrictions(List<FieldRestrictionDTO> restrictions, FieldDefinition entity) {
        if (restrictions == null) {
            return;
        }

        Map<Long, FieldRestriction> stored = byId(entity.getRestrictions(), FieldRestriction::getId);
        List<FieldRestriction> updated = new ArrayList<>();

        for (FieldRestrictionDTO dto : restrictions) {
            if (dto == null) {
                continue;
            }

            FieldRestriction target = dto.getId() != null ? stored.get(dto.getId()) : null;
            if (target != null) {
                fieldRestrictionMapper.updateEntity(dto, target);
            } else {
                target = fieldRestrictionMapper.toEntity(dto);
                // An id naming a restriction of some other field is not ours to reuse: this one is
                // new here, and cascading it with an assigned id would fail as a detached entity.
                target.setId(null);
            }
            fieldRestrictionMapper.setFieldDefinition(target, entity);
            updated.add(target);
        }

        replaceInPlace(entity.getRestrictions(), updated);
    }

    private void applyOptions(List<FieldOptionDTO> options, FieldDefinition entity) {
        if (options == null) {
            return;
        }

        Map<Long, FieldOption> stored = byId(entity.getOptions(), FieldOption::getId);
        List<FieldOption> updated = new ArrayList<>();

        for (FieldOptionDTO dto : options) {
            if (dto == null) {
                continue;
            }

            FieldOption target = dto.getId() != null ? stored.get(dto.getId()) : null;
            if (target != null) {
                fieldOptionMapper.updateEntity(dto, target);
            } else {
                target = fieldOptionMapper.toEntity(dto);
                target.setId(null);
            }
            fieldOptionMapper.setFieldDefinition(target, entity);
            updated.add(target);
        }

        replaceInPlace(entity.getOptions(), updated);
    }

    private <T> Map<Long, T> byId(List<T> stored, Function<T, Long> idOf) {
        Map<Long, T> index = new HashMap<>();
        if (stored == null) {
            return index;
        }

        for (T element : stored) {
            Long id = idOf.apply(element);
            if (id != null) {
                index.put(id, element);
            }
        }
        return index;
    }

    /**
     * Rewrites a collection without swapping the instance, which is what {@code orphanRemoval}
     * requires: handing the entity a brand new list would leave Hibernate unable to tell what was
     * removed.
     */
    private <T> void replaceInPlace(List<T> target, List<T> replacement) {
        target.clear();
        target.addAll(replacement);
    }
}
