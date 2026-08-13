package com.nc.formengine.model.validation;

/**
 * Why an answer was rejected.
 *
 * <p>Not every rejection comes from a restriction the form author configured. Two of them are the
 * engine's own, and reporting those as if some restriction had failed is how the old model came to
 * describe a missing required answer as a broken {@code NOT_NULL} rule that did not exist.
 */
public enum ValidationErrorCause {

    /** The field is required, and nothing was answered. */
    REQUIRED,

    /** The answer is not among the choices the field offers. Never configured; always enforced. */
    NOT_AN_OPTION,

    /** One of the restrictions stored with the field rejected the answer. */
    RESTRICTION
}
