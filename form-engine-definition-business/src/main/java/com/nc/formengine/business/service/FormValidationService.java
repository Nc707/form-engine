package com.nc.formengine.business.service;

import com.nc.formengine.model.validation.ValidationMode;
import com.nc.formengine.model.validation.ValidationReport;

import java.util.Map;

/**
 * Judges a set of answers against the restrictions the form declares.
 *
 * <p>Validation is driven entirely by data: a field's restrictions are stored with it, turned into
 * specifications at evaluation time, and applied to the answer given for that field. Adding a rule to
 * a form is therefore an edit to the form, not a change to this service.
 *
 * <p>Conditional dependencies are honoured through {@link DependencyEvaluationService}: a field the
 * answers have hidden is not validated at all, and is never demanded.
 *
 * @see com.nc.formengine.model.specification.FieldSpecificationFactory
 */
public interface FormValidationService {

    /**
     * Validates every field of a form against the answers given so far.
     *
     * <p>Answers are keyed by field {@code name}. A key with no matching field is ignored, and a
     * field with no answer is treated exactly as one answered null. Hidden fields are skipped
     * entirely, so a stale answer left behind by a field that has since been hidden cannot make the
     * form unsubmittable.
     *
     * <p>A field that is required and unanswered reports a single {@code NOT_NULL} error and none of
     * its other restrictions, since those would only restate that the answer is missing.
     *
     * @param formDefinitionId the form to validate against
     * @param values           the current answers, keyed by field name; may be null or empty
     * @param mode             {@link ValidationMode#DRAFT} to accept missing answers,
     *                         {@link ValidationMode#SUBMIT} to demand them; null means SUBMIT
     * @return the errors found, valid when there are none. A form that does not exist, or has no
     *         fields, is reported valid: there is nothing to break.
     */
    ValidationReport validate(Long formDefinitionId, Map<String, Object> values, ValidationMode mode);

    /**
     * Validates a single answer, as strictly as {@link ValidationMode#SUBMIT} would.
     *
     * <p>This is what a UI calls as the user leaves a field, so it takes the rest of the answers too:
     * they decide whether the field is currently visible, and they are what cross-field restrictions
     * read.
     *
     * @param fieldDefinitionId the field the answer belongs to
     * @param value             the answer to judge; may be null
     * @param formValues        the other current answers, keyed by field name; may be null, in which
     *                          case dependencies are not evaluated and the field is taken as visible
     * @return the errors found on that field, valid when there are none. An unknown field, or one the
     *         answers have hidden, is reported valid.
     */
    ValidationReport validateField(Long fieldDefinitionId, Object value, Map<String, Object> formValues);
}
