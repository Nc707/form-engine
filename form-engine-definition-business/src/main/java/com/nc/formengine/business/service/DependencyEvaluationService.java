package com.nc.formengine.business.service;

import com.nc.formengine.model.dependency.FieldState;

import java.util.Map;

/**
 * Resolves the conditional dependencies of a form against the answers given so far.
 * <p>
 * A form declares dependencies of the shape "when the value of the trigger field satisfies this
 * condition, apply this effect to the dependent field". This service turns those declarations plus a
 * set of current answers into the concrete state of every field.
 *
 * @see com.nc.formengine.model.enums.DependencyCondition
 * @see com.nc.formengine.model.enums.DependencyEffect
 */
public interface DependencyEvaluationService {

    /**
     * Computes the current state of every field of a form.
     * <p>
     * Each field starts visible, and required as declared by its definition. Then the effects of all
     * dependencies whose condition holds against the current value of their trigger field are
     * applied, following these rules:
     * <ul>
     *   <li><b>SHOW gate</b> — a field targeted by at least one {@code SHOW} dependency starts hidden
     *       and is visible only while one of them is satisfied. Without this, {@code SHOW} could never
     *       change anything, since the base state is already visible.</li>
     *   <li><b>Conflicts: the most restrictive effect wins</b> — a satisfied {@code HIDE} beats a
     *       satisfied {@code SHOW}, and a satisfied {@code REQUIRE} beats a satisfied {@code OPTIONAL}.
     *       That rule is commutative and associative, so the outcome cannot depend on the order the
     *       dependencies were loaded in, and a misconfigured form fails closed.</li>
     *   <li><b>Chaining</b> — a hidden field is treated as having no value, so dependencies triggered
     *       by it are never satisfied. Chains of any depth (A controls B, B controls C) resolve in
     *       dependency order.</li>
     *   <li><b>Cycles</b> — dependencies whose trigger and dependent field belong to the same cycle
     *       are ignored, and the cycle is logged. Dependencies entering the cycle from outside, and
     *       fields downstream of it, keep working. Evaluation always terminates.</li>
     *   <li><b>Hidden implies not required</b> — a field the user cannot see is never reported as
     *       required, so callers can read {@link FieldState#required()} on its own.</li>
     * </ul>
     *
     * @param formDefinitionId the form to evaluate
     * @param values           the current answers, keyed by field name; may be empty but not null
     * @return the state of every field of the form, keyed by field {@code name} (not id). Fields
     *         with no dependencies are included with their base state. Empty if the form has no
     *         fields or does not exist.
     */
    Map<String, FieldState> evaluate(Long formDefinitionId, Map<String, Object> values);
}
