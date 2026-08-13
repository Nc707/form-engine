package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.exception.DuplicateResourceException;
import com.nc.formengine.model.exception.FieldDefinitionNotFoundException;
import com.nc.formengine.model.rules.DefinitionRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The fields of a form.
 *
 * <p>Writes are refused unless the form is still a draft, and unless the field is one the engine could
 * actually work with. Both used to be checked only by the Vaadin builder, which made every rule here a
 * suggestion to anyone using the REST API.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FieldDefinitionServiceImpl implements FieldDefinitionService {

    private final FieldDefinitionDao fieldDefinitionDao;
    private final DefinitionMutationGuard guard;

    @Override
    public FieldDefinitionDTO create(FieldDefinitionDTO fieldDefinitionDTO) {
        if (fieldDefinitionDTO.getId() != null) {
            throw new IllegalArgumentException("New field should not have an ID");
        }
        guard.requireDraft(fieldDefinitionDTO.getFormDefinitionId());
        requireCoherent(fieldDefinitionDTO);
        return fieldDefinitionDao.save(fieldDefinitionDTO);
    }

    @Override
    public FieldDefinitionDTO update(Long id, FieldDefinitionDTO fieldDefinitionDTO) {
        FieldDefinitionDTO stored = fieldDefinitionDao.findById(id)
                .orElseThrow(() -> new FieldDefinitionNotFoundException(id));
        guard.requireDraft(stored.getFormDefinitionId());

        fieldDefinitionDTO.setId(id);
        // A field cannot be moved between forms: the answers already given for it point at this one.
        fieldDefinitionDTO.setFormDefinitionId(stored.getFormDefinitionId());
        requireCoherent(fieldDefinitionDTO);
        return fieldDefinitionDao.save(fieldDefinitionDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FieldDefinitionDTO> findById(Long id) {
        return fieldDefinitionDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDefinitionDTO> findByFormDefinitionId(Long formDefinitionId) {
        return fieldDefinitionDao.findByFormDefinitionId(formDefinitionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDefinitionDTO> findAll() {
        return fieldDefinitionDao.findAll();
    }

    @Override
    public void deleteById(Long id) {
        fieldDefinitionDao.findById(id)
                .ifPresent(stored -> guard.requireDraft(stored.getFormDefinitionId()));
        fieldDefinitionDao.deleteById(id);
    }

    @Override
    public void deleteByFormDefinitionId(Long formDefinitionId) {
        guard.requireDraft(formDefinitionId);
        fieldDefinitionDao.deleteByFormDefinitionId(formDefinitionId);
    }

    /**
     * Checks the field against the rules of the model, and against the form's other fields.
     *
     * <p>The siblings are what makes the name check possible: a duplicate name is only visible from the
     * form, and the evaluator resolves fields by name, so two fields sharing one silently drops the
     * other from both validation and dependency resolution.
     */
    private void requireCoherent(FieldDefinitionDTO field) {
        List<FieldDefinitionDTO> siblings =
                fieldDefinitionDao.findByFormDefinitionId(field.getFormDefinitionId()).stream()
                        .filter(other -> !Objects.equals(other.getId(), field.getId()))
                        .toList();

        boolean nameTaken = field.getName() != null && siblings.stream()
                .map(FieldDefinitionDTO::getName)
                .filter(Objects::nonNull)
                .anyMatch(other -> other.equalsIgnoreCase(field.getName()));
        if (nameTaken) {
            // A conflict with something already stored, so 409 rather than the 422 the rest get.
            throw new DuplicateResourceException("Field", "name", field.getName());
        }

        // Siblings are left out: the only rule that reads them is the name clash, already answered.
        guard.requireNoProblems("The field '" + field.getName() + "'",
                DefinitionRules.checkField(field, List.of()));
    }
}
