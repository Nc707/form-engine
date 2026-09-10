package com.nc.formengine.flow.components.builder;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.rules.DefinitionRules;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A rule that makes one field depend on the answer to another.
 *
 * <p>The value being compared against is typed to the trigger field rather than left as free text,
 * and that is the point of this dialog. The engine compares the stored answer with this string, so a
 * value that could never be produced — "Yes" against a checkbox that stores {@code true}, a country
 * name against a select that stores country codes — makes a rule that is silently never satisfied.
 * Offering the same widget the renderer will offer makes that mistake impossible rather than merely
 * unlikely.
 */
public final class DependencyDialog extends Dialog {

    private final ComboBox<FieldDefinitionDTO> trigger = new ComboBox<>("When this field");
    private final ComboBox<DependencyCondition> condition = new ComboBox<>("Is");
    private final Div valueSlot = new Div();
    private final ComboBox<DependencyEffect> effect = new ComboBox<>("Then");
    private final ComboBox<FieldDefinitionDTO> dependent = new ComboBox<>("This field");
    private final Div problems = new Div();

    private final Long dependencyId;
    private final List<FieldDefinitionDTO> fields;
    private final Consumer<FieldDependencyDTO> onSave;

    /** Reads the trigger value out of whatever widget the trigger's type called for. */
    private Supplier<String> valueReader = () -> null;

    /**
     * @param existing the rule to edit, or null to add one
     * @param fields   the form's fields, all of them saved and so all of them with an id
     * @param onSave   receives the finished rule; only called once it validates
     */
    public DependencyDialog(FieldDependencyDTO existing,
                     List<FieldDefinitionDTO> fields,
                     Consumer<FieldDependencyDTO> onSave) {
        this.dependencyId = existing == null ? null : existing.getId();
        // A field without an id cannot be referenced. The editor never produces one, and filtering
        // here means a bug elsewhere cannot turn into an unsaveable rule.
        this.fields = fields.stream().filter(field -> field.getId() != null).toList();
        this.onSave = onSave;

        setHeaderTitle(existing == null ? "New conditional rule" : "Edit conditional rule");
        setCloseOnOutsideClick(false);
        setWidth("34em");

        configure(trigger);
        configure(dependent);
        trigger.setItems(this.fields);
        trigger.addValueChangeListener(event -> onTriggerChanged(event.getValue(), null));

        condition.setItemLabelGenerator(DependencyDialog::readableCondition);
        condition.setWidthFull();

        effect.setItems(DependencyEffect.values());
        effect.setItemLabelGenerator(DependencyDialog::readableEffect);
        effect.setWidthFull();

        problems.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
        problems.setVisible(false);

        var body = new VerticalLayout(problems, trigger, condition, valueSlot, effect, dependent);
        body.setPadding(false);
        body.setSpacing(false);
        add(body);

        var save = new Button("Save rule", event -> submit());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(new Button("Cancel", event -> close()), save);

        populate(existing);
    }

    private void configure(ComboBox<FieldDefinitionDTO> box) {
        box.setItemLabelGenerator(field ->
                field.getLabel() == null ? field.getName() : field.getLabel());
        box.setWidthFull();
    }

    private void populate(FieldDependencyDTO existing) {
        if (existing == null) {
            effect.setValue(DependencyEffect.SHOW);
            onTriggerChanged(null, null);
            return;
        }
        trigger.setValue(fieldById(existing.getTriggerFieldId()));
        onTriggerChanged(trigger.getValue(), existing.getTriggerValue());
        condition.setValue(existing.getCondition());
        effect.setValue(existing.getEffect());
        dependent.setValue(fieldById(existing.getDependentFieldId()));
    }

    /**
     * Re-offers everything that depends on which field is being watched.
     *
     * @param initialValue the stored trigger value to show, when a saved rule is being reopened
     */
    private void onTriggerChanged(FieldDefinitionDTO chosen, String initialValue) {
        FieldType type = chosen == null ? null : chosen.getType();

        List<DependencyCondition> available = DefinitionRules.conditionsFor(type);
        DependencyCondition current = condition.getValue();
        condition.setItems(available);
        if (current != null && available.contains(current)) {
            condition.setValue(current);
        } else if (available.contains(DependencyCondition.EQUALS)) {
            condition.setValue(DependencyCondition.EQUALS);
        }

        // A field cannot be its own trigger, so it is not offered as its own dependent.
        FieldDefinitionDTO currentDependent = dependent.getValue();
        dependent.setItems(fields.stream()
                .filter(field -> chosen == null || !Objects.equals(field.getId(), chosen.getId()))
                .toList());
        if (currentDependent != null && chosen != null
                && !Objects.equals(currentDependent.getId(), chosen.getId())) {
            dependent.setValue(currentDependent);
        }

        buildValueInput(chosen, initialValue);
    }

