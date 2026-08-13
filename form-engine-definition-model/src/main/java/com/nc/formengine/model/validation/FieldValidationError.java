package com.nc.formengine.model.validation;

import com.nc.formengine.model.enums.RestrictionType;

/**
 * One thing wrong with one answer.
 *
 * <p>A field breaking three restrictions produces three of these, so a caller can report all of them
 * at once, or pick by {@code cause} which ones to surface.
 *
 * @param fieldName         name of the field the answer belongs to, the key used in the answers map
 * @param fieldDefinitionId id of the field definition, for callers addressing fields by id
 * @param cause             what rejected the answer
 * @param restriction       which restriction rejected it, non-null exactly when {@code cause} is
 *                          {@link ValidationErrorCause#RESTRICTION}
 * @param message           the message to show, custom if the form author wrote one, otherwise the
 *                          default of whatever rejected the value
 */
public record FieldValidationError(String fieldName,
                                   Long fieldDefinitionId,
                                   ValidationErrorCause cause,
                                   RestrictionType restriction,
                                   String message) {

    public FieldValidationError {
        if (cause == ValidationErrorCause.RESTRICTION && restriction == null) {
            throw new IllegalArgumentException("A RESTRICTION error must say which restriction failed");
        }
        if (cause != ValidationErrorCause.RESTRICTION && restriction != null) {
            throw new IllegalArgumentException(
                "A " + cause + " error does not come from a restriction, so it cannot name one");
        }
    }

    /** The field is required and was left unanswered. */
    public static FieldValidationError missingRequired(String fieldName, Long fieldDefinitionId,
                                                       String message) {
        return new FieldValidationError(fieldName, fieldDefinitionId,
            ValidationErrorCause.REQUIRED, null, message);
    }

    /** The answer is not one of the choices the field offers. */
    public static FieldValidationError notAnOption(String fieldName, Long fieldDefinitionId,
                                                   String message) {
        return new FieldValidationError(fieldName, fieldDefinitionId,
            ValidationErrorCause.NOT_AN_OPTION, null, message);
    }

    /** A restriction stored with the field rejected the answer. */
    public static FieldValidationError brokenRestriction(String fieldName, Long fieldDefinitionId,
                                                         RestrictionType restriction, String message) {
        return new FieldValidationError(fieldName, fieldDefinitionId,
            ValidationErrorCause.RESTRICTION, restriction, message);
    }
}
