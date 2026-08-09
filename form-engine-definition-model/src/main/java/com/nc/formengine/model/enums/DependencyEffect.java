package com.nc.formengine.model.enums;

/**
 * What a dependency does to its dependent field once its {@link DependencyCondition} holds.
 * <p>
 * When two satisfied dependencies disagree on the same field the most restrictive effect wins:
 * {@link #HIDE} beats {@link #SHOW} and {@link #REQUIRE} beats {@link #OPTIONAL}. That rule is
 * commutative and associative, so the outcome never depends on the order the dependencies happen to
 * be loaded in, and a misconfigured form fails closed.
 */
public enum DependencyEffect {

    /**
     * Reveals the dependent field. A field targeted by at least one {@code SHOW} dependency starts
     * hidden and becomes visible only while one of them is satisfied; fields with no {@code SHOW}
     * dependency are visible by default. Without that gate {@code SHOW} could never change anything,
     * since the base state of every field is already visible.
     */
    SHOW,

    /** Hides the dependent field. Beats a satisfied {@link #SHOW} on the same field. */
    HIDE,

    /** Makes the dependent field mandatory. Beats a satisfied {@link #OPTIONAL} on the same field. */
    REQUIRE,

    /** Makes the dependent field optional, overriding the {@code required} flag of its definition. */
    OPTIONAL
}
