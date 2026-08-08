package com.nc.formengine.ui.builder;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.ui.shared.ViewToolbar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal form builder: drag a field from the palette onto the canvas, name it, and save the
 * resulting {@link FormDefinitionDTO} through the business layer.
 *
 * <p>This is the foundation version. The full builder (all field types, property panel, restriction
 * and dependency editors) replaces it in a later phase.
 */
@Route("")
@PageTitle("Form Builder")
@Menu(order = 0, icon = "vaadin:form", title = "Form Builder")
class FormEditorView extends VerticalLayout {

    private final FormDefinitionService formService;
    private final TextField formTitle;
    private final VerticalLayout canvas;
    private final List<TextField> fieldLabels = new ArrayList<>();
    private int fieldCounter = 0;

    FormEditorView(FormDefinitionService formService) {
        this.formService = formService;

        formTitle = new TextField();
        formTitle.setPlaceholder("Form title");
        formTitle.setAriaLabel("Form title");
        formTitle.setWidth("30em");

        var saveButton = new Button("Save form", event -> saveForm());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var toolbar = new HorizontalLayout(formTitle, saveButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(Alignment.CENTER);

        canvas = new VerticalLayout();
        canvas.addClassName("drop-zone");
        canvas.getStyle()
                .setBorder("2px dashed var(--lumo-contrast-30pct)")
                .setBorderRadius("8px")
                .setPadding("2em")
                .setMinHeight("400px")
                .setBackgroundColor("var(--lumo-contrast-5pct)");
        canvas.setSpacing(true);
        canvas.setWidth("100%");
        showEmptyHint();
        enableDropZone();

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(new ViewToolbar("Form Builder", toolbar), createPalette(), canvas);
    }

    private Div createPalette() {
        var palette = new Div();
        palette.addClassName("palette");
        palette.getStyle()
                .setBorder("1px solid var(--lumo-contrast-20pct)")
                .setBorderRadius("8px")
                .setPadding("1em")
                .setMargin("1em")
                .setBackgroundColor("var(--lumo-base-color)");

        var paletteTitle = new H4("Available fields");
        paletteTitle.getStyle().setMargin("0 0 0.5em 0");

        var draggableField = new Div();
        draggableField.setText("Text field");
        draggableField.getStyle()
                .setBorder("1px solid var(--lumo-contrast-30pct)")
                .setBorderRadius("4px")
                .setPadding("0.5em 1em")
                .setCursor("grab")
                .setBackgroundColor("var(--lumo-primary-color-10pct)");
        draggableField.getElement().setAttribute("draggable", "true");
        draggableField.getElement()
                .addEventListener("dragstart", e -> draggableField.getStyle().setCursor("grabbing"));
        draggableField.getElement()
                .addEventListener("dragend", e -> draggableField.getStyle().setCursor("grab"));

        palette.add(paletteTitle, draggableField);
        return palette;
    }

    private void enableDropZone() {
        canvas.getElement()
                .addEventListener("dragover",
                        e -> canvas.getStyle().setBackgroundColor("var(--lumo-primary-color-10pct)"))
                .addEventData("event.preventDefault()");

        canvas.getElement()
                .addEventListener("dragleave",
                        e -> canvas.getStyle().setBackgroundColor("var(--lumo-contrast-5pct)"));

        canvas.getElement().addEventListener("drop", e -> {
            addField();
            canvas.getStyle().setBackgroundColor("var(--lumo-contrast-5pct)");
        }).addEventData("event.preventDefault()");
    }

    private void addField() {
        fieldCounter++;

        var labelInput = new TextField();
        labelInput.setLabel("Field " + fieldCounter + " label");
        labelInput.setPlaceholder("e.g. Full name");
        labelInput.setWidthFull();

        var fieldRow = new HorizontalLayout();
        fieldRow.setWidthFull();
        fieldRow.setAlignItems(Alignment.BASELINE);

        var removeButton = new Button("Remove", event -> {
            canvas.remove(fieldRow);
            fieldLabels.remove(labelInput);
            if (fieldLabels.isEmpty()) {
                showEmptyHint();
            }
        });
        removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        fieldRow.add(labelInput, removeButton);
        fieldRow.setFlexGrow(1, labelInput);

        if (fieldLabels.isEmpty()) {
            canvas.removeAll();
        }
        fieldLabels.add(labelInput);
        canvas.add(fieldRow);
    }

    private void saveForm() {
        var title = formTitle.getValue();
        if (title == null || title.isBlank()) {
            error("Please enter a form title");
            return;
        }
        if (fieldLabels.isEmpty()) {
            error("Please add at least one field");
            return;
        }

        var fields = new ArrayList<FieldDefinitionDTO>();
        for (var labelInput : fieldLabels) {
            var label = labelInput.getValue();
            if (label == null || label.isBlank()) {
                error("Every field needs a label");
                return;
            }
            fields.add(FieldDefinitionDTO.builder()
                    .name(toIdentifier(label))
                    .label(label)
                    .type(FieldType.TEXT)
                    .orderIndex(fields.size())
                    .required(false)
                    .build());
        }

        var form = FormDefinitionDTO.builder()
                .code(toIdentifier(title))
                .title(title)
                .version(1)
                .fields(fields)
                .build();

        try {
            var saved = formService.create(form);
            Notification.show("Saved form '" + saved.getTitle() + "' (id " + saved.getId() + ")",
                            3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            resetEditor();
        } catch (RuntimeException ex) {
            error("Could not save the form: " + ex.getMessage());
        }
    }

    private void resetEditor() {
        formTitle.clear();
        fieldLabels.clear();
        fieldCounter = 0;
        canvas.removeAll();
        showEmptyHint();
    }

    private void showEmptyHint() {
        var hint = new Span("Drag a field from the palette to start building");
        hint.getStyle().setColor("var(--lumo-contrast-50pct)");
        canvas.add(hint);
    }

    private void error(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /** Turns a human label into a stable snake_case identifier usable as a field or form code. */
    private static String toIdentifier(String text) {
        var slug = text.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        return slug.replaceAll("^_|_$", "");
    }
}
