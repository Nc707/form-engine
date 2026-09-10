package com.nc.formengine.flow.components.field;

import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.validation.FieldValidationError;
import com.vaadin.flow.component.Component;

import java.util.List;

/**
 * One field of a rendered form: the input the user types into, plus the small amount of behaviour
 * the engine needs to drive it.
 *
 * <p>Every implementation speaks {@link String} regardless of the widget behind it, because that is
 * what a submission stores and what the validator reads. Keeping the conversion here means no view
 * has to know that a checkbox is {@code "true"} or that a multi-select is comma-joined.
 */
public interface FieldEditor {

    /** The definition this editor was built from. */
    FieldDefinitionDTO field();

    /** The component to place in a layout. */
    Component component();

    /** The field's {@code name}, which is the key the engine uses for answers. */
    default String name() {
        return field().getName();
    }

    /**
     * The current answer in its canonical string form, or null when the user has left it empty.
     * Never an empty string: "not answered" has one representation, so a required check does not
     * depend on which widget produced it.
     */
    String value();

    /** Puts an answer back into the widget. A null clears it. */
    void setValue(String value);

    /**
     * Shows the errors found for this field, or clears the error state when the list is empty.
     * Only the first message is displayed; the rest would not fit and say the same thing.
     */
    void setErrors(List<FieldValidationError> errors);

    /**
     * Applies what the dependency engine decided: whether the field is shown at all, and whether it
     * is currently mandatory.
     *
     * <p>A field being hidden also clears its answer, so a value typed before a dependency hid the
     * field is not submitted behind the user's back.
     */
    void applyState(FieldState state);

    /** Enables or disables input without hiding the field, for read-only renderings. */
    void setReadOnly(boolean readOnly);
}
