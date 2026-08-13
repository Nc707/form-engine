package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FieldDependencyService;
import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.data.dao.FieldDependencyDao;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.exception.FieldDependencyNotFoundException;
import com.nc.formengine.model.exception.ValidationFailedException;
import com.nc.formengine.model.rules.DefinitionRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The show/hide/require rules between a form's fields.
 *
 * <p>A dependency names two fields, and both have to belong to the same draft form: one pointing across
 * forms would be evaluated against answers that cannot exist. The condition also has to mean something
 * for the trigger's type, and the value it compares against has to be there at all — a blank one is a
 * rule that can never fire, which is indistinguishable from a rule nobody wrote.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FieldDependencyServiceImpl implements FieldDependencyService {

    private final FieldDependencyDao fieldDependencyDao;
    private final FieldDefinitionDao fieldDefinitionDao;
    private final DefinitionMutationGuard guard;

    @Override
    public FieldDependencyDTO create(FieldDependencyDTO fieldDependencyDTO) {
        if (fieldDependencyDTO.getId() != null) {
            throw new IllegalArgumentException("New field dependency should not have an ID");
        }
        requireCoherent(fieldDependencyDTO);
        return fieldDependencyDao.save(fieldDependencyDTO);
    }

    @Override
    public FieldDependencyDTO update(Long id, FieldDependencyDTO fieldDependencyDTO) {
        FieldDependencyDTO stored = fieldDependencyDao.findById(id)
                .orElseThrow(() -> new FieldDependencyNotFoundException(id));
        guard.requireDraftOfField(stored.getTriggerFieldId());

        fieldDependencyDTO.setId(id);
        requireCoherent(fieldDependencyDTO);
        return fieldDependencyDao.save(fieldDependencyDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FieldDependencyDTO> findById(Long id) {
        return fieldDependencyDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDependencyDTO> findByTriggerFieldId(Long triggerFieldId) {
        return fieldDependencyDao.findByTriggerFieldId(triggerFieldId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDependencyDTO> findAll() {
        return fieldDependencyDao.findAll();
    }

    @Override
    public void deleteById(Long id) {
        fieldDependencyDao.findById(id)
                .ifPresent(stored -> guard.requireDraftOfField(stored.getTriggerFieldId()));
        fieldDependencyDao.deleteById(id);
    }

    private void requireCoherent(FieldDependencyDTO dependency) {
        FieldDefinitionDTO trigger = guard.requireDraftOfField(dependency.getTriggerFieldId());
        FieldDefinitionDTO dependent = guard.requireDraftOfField(dependency.getDependentFieldId());

        if (!trigger.getFormDefinitionId().equals(dependent.getFormDefinitionId())) {
            // Evaluation resolves both fields within one form's answers, so a rule spanning two forms
            // could never be satisfied and could never be seen to fail either.
            throw new ValidationFailedException("The dependency spans two forms",
                    "A dependency's trigger and dependent field must belong to the same form.");
        }

        List<FieldDefinitionDTO> fields =
                fieldDefinitionDao.findByFormDefinitionId(trigger.getFormDefinitionId());
        guard.requireNoProblems("The dependency", DefinitionRules.checkDependency(dependency, fields));
    }
}
