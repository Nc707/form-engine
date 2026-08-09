package com.nc.formengine.model.validation;

/**
 * How strictly a set of answers is judged.
 */
public enum ValidationMode {

    /**
     * The user is still filling the form in. Missing answers are expected, so required fields are
     * not demanded; whatever has been answered is still checked against its restrictions.
     */
    DRAFT,

    /**
     * The user is submitting the form. Every visible required field must have an answer.
     */
    SUBMIT
}
