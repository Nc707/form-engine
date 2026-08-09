package com.nc.formengine.ui.builder;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The form's fields, in the order they will be asked.
 *
 * <p>Holds no service and decides nothing: every action is a callback the editor supplies, so the
 * question of whether a form may be changed at all is answered in one place rather than here.
 */
final class FieldListPanel extends VerticalLayout {

    private final Grid<FieldDefinitionDTO> grid = new Grid<>();
    private final Consumer<FieldDefinitionDTO> onEdit;
    private final BiConsumer<FieldDefinitionDTO, Integer> onMove;
    private final Consumer<FieldDefinitionDTO> onDelete;

    private final Button addButton;
    private List<FieldDefinitionDTO> fields = List.of();
    private boolean editable = true;

    FieldListPanel(Runnable onAdd,
                   Consumer<FieldDefinitionDTO> onEdit,
                   BiConsumer<FieldDefinitionDTO, Integer> onMove,
                   Consumer<FieldDefinitionDTO> onDelete) {
        this.onEdit = onEdit;
        this.onMove = onMove;
        this.onDelete = onDelete;

        setPadding(false);
        setSpacing(false);

        addButton = new Button("Add field", VaadinIcon.PLUS.create(), event -> onAdd.run());
        addButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        var header = new HorizontalLayout(new H4("Fields"), addButton);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        buildGrid();
        add(header, grid);
    }

    private void buildGrid() {
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);

        grid.addColumn(FieldDefinitionDTO::getLabel).setHeader("Label").setFlexGrow(1);
        grid.addColumn(FieldDefinitionDTO::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(field -> field.getType() == null ? "" : field.getType().name())
                .setHeader("Type").setAutoWidth(true);
        grid.addColumn(field -> Boolean.TRUE.equals(field.getRequired()) ? "Yes" : "")
                .setHeader("Required").setAutoWidth(true);
        grid.addColumn(FieldListPanel::summary).setHeader("Rules").setAutoWidth(true);
        grid.addComponentColumn(this::rowActions).setHeader("").setAutoWidth(true).setFlexGrow(0);
    }

    /** How much is attached to a field, so a rule is never invisible from the list. */
    private static String summary(FieldDefinitionDTO field) {
        int rules = field.getRestrictions() == null ? 0 : field.getRestrictions().size();
        int options = field.getOptions() == null ? 0 : field.getOptions().size();
        var parts = new ArrayList<String>();
        if (rules > 0) {
            parts.add(rules + (rules == 1 ? " rule" : " rules"));
        }
        if (options > 0) {
            parts.add(options + (options == 1 ? " option" : " options"));
        }
        return String.join(" · ", parts);
    }

    private HorizontalLayout rowActions(FieldDefinitionDTO field) {
        int index = fields.indexOf(field);

        var edit = iconButton(VaadinIcon.EDIT, "Edit", () -> onEdit.accept(field));
        edit.setEnabled(editable);
        var up = iconButton(VaadinIcon.ARROW_UP, "Move up", () -> onMove.accept(field, -1));
        up.setEnabled(editable && index > 0);
        var down = iconButton(VaadinIcon.ARROW_DOWN, "Move down", () -> onMove.accept(field, 1));
        down.setEnabled(editable && index >= 0 && index < fields.size() - 1);
        var remove = iconButton(VaadinIcon.TRASH, "Delete", () -> onDelete.accept(field));
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
        remove.setEnabled(editable);

        var row = new HorizontalLayout(edit, up, down, remove);
        row.setSpacing(false);
        return row;
    }

    /**
     * Shows the fields as they now stand.
     *
     * <p>The editable flag is applied to each button rather than left to the disabled pane around
     * this panel. Flow refuses a click from anything inside a disabled container, so the pane alone
     * is enough to keep a frozen form safe — but a row of buttons that still look pressable on a
     * published form is telling the user something untrue.
     *
     * @param updated  the fields to show
     * @param editable whether the form may still be changed
     */
    void setFields(List<FieldDefinitionDTO> updated, boolean editable) {
        this.fields = updated == null ? List.of() : updated;
        this.editable = editable;
        addButton.setEnabled(editable);
        grid.setItems(this.fields);
    }

    private static Button iconButton(VaadinIcon icon, String tooltip, Runnable onClick) {
        var button = new Button(icon.create(), event -> onClick.run());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        button.getElement().setAttribute("title", tooltip);
        return button;
    }
}
