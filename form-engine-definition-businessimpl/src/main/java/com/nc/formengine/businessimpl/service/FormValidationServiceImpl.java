package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.DependencyEvaluationService;
import com.nc.formengine.business.service.FormValidationService;
import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.FieldSpecificationFactory;
import com.nc.formengine.model.specification.SpecificationResult;
import com.nc.formengine.model.validation.AnswerCodec;
import com.nc.formengine.model.validation.Answers;
import com.nc.formengine.model.validation.FieldValidationError;
import com.nc.formengine.model.validation.ValidationMode;
import com.nc.formengine.model.validation.ValidationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** Used when the form author did not word the field's own required message. */
    private static final String DEFAULT_REQUIRED_MESSAGE = "This field is required";

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
            errors.addAll(validateOne(field, answers.get(field.getName()), state, mode));
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

        return ValidationReport.of(validateOne(field, value, state, ValidationMode.SUBMIT));
    }

    /**
     * Collects everything wrong with one answer.
     *
     * <p>A missing answer to a required field short-circuits the rest: the other restrictions of the
     * field would all be reporting, in their own words, that there is nothing there.
     *
     * <p>Presence is asked about exactly once, here, and only the mode decides whether to demand it.
     * No restriction can demand it, so a draft needs no rule to be skipped for it.
     */
    private List<FieldValidationError> validateOne(FieldDefinitionDTO field, Object value,
                                                   FieldState state, ValidationMode mode) {
        boolean missing = Answers.isMissing(value);

        if (mode != ValidationMode.DRAFT && state.required() && missing) {
            return List.of(FieldValidationError.missingRequired(
                field.getName(), field.getId(), requiredMessage(field)));
        }

        if (missing) {
            // Nothing to judge. An optional field left blank is not a rule violation.
            return List.of();
        }

        List<FieldValidationError> notOptions = validateOptionMembership(field, value);
        if (!notOptions.isEmpty()) {
            return notOptions;
        }

        FieldContext context = FieldContext.builder()
            .fieldType(field.getType())
            .fieldName(field.getName())
            .build();

        List<FieldValidationError> errors = new ArrayList<>();
        for (FieldRestrictionDTO restriction : restrictionsOf(field)) {
            SpecificationResult result = FieldSpecificationFactory.from(restriction)
                .isSatisfiedBy(value, context);
            if (result.isSatisfied()) {
                continue;
            }

            for (String reason : result.getReasons()) {
                errors.add(FieldValidationError.brokenRestriction(
                    field.getName(), field.getId(), restriction.getRestrictionType(), reason));
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
     * Refuses a choice the field does not offer.
     *
     * <p>Not a restriction anyone configures: an answer outside a field's own options is never valid,
     * so there would be nothing to turn off. Until this existed the only thing keeping a SELECT honest
     * was the widget, and any string at all could be stored through the API.
     *
     * <p>An option removed in a later version is not judged here — old answers are read against the
     * definition they were filled under, and validation only ever runs against the current one.
     */
    private List<FieldValidationError> validateOptionMembership(FieldDefinitionDTO field, Object value) {
        if (field.getType() != FieldType.SELECT && field.getType() != FieldType.MULTI_SELECT) {
            return List.of();
        }

        Set<String> offered = field.getOptions() == null ? Set.of() : field.getOptions().stream()
            .filter(Objects::nonNull)
            .map(FieldOptionDTO::getValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return AnswerCodec.decodeSelections(value).stream()
            .filter(chosen -> !offered.contains(chosen))
            .map(chosen -> FieldValidationError.notAnOption(field.getName(), field.getId(),
                "'" + chosen + "' is not one of the choices for this field"))
            .toList();
    }

    /** The message for a missing required answer: the field author's own, or the engine's default. */
    private String requiredMessage(FieldDefinitionDTO field) {
        String authored = field.getRequiredMessage();
        return authored != null && !authored.isBlank() ? authored : DEFAULT_REQUIRED_MESSAGE;
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
