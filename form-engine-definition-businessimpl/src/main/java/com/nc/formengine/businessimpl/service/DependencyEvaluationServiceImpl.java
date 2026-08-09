package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.DependencyEvaluationService;
import com.nc.formengine.businessimpl.service.dependency.DependencyGraphEvaluator;
import com.nc.formengine.businessimpl.service.dependency.EvaluationResult;
import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.data.dao.FieldDependencyDao;
import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Loads a form's fields and dependencies and hands them to {@link DependencyGraphEvaluator}.
 * <p>
 * All the rules live in the evaluator, which is pure and container-free; this class only bridges it
 * to the data layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DependencyEvaluationServiceImpl implements DependencyEvaluationService {

    private final FieldDefinitionDao fieldDefinitionDao;
    private final FieldDependencyDao fieldDependencyDao;

    @Override
    @Transactional(readOnly = true)
    public Map<String, FieldState> evaluate(Long formDefinitionId, Map<String, Object> values) {
        if (formDefinitionId == null) {
            return Map.of();
        }

        List<FieldDefinitionDTO> fields = fieldDefinitionDao.findByFormDefinitionId(formDefinitionId);
        if (fields.isEmpty()) {
            return Map.of();
        }
        List<FieldDependencyDTO> dependencies =
                fieldDependencyDao.findByFormDefinitionId(formDefinitionId);

        EvaluationResult result = DependencyGraphEvaluator.evaluate(fields, dependencies, values);

        // A cycle is a form authoring mistake: the dependencies inside it are ignored so that
        // evaluation still terminates, but the form will not behave as its author intended.
        if (!result.cycles().isEmpty()) {
            log.warn("Form definition {} has {} dependency cycle(s), whose dependencies are ignored: {}",
                    formDefinitionId, result.cycles().size(), result.cycles());
        }

        return result.states();
    }
}
