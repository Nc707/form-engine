package com.nc.formengine.ui.builder;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * One field, with everything attached to it.
 *
 * <p>A field, its validation rules and its options are saved together in a single call, because that
 * is the unit the persistence layer actually reconciles: {@code FieldDefinitionService.update} takes
 * all three, matches the rules and options it already has by id, and deletes whatever the DTO no
 * longer mentions. Splitting them across separate saves would mean re-implementing that here.
 *
 * <p>Everything is edited on a copy, so Cancel really does cancel — including the cascade that
 * follows changing a field's type, which can drop rules and options that the new type cannot use.
 */
final class FieldDialog extends Dialog {

    private final TextField name = new TextField("Name");
    private final TextField label = new TextField("Label");
    private final ComboBox<FieldType> type = new ComboBox<>("Type");
    private final Checkbox required = new Checkbox("Required");
    private final TextField requiredMessage = new TextField("Message when left blank");

    private final RestrictionListEditor restrictions = new RestrictionListEditor();
    private final OptionListEditor options = new OptionListEditor();
    private final VerticalLayout optionsSection = new VerticalLayout();
    private final Div problems = new Div();

    private final Long fieldId;
    private final List<FieldDefinitionDTO> siblings;
    private final Consumer<FieldDefinitionDTO> onSave;

    /** Guards the type listener while it puts back a change that was just declined. */
    private boolean reverting;
    private FieldType lastType;

    /**
     * @param field    the field to edit, or null to add one
     * @param siblings the form's other fields, for the duplicate-name check
     * @param onSave   receives the finished field; only called once it validates
     */
    FieldDialog(FieldDefinitionDTO field,
                List<FieldDefinitionDTO> siblings,
                Consumer<FieldDefinitionDTO> onSave) {
        this.fieldId = field == null ? null : field.getId();
        this.siblings = siblings;
        this.onSave = onSave;

        setHeaderTitle(field == null ? "New field" : "Edit field");
        setCloseOnOutsideClick(false);
        setWidth("46em");

        name.setHelperText("The key the engine knows this field by. Letters, digits and underscores.");
        name.setWidthFull();
        label.setHelperText("What the person filling the form reads.");
        label.setWidthFull();
        type.setItems(FieldType.values());
        type.setItemLabelGenerator(FieldDialog::readable);
        type.setWidthFull();
        type.addValueChangeListener(event -> onTypeChanged(event.getOldValue(), event.getValue()));

        requiredMessage.setHelperText("Optional. Leave empty for the engine's default.");
        requiredMessage.setWidthFull();
        // Only a required field can be left blank in a way worth wording.
        requiredMessage.setEnabled(required.getValue());
        required.addValueChangeListener(event -> requiredMessage.setEnabled(event.getValue()));

        problems.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
        problems.setVisible(false);

        optionsSection.setPadding(false);
        optionsSection.setSpacing(false);
        optionsSection.add(new H5("Options"), options);

        var rulesSection = new VerticalLayout(new H5("Validation rules"), restrictions);
        rulesSection.setPadding(false);
        rulesSection.setSpacing(false);

        var body = new VerticalLayout(problems, name, label, type, required, requiredMessage,
                rulesSection, optionsSection);
        body.setPadding(false);
        body.setSpacing(false);
        add(body);

        var save = new Button("Save field", event -> submit());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(new Button("Cancel", event -> close()), save);

        populate(field);
    }

    private void populate(FieldDefinitionDTO field) {
        FieldType initial = field == null || field.getType() == null ? FieldType.TEXT : field.getType();
        if (field != null) {
            name.setValue(field.getName() == null ? "" : field.getName());
            label.setValue(field.getLabel() == null ? "" : field.getLabel());
            required.setValue(Boolean.TRUE.equals(field.getRequired()));
            requiredMessage.setValue(field.getRequiredMessage() == null ? "" : field.getRequiredMessage());
        }
        requiredMessage.setEnabled(required.getValue());
        reverting = true;
        type.setValue(initial);
        reverting = false;
        lastType = initial;

        restrictions.setFieldType(initial);
        restrictions.setRestrictions(field == null ? List.of() : field.getRestrictions());
        options.setOptions(field == null ? List.of() : field.getOptions());
        optionsSection.setVisible(takesOptions(initial));
    }

