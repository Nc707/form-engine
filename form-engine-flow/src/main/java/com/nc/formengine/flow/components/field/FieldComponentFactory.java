package com.nc.formengine.flow.components.field;

import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.validation.AnswerCodec;
import com.nc.formengine.model.validation.FieldValidationError;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the input for a field definition.
 *
 * <p>This is the single place that decides what a {@link FieldType} looks like on screen, so a form
 * looks the same whether it is being filled in or previewed in the builder.
 *
 * <p>Every editor reads and writes {@link String}, whatever widget is behind it, because that is
 * what a submission stores and what the validator reads. Widget-specific parsing lives here; the part
 * the engine also has to read — how a boolean and a multi-select are written down — comes from
 * {@link AnswerCodec}, so this class cannot drift away from what validation expects.
 */
public final class FieldComponentFactory {

    private FieldComponentFactory() {
    }

    public static FieldEditor create(FieldDefinitionDTO field) {
        FieldEditor editor = switch (field.getType()) {
            case TEXT -> textEditor(field);
            case NUMBER -> numberEditor(field);
            case DATE -> dateEditor(field);
            case BOOLEAN -> booleanEditor(field);
            case SELECT -> selectEditor(field);
            case MULTI_SELECT -> multiSelectEditor(field);
        };
        editor.component().getElement().setAttribute("data-field-name", String.valueOf(field.getName()));
        return editor;
    }

    private static FieldEditor textEditor(FieldDefinitionDTO field) {
        var input = new TextField(field.getLabel());
        input.setWidthFull();
        input.setClearButtonVisible(true);
        return new BaseEditor<>(field, input, value -> value, value -> value);
    }

    private static FieldEditor numberEditor(FieldDefinitionDTO field) {
        var input = new NumberField(field.getLabel());
        input.setWidthFull();
        return new BaseEditor<>(field, input,
                // BigDecimal rather than Double.toString: 3 should read as "3", not "3.0".
                number -> BigDecimal.valueOf(number).stripTrailingZeros().toPlainString(),
                text -> {
                    try {
                        return Double.valueOf(text);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                });
    }

    private static FieldEditor dateEditor(FieldDefinitionDTO field) {
        var input = new DatePicker(field.getLabel());
        input.setWidthFull();
        return new BaseEditor<>(field, input,
                LocalDate::toString,
                text -> {
                    try {
                        return LocalDate.parse(text);
                    } catch (DateTimeParseException ex) {
                        return null;
                    }
                });
    }

    private static FieldEditor booleanEditor(FieldDefinitionDTO field) {
        var input = new Checkbox(field.getLabel());
        return new BaseEditor<>(field, input, AnswerCodec::encodeBoolean, Boolean::valueOf);
    }

    private static FieldEditor selectEditor(FieldDefinitionDTO field) {
        var input = new Select<String>();
        input.setLabel(field.getLabel());
        input.setWidthFull();
        input.setEmptySelectionAllowed(true);
        input.setItems(optionValues(field));
        input.setItemLabelGenerator(value -> labelFor(field, value));
        return new BaseEditor<>(field, input, value -> value, value -> value);
    }

    private static FieldEditor multiSelectEditor(FieldDefinitionDTO field) {
        var input = new MultiSelectComboBox<String>(field.getLabel());
        input.setWidthFull();
        input.setItems(optionValues(field));
        input.setItemLabelGenerator(value -> labelFor(field, value));
        return new BaseEditor<Set<String>>(field, input,
                AnswerCodec::encodeSelections,
                text -> new LinkedHashSet<>(AnswerCodec.decodeSelections(text)));
    }

    private static List<String> optionValues(FieldDefinitionDTO field) {
        return field.getOptions() == null
                ? List.of()
                : field.getOptions().stream()
                        .map(FieldOptionDTO::getValue)
                        .filter(java.util.Objects::nonNull)
                        .toList();
    }

    /** Options carry a label for people and a value for the engine; the widget shows the label. */
    private static String labelFor(FieldDefinitionDTO field, String value) {
        // A Select that allows an empty selection asks its generator to label that item too, and the
        // value it passes for it is null.
        if (value == null) {
            return "";
        }
        if (field.getOptions() == null) {
            return value;
        }
        return field.getOptions().stream()
                .filter(option -> value.equals(option.getValue()))
                .map(FieldOptionDTO::getLabel)
                .findFirst()
                .orElse(value);
    }

    /**
     * The behaviour every editor shares, over whatever type its widget happens to hold.
     *
     * @param <T> the widget's own value type
     */
    private static final class BaseEditor<T> implements FieldEditor {

        private interface ToText<T> {
            String apply(T value);
        }

        private interface FromText<T> {
            T apply(String text);
        }

        private final FieldDefinitionDTO field;
        private final Component component;
        private final HasValue<?, T> input;
        private final ToText<T> toText;
        private final FromText<T> fromText;

        private <C extends Component & HasValue<?, T>> BaseEditor(
                FieldDefinitionDTO field, C input, ToText<T> toText, FromText<T> fromText) {
            this.field = field;
            this.component = input;
            this.input = input;
            this.toText = toText;
            this.fromText = fromText;
            input.setRequiredIndicatorVisible(Boolean.TRUE.equals(field.getRequired()));
        }

        @Override
        public FieldDefinitionDTO field() {
            return field;
        }

        @Override
        public Component component() {
            return component;
        }

        @Override
        public String value() {
            T raw = input.getValue();
            if (raw == null) {
                return null;
            }
            String text = toText.apply(raw);
            // "Not answered" gets one representation, so a required check does not depend on which
            // widget produced the answer.
            return text == null || text.isEmpty() ? null : text;
        }

        @Override
        public void setValue(String value) {
            if (value == null || value.isEmpty()) {
                input.clear();
                return;
            }
            T converted = fromText.apply(value);
            if (converted == null) {
                input.clear();
            } else {
                input.setValue(converted);
            }
        }

        @Override
        public void setErrors(List<FieldValidationError> errors) {
            if (!(component instanceof HasValidation validation)) {
                return;
            }
            if (errors == null || errors.isEmpty()) {
                validation.setInvalid(false);
                validation.setErrorMessage(null);
                return;
            }
            // Only the first: the rest would not fit and would restate the same problem.
            validation.setErrorMessage(errors.get(0).message());
            validation.setInvalid(true);
        }

        @Override
        public void applyState(FieldState state) {
            component.setVisible(state.visible());
            input.setRequiredIndicatorVisible(state.required());
            if (!state.visible()) {
                // A value typed before a dependency hid the field must not be submitted behind the
                // user's back.
                input.clear();
                setErrors(List.of());
            }
        }

        @Override
        public void setReadOnly(boolean readOnly) {
            input.setReadOnly(readOnly);
        }
    }
}
