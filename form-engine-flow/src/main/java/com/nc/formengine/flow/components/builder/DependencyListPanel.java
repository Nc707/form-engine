package com.nc.formengine.flow.components.builder;

import com.nc.formengine.flow.utils.builder.DependencyText;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The rules that make parts of the form appear, disappear or become required.
 *
 * <p>Each one is shown as the sentence it means rather than as the four enum values it is stored as.
 * This list is where a rule pointing at the wrong field, or comparing against a value that can never
 * occur, is going to be noticed — and only if it reads like something a person wrote.
 */
public final class DependencyListPanel extends VerticalLayout {

    private final Grid<FieldDependencyDTO> grid = new Grid<>();
    private final Button add = new Button("Add rule", VaadinIcon.PLUS.create());
    private final Span hint = new Span();

    private Map<Long, FieldDefinitionDTO> fieldsById = Map.of();
    private boolean editable = true;

    public DependencyListPanel(Runnable onAdd,
                        Consumer<FieldDependencyDTO> onEdit,
                        Consumer<FieldDependencyDTO> onDelete) {
        setPadding(false);
        setSpacing(false);

        add.addThemeVariants(ButtonVariant.LUMO_SMALL);
        add.addClickListener(event -> onAdd.run());

        var header = new HorizontalLayout(new H4("Conditional rules"), add);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        hint.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        grid.addColumn(this::sentence).setHeader("Rule").setFlexGrow(1);
        grid.addComponentColumn(dependency -> {
            var edit = iconButton(VaadinIcon.EDIT, "Edit", () -> onEdit.accept(dependency));
            edit.setEnabled(editable);
            var remove = iconButton(VaadinIcon.TRASH, "Delete", () -> onDelete.accept(dependency));
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
            remove.setEnabled(editable);
            var actions = new HorizontalLayout(edit, remove);
            actions.setSpacing(false);
            return actions;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);

        add(header, hint, grid);
    }

    /**
     * Shows the rules against the fields they name.
     *
     * @param dependencies the form's rules
     * @param fields       the form's fields, needed to turn ids back into labels
     * @param editable     whether the form may still be changed
     */
    public void setDependencies(List<FieldDependencyDTO> dependencies,
                         Map<Long, FieldDefinitionDTO> fields,
                         boolean editable) {
        this.fieldsById = fields;
        this.editable = editable;
        grid.setItems(dependencies);
        grid.setVisible(!dependencies.isEmpty());

        // A rule needs one field to watch and a different one to affect, so two is the floor.
        boolean possible = fields.size() >= 2;
        add.setEnabled(editable && possible);
        if (!possible) {
            hint.setText("Add at least two fields before making one depend on another.");
        } else if (dependencies.isEmpty()) {
            hint.setText("No conditional rules yet. Every field is always shown.");
        } else {
            hint.setText("");
        }
        hint.setVisible(!hint.getText().isEmpty());
    }

    private String sentence(FieldDependencyDTO dependency) {
        return DependencyText.sentence(dependency,
                labelOf(dependency.getTriggerFieldId()),
                labelOf(dependency.getDependentFieldId()));
    }

    private String labelOf(Long fieldId) {
        FieldDefinitionDTO field = fieldsById.get(fieldId);
        if (field == null) {
            // Only reachable if a field went away underneath us; saying so beats an empty cell.
            return "a deleted field";
        }
        return field.getLabel() == null ? field.getName() : field.getLabel();
    }

    private static Button iconButton(VaadinIcon icon, String tooltip, Runnable onClick) {
        var button = new Button(icon.create(), event -> onClick.run());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        button.getElement().setAttribute("title", tooltip);
        return button;
    }
}
