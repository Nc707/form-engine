package com.nc.formengine.flow;

import com.nc.formengine.model.dto.FieldOptionDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The choices a select field offers.
 *
 * <p>An option is two things at once: a label, which is what the person filling the form reads, and
 * a value, which is what the answer is stored as and what a conditional dependency compares against.
 * Keeping them separate matters — renaming a label should not invalidate every submission made
 * before it — but asking for both is a good way to collect typos, so the value is filled in from the
 * label until someone edits it deliberately.
 */
final class OptionListEditor extends VerticalLayout {

    private final VerticalLayout rows = new VerticalLayout();
    private final List<OptionRow> optionRows = new ArrayList<>();

    OptionListEditor() {
        setPadding(false);
        setSpacing(false);

        rows.setPadding(false);
        rows.setSpacing(false);

        var add = new Button("Add option", VaadinIcon.PLUS.create(), event -> addRow(null));
        add.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        add(rows, add);
    }

    /** Replaces what is shown with the stored options, in their own order. */
    void setOptions(List<FieldOptionDTO> options) {
        rows.removeAll();
        optionRows.clear();
        if (options != null) {
            List<FieldOptionDTO> sorted = new ArrayList<>(options);
            sorted.sort(java.util.Comparator.comparing(FieldOptionDTO::getOrderIndex,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
            sorted.forEach(this::addRow);
        }
        showEmptyHint();
    }

    /** The options as they now stand, numbered 0..n-1 by the order they are shown in. */
    List<FieldOptionDTO> toDtos() {
        List<FieldOptionDTO> options = new ArrayList<>();
        for (OptionRow row : optionRows) {
            options.add(row.toDto());
        }
        OrderIndexes.reindex(options, FieldOptionDTO::setOrderIndex);
        return options;
    }

    boolean hasOptions() {
        return !optionRows.isEmpty();
    }

    int size() {
        return optionRows.size();
    }

    void clearOptions() {
        rows.removeAll();
        optionRows.clear();
        showEmptyHint();
    }

    private void addRow(FieldOptionDTO option) {
        var row = new OptionRow(option);
        optionRows.add(row);
        rows.add(row);
        showEmptyHint();
    }

    private void remove(OptionRow row) {
        optionRows.remove(row);
        rows.remove(row);
        showEmptyHint();
    }

    private void move(OptionRow row, int delta) {
        int index = optionRows.indexOf(row);
        if (OrderIndexes.swap(optionRows, index, delta)) {
            rows.removeAll();
            optionRows.forEach(rows::add);
        }
    }

    private void showEmptyHint() {
        if (!optionRows.isEmpty()) {
            return;
        }
        var hint = new Span("A select field needs at least one option.");
        hint.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        rows.add(hint);
    }

    /** One choice: what it reads as, what it is stored as, and where it sits. */
    private final class OptionRow extends HorizontalLayout {

        private final Long id;
        private final TextField label = new TextField();
        private final TextField value = new TextField();

        /** Once the value has been typed into, the label stops writing over it. */
        private boolean valueEdited;

        private OptionRow(FieldOptionDTO option) {
            this.id = option == null ? null : option.getId();

            label.setPlaceholder("Label");
            label.setWidthFull();
            label.setValueChangeMode(ValueChangeMode.EAGER);
            value.setPlaceholder("Value");
            value.setWidthFull();
            value.setValueChangeMode(ValueChangeMode.EAGER);

            if (option != null) {
                label.setValue(option.getLabel() == null ? "" : option.getLabel());
                value.setValue(option.getValue() == null ? "" : option.getValue());
                valueEdited = true;
            }

            label.addValueChangeListener(event -> {
                if (!valueEdited) {
                    value.setValue(slug(label.getValue()));
                }
            });
            value.addValueChangeListener(event -> {
                if (event.isFromClient()) {
                    valueEdited = true;
                }
            });

            var up = iconButton(VaadinIcon.ARROW_UP, () -> move(this, -1));
            var down = iconButton(VaadinIcon.ARROW_DOWN, () -> move(this, 1));
            var remove = iconButton(VaadinIcon.CLOSE_SMALL, () -> OptionListEditor.this.remove(this));
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR);

            setWidthFull();
            setSpacing(false);
            setAlignItems(Alignment.CENTER);
            add(label, value, up, down, remove);
            setFlexGrow(1, label);
            setFlexGrow(1, value);
        }

        private FieldOptionDTO toDto() {
            return FieldOptionDTO.builder()
                    .id(id)
                    .label(label.getValue() == null ? null : label.getValue().trim())
                    .value(value.getValue() == null ? null : value.getValue().trim())
                    .build();
        }
    }

    private static Button iconButton(VaadinIcon icon, Runnable onClick) {
        var button = new Button(icon.create(), event -> onClick.run());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        return button;
    }

    /** Turns a label into something usable as a stored value. */
    private static String slug(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        return cleaned.replaceAll("^_|_$", "");
    }
}