    /** Puts the same widget the renderer would offer for this field into the value slot. */
    private void buildValueInput(FieldDefinitionDTO chosen, String initialValue) {
        valueSlot.removeAll();
        valueReader = () -> null;

        if (chosen == null || chosen.getType() == null) {
            return;
        }

        switch (chosen.getType()) {
            case BOOLEAN -> {
                var input = labelled(new Select<String>());
                input.setItems("true", "false");
                input.setItemLabelGenerator(value -> "true".equals(value) ? "Ticked" : "Not ticked");
                input.setValue(initialValue == null ? "true" : initialValue);
                valueReader = input::getValue;
                valueSlot.add(input);
            }
            case SELECT, MULTI_SELECT -> {
                var input = labelled(new Select<String>());
                List<FieldOptionDTO> options = chosen.getOptions() == null
                        ? List.of() : chosen.getOptions();
                input.setItems(options.stream().map(FieldOptionDTO::getValue).toList());
                input.setItemLabelGenerator(value -> options.stream()
                        .filter(option -> Objects.equals(option.getValue(), value))
                        .map(FieldOptionDTO::getLabel)
                        .findFirst()
                        .orElse(value));
                if (initialValue != null) {
                    input.setValue(initialValue);
                }
                valueReader = input::getValue;
                valueSlot.add(input);
            }
            case NUMBER -> {
                var input = new NumberField("Has the value");
                input.setWidthFull();
                if (initialValue != null) {
                    try {
                        input.setValue(Double.valueOf(initialValue));
                    } catch (NumberFormatException ignored) {
                        // A stored value that is not a number is shown as empty rather than crashing.
                    }
                }
                // The same encoding the renderer stores, so 3 compares as "3" and not "3.0".
                valueReader = () -> input.getValue() == null ? null
                        : BigDecimal.valueOf(input.getValue()).stripTrailingZeros().toPlainString();
                valueSlot.add(input);
            }
            case DATE -> {
                var input = new DatePicker("Has the value");
                input.setWidthFull();
                if (initialValue != null) {
                    try {
                        input.setValue(LocalDate.parse(initialValue));
                    } catch (RuntimeException ignored) {
                        // Same again: an unparseable stored date starts empty.
                    }
                }
                valueReader = () -> input.getValue() == null ? null : input.getValue().toString();
                valueSlot.add(input);
            }
            case TEXT -> {
                var input = new TextField("Has the value");
                input.setWidthFull();
                if (initialValue != null) {
                    input.setValue(initialValue);
                }
                valueReader = input::getValue;
                valueSlot.add(input);
            }
        }
    }

    private Select<String> labelled(Select<String> input) {
        input.setLabel("Has the value");
        input.setWidthFull();
        return input;
    }

    private void submit() {
        var candidate = FieldDependencyDTO.builder()
                .id(dependencyId)
                .triggerFieldId(trigger.getValue() == null ? null : trigger.getValue().getId())
                .dependentFieldId(dependent.getValue() == null ? null : dependent.getValue().getId())
                .condition(condition.getValue())
                .effect(effect.getValue())
                .triggerValue(valueReader.get())
                .build();

        List<String> found = DefinitionRules.checkDependency(candidate, fields);
        if (!found.isEmpty()) {
            problems.removeAll();
            found.forEach(problem -> problems.add(new Div(new Span(problem))));
            problems.setVisible(true);
            return;
        }

        close();
        onSave.accept(candidate);
    }

    private FieldDefinitionDTO fieldById(Long id) {
        return fields.stream()
                .filter(field -> Objects.equals(field.getId(), id))
                .findFirst()
                .orElse(null);
    }

    private static String readableCondition(DependencyCondition condition) {
        return switch (condition) {
            case EQUALS -> "equal to";
            case NOT_EQUALS -> "not equal to";
            case GREATER_THAN -> "greater than";
            case LESS_THAN -> "less than";
            case CONTAINS -> "containing";
        };
    }

    private static String readableEffect(DependencyEffect effect) {
        return switch (effect) {
            case SHOW -> "show";
            case HIDE -> "hide";
            case REQUIRE -> "require";
            case OPTIONAL -> "make optional";
        };
    }
}
