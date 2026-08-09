package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.mapper.FieldDefinitionMapper;
import com.nc.formengine.dataimpl.repository.FieldRepository;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FieldDefinitionDaoImpl implements FieldDefinitionDao {

    private final FieldRepository jpaRepository;
    private final FormRepository formRepository;
    private final FieldDefinitionMapper mapper;

    /**
     * Creates the field, or updates the stored one when the DTO names it.
     *
     * <p>An update reads the stored entity and applies the DTO onto it, rather than merging one built
     * from scratch. The field owns its restrictions and its options with {@code orphanRemoval}, and a
     * detached entity cannot express "leave those alone": it would arrive with both collections empty
     * and the merge would delete every row. Reading first also keeps the form association of a DTO
     * that does not carry one, which the previous merge would have nulled out.
     */
    @Override
    public FieldDefinitionDTO save(FieldDefinitionDTO fieldDefinitionDTO) {
        FieldDefinition stored = fieldDefinitionDTO.getId() != null
                ? jpaRepository.findById(fieldDefinitionDTO.getId()).orElse(null)
                : null;

        FieldDefinition entity;
        if (stored != null) {
            mapper.updateEntity(fieldDefinitionDTO, stored);
            entity = stored;
        } else {
            entity = mapper.toEntity(fieldDefinitionDTO);
        }

        if (fieldDefinitionDTO.getFormDefinitionId() != null) {
            FormDefinition formDefinition = formRepository.findById(fieldDefinitionDTO.getFormDefinitionId())
                    .orElseThrow(() -> new RuntimeException("Form not found"));
            mapper.setFormDefinition(entity, formDefinition);
        }

        FieldDefinition saved = jpaRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public Optional<FieldDefinitionDTO> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public List<FieldDefinitionDTO> findByFormDefinitionId(Long formDefinitionId) {
        return jpaRepository.findByFormDefinitionIdOrderByOrderIndexAsc(formDefinitionId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FieldDefinitionDTO> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByFormDefinitionId(Long formDefinitionId) {
        jpaRepository.deleteByFormDefinitionId(formDefinitionId);
    }
}
