package com.nc.formengine.model.enums;

/**
 * The comparison applied to the current value of a dependency's trigger field.
 * <p>
 * When the comparison holds, the dependency's {@link DependencyEffect} is applied to its dependent
 * field. A condition never holds when the trigger field has no value: absence neutralises a trigger
 * instead of flipping half of its rules. In particular {@code NOT_EQUALS} is the negation of
 * {@code EQUALS} over present values only.
 */
public enum DependencyCondition {

    /** Equal values. Numeric when both sides parse as numbers, otherwise an exact string match. */
    EQUALS,

    /** The negation of {@link #EQUALS}, evaluated only when the trigger field has a value. */
    NOT_EQUALS,

    /** Numeric when both sides parse as numbers, otherwise a lexicographic string comparison. */
    GREATER_THAN,

    /** Numeric when both sides parse as numbers, otherwise a lexicographic string comparison. */
    LESS_THAN,

    /**
     * Membership for multi-valued answers (a collection or array contains the expected value),
     * substring containment for anything else.
     */
    CONTAINS
}
