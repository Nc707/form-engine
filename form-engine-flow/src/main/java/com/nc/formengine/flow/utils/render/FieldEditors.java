package com.nc.formengine.flow.utils.render;

import com.nc.formengine.flow.components.field.FieldEditor;
import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasValue;

/**
 * The three things this view needs from an editor that {@link FieldEditor} does not expose: when its
 * value changed, when the user left it, and how to bring it back into view.
 *
 * <p>{@link FieldEditor} is deliberately narrow — value in, value out, errors, state — and every
 * widget {@link com.nc.formengine.flow.components.field.FieldComponentFactory} builds happens to be a
 * {@link HasValue} that is also focusable and reports blur. Doing the cast here keeps that assumption
 * in one place, and keeps it from failing loudly if some future field type is built out of something
 * else: an editor that cannot report a change simply never triggers one.
 */
public final class FieldEditors {

    private FieldEditors() {
    }

    /**
     * Runs {@code action} when the field's value changes.
     *
     * <p>This is the value-change event, not the input event: with Vaadin's default
     * {@link com.vaadin.flow.data.value.ValueChangeMode} a text field reports once the user is done
     * typing, so the dependency engine is asked once per answer rather than once per keystroke.
     */
    public static void onValueChange(FieldEditor editor, Runnable action) {
        if (editor.component() instanceof HasValue<?, ?> input) {
            input.addValueChangeListener(event -> action.run());
        }
    }

    /** Runs {@code action} when the user leaves the field. */
    public static void onBlur(FieldEditor editor, Runnable action) {
        if (editor.component() instanceof BlurNotifier<?> input) {
            input.addBlurListener(event -> action.run());
        }
    }

    /** Scrolls the field into view and focuses it, to send the user to the answer that needs fixing. */
    public static void focus(FieldEditor editor) {
        editor.component().scrollIntoView();
        if (editor.component() instanceof Focusable<?> focusable) {
            focusable.focus();
        }
    }
}
