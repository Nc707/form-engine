package com.nc.formengine.ui.builder;

import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The validation rules on one field.
 *
 * <p>Their order is worth caring about: {@code FieldSpecificationFactory.composite} combines them in
 * {@code orderIndex} order and stops at the first failure, so the rule nearest the top is the
 * message a user sees first when several are broken at once.
 */
final class RestrictionListEditor extends VerticalLayout {

    private final VerticalLayout rows = new VerticalLayout();
    private final List<RestrictionRow> restrictionRows = new ArrayList<>();
    private final Span hint = new Span();
    private final Button add = new Button("Add rule", VaadinIcon.PLUS.create(), event -> addRow());

    private FieldType fieldType;

    RestrictionListEditor() {
        setPadding(false);
        setSpacing(false);

        rows.setPadding(false);
        rows.setSpacing(false);

        hint.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        add.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        add(hint, rows, add);
    }

    /**
     * Points every row at the rules the field's type allows.
     *
     * <p>Four of the six types allow none at all. Saying so, and taking the Add button away, is better
     * than offering to add a rule and then showing an empty dropdown.
     */
    void setFieldType(FieldType type) {
        this.fieldType = type;
        restrictionRows.forEach(row -> row.setFieldType(type));
        List<RestrictionType> applicable = RestrictionParameterSpec.applicableTo(type);
        add.setVisible(!applicable.isEmpty());
        if (applicable.isEmpty()) {
            hint.setText("This kind of field has no rules to configure. "
                    + "Use Required above, and its options, to say what a valid answer is.");
        } else if (applicable.size() == 1) {
            hint.setText("One rule applies to this kind of field.");
        } else {
            hint.setText("Rules are checked from the top down, "
                    + "and the first one that fails is the message shown.");
        }
    }

    /** Shows the stored rules, in the order the engine will apply them. */
    void setRestrictions(List<FieldRestrictionDTO> restrictions) {
        rows.removeAll();
        restrictionRows.clear();
        if (restrictions == null) {
            return;
        }
        List<FieldRestrictionDTO> sorted = new ArrayList<>(restrictions);
        sorted.removeIf(Objects::isNull);
        sorted.sort(Comparator.comparing(FieldRestrictionDTO::getOrderIndex,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (FieldRestrictionDTO restriction : sorted) {
            addRow().setRestriction(restriction);
        }
    }

    /** The rules as they now stand, numbered by the order they are shown in. */
    List<FieldRestrictionDTO> toDtos() {
        List<FieldRestrictionDTO> restrictions = new ArrayList<>();
        for (int index = 0; index < restrictionRows.size(); index++) {
            restrictions.add(restrictionRows.get(index).toDto(index));
        }
        return restrictions;
    }

    /** The rules on show that a field of {@code type} could not use. */
    List<RestrictionType> incompatibleWith(FieldType type) {
        List<RestrictionType> applicable = RestrictionParameterSpec.applicableTo(type);
        return restrictionRows.stream()
                .map(RestrictionRow::restrictionType)
                .filter(Objects::nonNull)
                .filter(rule -> !applicable.contains(rule))
                .distinct()
                .toList();
    }

    /** Drops the rules a field of {@code type} could not use, once that has been agreed to. */
    void removeIncompatibleWith(FieldType type) {
        List<RestrictionType> applicable = RestrictionParameterSpec.applicableTo(type);
        List<RestrictionRow> doomed = restrictionRows.stream()
                .filter(row -> row.restrictionType() != null
                        && !applicable.contains(row.restrictionType()))
                .toList();
        doomed.forEach(this::remove);
    }

    private RestrictionRow addRow() {
        // The row's remove button has to name the row, which does not exist until the constructor
        // has returned; the holder is what lets the callback refer to it.
        var holder = new RestrictionRow[1];
        var row = new RestrictionRow(fieldType, () -> remove(holder[0]));
        holder[0] = row;
        restrictionRows.add(row);
        rows.add(row);
        return row;
    }

    private void remove(RestrictionRow row) {
        restrictionRows.remove(row);
        rows.remove(row);
    }
}