    /**
     * Follows a change of type through to the rules and options that depend on it.
     *
     * <p>A number field cannot carry a length rule and a text field cannot carry options, so a change
     * of type sometimes means throwing things away. That is asked about rather than done quietly, and
     * declining puts the type back.
     */
    private void onTypeChanged(FieldType previous, FieldType chosen) {
        if (reverting || chosen == null) {
            return;
        }

        List<RestrictionType> doomedRules = restrictions.incompatibleWith(chosen);
        boolean losingOptions = !takesOptions(chosen) && options.hasOptions();

        if (doomedRules.isEmpty() && !losingOptions) {
            applyType(chosen);
            return;
        }

        var dialog = new ConfirmDialog();
        dialog.setHeader("Change the type of this field?");
        dialog.setText(consequence(chosen, doomedRules, losingOptions));
        dialog.setCancelable(true);
        dialog.setConfirmText("Change type");
        dialog.addConfirmListener(event -> {
            restrictions.removeIncompatibleWith(chosen);
            if (!takesOptions(chosen)) {
                options.clearOptions();
            }
            applyType(chosen);
        });
        dialog.addCancelListener(event -> revertTo(previous));
        // Dismissing the question is not agreeing to it.
        dialog.addRejectListener(event -> revertTo(previous));
        dialog.open();
    }

    private String consequence(FieldType chosen, List<RestrictionType> doomedRules, boolean losingOptions) {
        var parts = new ArrayList<String>();
        if (!doomedRules.isEmpty()) {
            parts.add("the " + doomedRules.stream().map(RestrictionType::name)
                    .collect(Collectors.joining(", ")) + " rule" + (doomedRules.size() > 1 ? "s" : ""));
        }
        if (losingOptions) {
            parts.add("its " + options.size() + " option" + (options.size() > 1 ? "s" : ""));
        }
        return "A " + readable(chosen).toLowerCase(Locale.ROOT) + " field cannot use "
                + String.join(" or ", parts) + ". Changing the type will remove "
                + (parts.size() > 1 ? "them" : "it") + ".";
    }

    private void applyType(FieldType chosen) {
        lastType = chosen;
        restrictions.setFieldType(chosen);
        optionsSection.setVisible(takesOptions(chosen));
    }

    private void revertTo(FieldType previous) {
        reverting = true;
        type.setValue(previous == null ? lastType : previous);
        reverting = false;
    }

    private void submit() {
        var candidate = FieldDefinitionDTO.builder()
                .id(fieldId)
                .name(name.getValue() == null ? null : name.getValue().trim())
                .label(label.getValue() == null ? null : label.getValue().trim())
                .type(type.getValue())
                .required(required.getValue())
                .requiredMessage(requiredMessageValue())
                .restrictions(new ArrayList<>(restrictions.toDtos()))
                .options(takesOptions(type.getValue())
                        ? new ArrayList<>(options.toDtos()) : new ArrayList<>())
                .build();

        List<String> found = BuilderValidation.validateField(candidate, siblings);
        if (!found.isEmpty()) {
            problems.removeAll();
            found.forEach(problem -> problems.add(new Div(new Span(problem))));
            problems.setVisible(true);
            return;
        }

        close();
        onSave.accept(candidate);
    }

    /** Null rather than empty, so a field with no message of its own stores nothing. */
    private String requiredMessageValue() {
        if (!required.getValue()) {
            return null;
        }
        String written = requiredMessage.getValue();
        return written == null || written.isBlank() ? null : written.trim();
    }

    private static boolean takesOptions(FieldType type) {
        return type == FieldType.SELECT || type == FieldType.MULTI_SELECT;
    }

    private static String readable(FieldType type) {
        return switch (type) {
            case TEXT -> "Text";
            case NUMBER -> "Number";
            case DATE -> "Date";
            case BOOLEAN -> "Yes / no";
            case SELECT -> "Select one";
            case MULTI_SELECT -> "Select several";
        };
    }
}
