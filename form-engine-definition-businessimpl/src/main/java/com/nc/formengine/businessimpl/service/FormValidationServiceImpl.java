package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.DependencyEvaluationService;
import com.nc.formengine.business.service.FormValidationService;
import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.FieldSpecificationFactory;
import com.nc.formengine.model.specification.SpecificationResult;
import com.nc.formengine.model.specification.impl.NotNullSpecification;
import com.nc.formengine.model.validation.FieldValidationError;
import com.nc.formengine.model.validation.ValidationMode;
import com.nc.formengine.model.validation.ValidationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Applies the restrictions stored with a form to the answers given for it.
 *
 * <p>The rules themselves live in the specifications; this class decides <em>which</em> ones to run
 * and turns what they reject into errors. Each restriction is evaluated on its own rather than
 * through {@link FieldSpecificationFactory#composite(List)}, because combining them stops at the
 * first failure and loses which restriction it was — and a user fixing a form is better served by
 * every problem at once than by one per round trip.
 */
@Service
@RequiredArgsConstructor
public class FormValidationServiceImpl implements FormValidationService {

    /** Ties in {@code orderIndex} keep the order the restrictions were loaded in. */
    private static final Comparator<FieldRestrictionDTO> BY_ORDER =
        Comparator.comparing(FieldRestrictionDTO::getOrderIndex,
            Comparator.nullsLast(Comparator.naturalOrder()));

    private final FieldDefinitionDao fieldDefinitionDao;
    private final DependencyEvaluationService dependencyEvaluationService;

    @Override
    @Transactional(readOnly = true)
    public ValidationReport validate(Long formDefinitionId, Map<String, Object> values, ValidationMode mode) {
        if (formDefinitionId == null) {
            return ValidationReport.noErrors();
        }

        List<FieldDefinitionDTO> fields = fieldDefinitionDao.findByFormDefinitionId(formDefinitionId);
        if (fields.isEmpty()) {
            return ValidationReport.noErrors();
        }

        Map<String, Object> answers = values != null ? values : Map.of();
        Map<String, FieldState> states = dependencyEvaluationService.evaluate(formDefinitionId, answers);

        List<FieldValidationError> errors = new ArrayList<>();
        for (FieldDefinitionDTO field : fields) {
            FieldState state = states.getOrDefault(field.getName(), baseStateOf(field));
            if (!state.visible()) {
                continue;
            }
            errors.addAll(validateOne(field, answers.get(field.getName()), answers, state, mode));
        }

        return ValidationReport.of(errors);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationReport validateField(Long fieldDefinitionId, Object value, Map<String, Object> formValues) {
        if (fieldDefinitionId == null) {
            return ValidationReport.noErrors();
        }

        Optional<FieldDefinitionDTO> found = fieldDefinitionDao.findById(fieldDefinitionId);
        if (found.isEmpty()) {
            return ValidationReport.noErrors();
        }
        FieldDefinitionDTO field = found.get();

        FieldState state = stateOf(field, formValues);
        if (!state.visible()) {
            return ValidationReport.noErrors();
        }

        Map<String, Object> answers = formValues != null ? formValues : Map.of();
        return ValidationReport.of(validateOne(field, value, answers, state, ValidationMode.SUBMIT));
    }

    /**
     * Collects everything wrong with one answer.
     *
     * <p>A missing answer to a required field short-circuits the rest: the other restrictions of the
     * field would all be reporting, in their own words, that there is nothing there.
     */
    private List<FieldValidationError> validateOne(FieldDefinitionDTO field, Object value,
                                                   Map<String, Object> formValues,
                                                   FieldState state, ValidationMode mode) {
        boolean demandPresence = mode != ValidationMode.DRAFT;

        if (demandPresence && state.required() && isMissing(value)) {
            return List.of(new FieldValidationError(field.getName(), field.getId(),
                RestrictionType.NOT_NULL, requiredMessage(field)));
        }

        FieldContext context = FieldContext.builder()
            .fieldType(field.getType())
            .fieldName(field.getName())
            .formData(formValues)
            .build();

        List<FieldValidationError> errors = new ArrayList<>();
        for (FieldRestrictionDTO restriction : restrictionsOf(field)) {
            // While the form is a draft, demanding a value is exactly what is being suspended.
            if (!demandPresence && restriction.getRestrictionType() == RestrictionType.NOT_NULL) {
                continue;
            }

            SpecificationResult result = FieldSpecificationFactory.from(restriction)
                .isSatisfiedBy(value, context);
            if (result.isSatisfied()) {
                continue;
            }

            for (String reason : result.getReasons()) {
                errors.add(new FieldValidationError(field.getName(), field.getId(),
                    restriction.getRestrictionType(), reason));
            }
        }

        return errors;
    }

    /**
     * The state of a single field, when only that field is being validated.
     *
     * <p>Without the other answers there is nothing to evaluate the dependencies against, so the
     * field is taken at face value: visible, and required as its definition declares.
     */
    private FieldState stateOf(FieldDefinitionDTO field, Map<String, Object> formValues) {
        if (formValues == null || field.getFormDefinitionId() == null) {
            return baseStateOf(field);
        }

        return dependencyEvaluationService.evaluate(field.getFormDefinitionId(), formValues)
            .getOrDefault(field.getName(), baseStateOf(field));
    }

    private FieldState baseStateOf(FieldDefinitionDTO field) {
        return new FieldState(true, Boolean.TRUE.equals(field.getRequired()));
    }

    /**
     * An answer counts as missing when there is nothing in it a restriction could judge: no value,
     * only whitespace, or an empty selection.
     */
    private boolean isMissing(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    /**
     * The message for a missing required answer: the one the author wrote on the field's own
     * {@code NOT_NULL} restriction if there is one, otherwise the default of the specification that
     * would have rejected it.
     */
    private String requiredMessage(FieldDefinitionDTO field) {
        return restrictionsOf(field).stream()
            .filter(restriction -> restriction.getRestrictionType() == RestrictionType.NOT_NULL)
            .map(FieldRestrictionDTO::getErrorMessage)
            .filter(message -> message != null && !message.isBlank())
            .findFirst()
            .orElseGet(() -> new NotNullSpecification().getErrorMessage());
    }

    private List<FieldRestrictionDTO> restrictionsOf(FieldDefinitionDTO field) {
        if (field.getRestrictions() == null) {
            return List.of();
        }

        return field.getRestrictions().stream()
            .filter(Objects::nonNull)
            .filter(restriction -> restriction.getRestrictionType() != null)
            .sorted(BY_ORDER)
            .toList();
    }
}
