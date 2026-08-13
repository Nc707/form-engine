package com.nc.formengine.model.enums;

/**
 * The kinds of rule a field can carry.
 *
 * <p>Every one of these judges an answer that <em>is</em> there. Whether an answer has to be there at
 * all is not a restriction: it is the field's own {@code required} flag, which dependencies can also
 * turn on and off per submission. Saying it twice would mean two answers to one question — and they
 * would not agree, because a rule cannot know that the form is only being saved as a draft.
 */
public enum RestrictionType {

    /**
     * Validates minimum string length.
     * Applicable to: TEXT
     */
    MIN_LENGTH,

    /**
     * Validates maximum string length.
     * Applicable to: TEXT
     */
    MAX_LENGTH,

    /**
     * Validates minimum numeric value.
     * Applicable to: NUMBER
     */
    MIN_VALUE,

    /**
     * Validates maximum numeric value.
     * Applicable to: NUMBER
     */
    MAX_VALUE,

    /**
     * Validates string matches a regex pattern.
     * Applicable to: TEXT
     */
    PATTERN,

    /**
     * Validates email format.
     * Applicable to: TEXT
     */
    EMAIL
}
