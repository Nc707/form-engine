package com.nc.formengine.businessimpl.service.dependency;

import com.nc.formengine.model.dependency.FieldState;

import java.util.List;
import java.util.Map;

/**
 * The outcome of one evaluation: the state of every field, plus any dependency cycles found on the
 * way.
 * <p>
 * The public service contract returns only the states. Cycles are surfaced here so that the engine
 * can report them, and so that cycle detection is directly assertable in a unit test.
 *
 * @param states the state of every field of the form, keyed by field name
 * @param cycles one entry per cycle, each listing the names of the fields caught in it; empty when
 *               the dependency graph is acyclic
 */
public record EvaluationResult(Map<String, FieldState> states, List<List<String>> cycles) {
}
