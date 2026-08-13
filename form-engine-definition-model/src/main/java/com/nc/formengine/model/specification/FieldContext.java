package com.nc.formengine.model.specification;

import com.nc.formengine.model.enums.FieldType;
import lombok.Builder;
import lombok.Data;

/**
 * What a specification may know about the answer it is judging, beyond the value itself.
 *
 * <p>Only what some specification actually reads belongs here. A restriction cannot name another
 * field, so there is nothing cross-field to carry: the rest of the form's answers decide whether a
 * field is <em>visible</em>, which is the dependency evaluator's job and happens before any
 * specification runs.
 */
@Data
@Builder
public class FieldContext {

    /**
     * The type of the field being validated. A specification that does not apply to this type
     * reports itself satisfied rather than rejecting the value.
     */
    private FieldType fieldType;

    /** The name of the field being validated, used to attribute the error. */
    private String fieldName;
}
