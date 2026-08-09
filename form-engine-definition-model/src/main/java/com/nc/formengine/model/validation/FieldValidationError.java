package com.nc.formengine.model.validation;

import com.nc.formengine.model.enums.RestrictionType;

/**
 * A single restriction of a single field that the given answer does not satisfy.
 *
 * <p>One broken rule is one error: a field breaking three restrictions produces three of these, so a
 * caller can report all of them at once, or pick by {@code restriction} which ones to surface.
 *
 * @param fieldName         name of the field the answer belongs to, the key used in the answers map
 * @param fieldDefinitionId id of the field definition, for callers addressing fields by id
 * @param restriction       the kind of restriction that was not satisfied
 * @param message           the message to show, custom if the form author wrote one, otherwise the
 *                          default of the specification that rejected the value
 */
public record FieldValidationError(String fieldName,
                                   Long fieldDefinitionId,
                                   RestrictionType restriction,
                                   String message) {
}
