package com.nc.formengine.ui.builder;

import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;

/**
 * One validation rule on a field: which rule, what it is configured with, and what it says when it
 * fails.
 *
 * <p>The parameter input is rebuilt whenever the rule changes, because each rule wants a different
 * one — a whole number for the length rules, a decimal for the value rules, an expression for the
 * pattern, and nothing at all for the three that take no configuration. What it is stored under
 * comes from {@link RestrictionParameterSpec} rather than from anything typed here.
 */
final class RestrictionRow extends HorizontalLayout {

    private final ComboBox<RestrictionType> type = new ComboBox<>();
    private final Div parameterSlot = new Div();
    private final TextField errorMessage = new TextField();

    private Long id;
    private AbstractField<?, ?> parameterInput;

    RestrictionRow(FieldType fieldType, Runnable onRemove) {
        type.setPlaceholder("Rule");
        type.setWidth("14em");
        type.addValueChangeListener(event -> rebuildParameter(null));

        parameterSlot.setWidth("12em");

        errorMessage.setPlaceholder("Custom message (optional)");
        errorMessage.setWidthFull();

        var remove = new Button(VaadinIcon.CLOSE_SMALL.create(), event -> onRemove.run());
        remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_ERROR);

        setWidthFull();
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        add(type, parameterSlot, errorMessage, remove);
        setFlexGrow(1, errorMessage);

        setFieldType(fieldType);
    }

    /**
     * Re-offers the rules that suit the field's type.
     *
     * <p>A rule already chosen that the new type cannot use is dropped here. The dialog asks before
     * it comes to that, so by the time this runs the answer has been given.
     */
    void setFieldType(FieldType fieldType) {
        List<RestrictionType> applicable = RestrictionParameterSpec.applicableTo(fieldType);
        RestrictionType current = type.getValue();
        type.setItems(applicable);
        type.setItemLabelGenerator(RestrictionType::name);
        if (current != null && applicable.contains(current)) {
            type.setValue(current);
        } else if (current != null) {
            type.clear();
        }
    }

    /** Shows a stored rule. */
    void setRestriction(FieldRestrictionDTO restriction) {
        id = restriction.getId();
        type.setValue(restriction.getRestrictionType());
        rebuildParameter(RestrictionParameterSpec.read(
                restriction.getRestrictionType(), restriction.getParameters()));
        errorMessage.setValue(restriction.getErrorMessage() == null
                ? "" : restriction.getErrorMessage());
    }

    RestrictionType restrictionType() {
        return type.getValue();
    }

    /**
     * The rule as it now stands.
     *
     * @param orderIndex where it sits, which decides which failure is reported first
     */
    FieldRestrictionDTO toDto(int orderIndex) {
        RestrictionType selected = type.getValue();
        return FieldRestrictionDTO.builder()
                .id(id)
                .restrictionType(selected)
                .parameters(RestrictionParameterSpec.parameters(selected, parameterValue()))
                .errorMessage(errorMessage.getValue() == null || errorMessage.getValue().isBlank()
                        ? null : errorMessage.getValue().trim())
                .orderIndex(orderIndex)
                .build();
    }

    private Object parameterValue() {
        return parameterInput == null ? null : parameterInput.getValue();
    }

    /**
     * Puts the right input in the parameter slot for the chosen rule.
     *
     * @param value the value to show, or null for a rule being configured from scratch
     */
    private void rebuildParameter(Object value) {
        parameterSlot.removeAll();
        parameterInput = null;

        RestrictionParameterSpec spec = RestrictionParameterSpec.of(type.getValue());
        if (spec == null || !spec.hasParameter()) {
            return;
        }

        parameterInput = switch (spec.kind()) {
            case INTEGER -> {
                var input = new IntegerField();
                input.setMin(0);
                input.setStepButtonsVisible(true);
                if (value instanceof Integer stored) {
                    input.setValue(stored);
                }
                yield input;
            }
            case DECIMAL -> {
                var input = new NumberField();
                if (value instanceof Double stored) {
                    input.setValue(stored);
                }
                yield input;
            }
            case REGEX -> {
                var input = new TextField();
                if (value != null) {
                    input.setValue(value.toString());
                }
                yield input;
            }
            case NONE -> null;
        };

        if (parameterInput == null) {
            return;
        }
        parameterInput.getElement().setProperty("placeholder", spec.label());
        // The consequence, not the rule: a length rule with no length accepts every answer, and the
        // engine will not complain about it.
        parameterInput.getElement().setAttribute("title", spec.helper());
        parameterInput.getElement().setProperty("required", true);
        parameterSlot.add(parameterInput);
    }
}
