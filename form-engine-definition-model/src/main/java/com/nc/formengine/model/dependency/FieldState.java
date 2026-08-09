package com.nc.formengine.model.dependency;

/**
 * The runtime state of a single field, once every conditional dependency of its form has been
 * resolved against the answers given so far.
 *
 * @param visible  whether the field should be presented to the user right now
 * @param required whether an answer is mandatory right now, which may differ from the {@code required}
 *                 flag of the field definition
 */
public record FieldState(boolean visible, boolean required) {

    /**
     * A hidden field is never required: the user has no way to answer it, so demanding a value would
     * make the form impossible to submit. Normalising here means callers can read {@link #required()}
     * on its own instead of remembering to check {@link #visible()} first.
     */
    public FieldState {
        required = visible && required;
    }
}
