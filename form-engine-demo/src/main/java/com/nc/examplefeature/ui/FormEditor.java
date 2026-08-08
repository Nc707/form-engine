package com.nc.examplefeature.ui;

import com.nc.base.ui.ViewToolbar;
import com.nc.examplefeature.TaskService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
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
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route("")
@PageTitle("Form Editor")
@Menu(order = 0, icon = "vaadin:form", title = "Form Editor")
class FormEditor extends VerticalLayout {

    private final FormDefinitionService formService;
    private final TextField formTitle;
    private final VerticalLayout dropZone;
    private final List<TextField> formFields;
    private int fieldCounter = 0;

    FormEditor(FormDefinitionService formService) {
        this.formService = formService;
        this.formFields = new ArrayList<>();

        // Form title input
        formTitle = new TextField();
        formTitle.setPlaceholder("Form Title");
        formTitle.setAriaLabel("Form Title");
        formTitle.setWidth("30em");

        // Submit button
        Button submitBtn = new Button("Submit Form", event -> submitForm());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Toolbar with title and submit button
        HorizontalLayout toolbar = new HorizontalLayout(formTitle, submitBtn);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(Alignment.CENTER);

        // Palette section
        Div palette = createPalette();

        // Drop zone for form fields
        dropZone = new VerticalLayout();
        dropZone.addClassName("drop-zone");
        dropZone.getStyle()
                .setBorder("2px dashed var(--lumo-contrast-30pct)")
                .setBorderRadius("8px")
                .setPadding("2em")
                .setMinHeight("400px")
                .setBackgroundColor("var(--lumo-contrast-5pct)");
        dropZone.setSpacing(true);
        dropZone.setWidth("100%");

        Span dropZoneHint = new Span("Drop form fields here");
        dropZoneHint.getStyle()
                .setColor("var(--lumo-contrast-50pct)");
        dropZone.add(dropZoneHint);

        // Enable drop zone
        enableDropZone();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().setOverflow(Style.Overflow.HIDDEN);

        add(new ViewToolbar("Form Editor", toolbar));
        add(palette);
        add(dropZone);
    }

    private Div createPalette() {
        Div palette = new Div();
        palette.addClassName("palette");
        palette.getStyle()
                .setBorder("1px solid var(--lumo-contrast-20pct)")
                .setBorderRadius("8px")
                .setPadding("1em")
                .setMargin("1em")
                .setBackgroundColor("var(--lumo-base-color)");

        H4 paletteTitle = new H4("Available Fields");
        paletteTitle.getStyle().setMargin("0 0 0.5em 0");

        // Draggable text field component
        Div draggableField = new Div();
        draggableField.setText("📝 Text Field");
        draggableField.getStyle()
                .setBorder("1px solid var(--lumo-contrast-30pct)")
                .setBorderRadius("4px")
                .setPadding("0.5em 1em")
                .setCursor("grab")
                .setBackgroundColor("var(--lumo-primary-color-10pct)");
        
        draggableField.getElement().setAttribute("draggable", "true");
        draggableField.getElement().addEventListener("dragstart", e -> {
            draggableField.getStyle().setCursor("grabbing");
        });
        draggableField.getElement().addEventListener("dragend", e -> {
            draggableField.getStyle().setCursor("grab");
        });

        palette.add(paletteTitle, draggableField);
        return palette;
    }

    private void enableDropZone() {
        dropZone.getElement().addEventListener("dragover", e -> {
            dropZone.getStyle().setBackgroundColor("var(--lumo-primary-color-10pct)");
        }).addEventData("event.preventDefault()");

        dropZone.getElement().addEventListener("dragleave", e -> {
            dropZone.getStyle().setBackgroundColor("var(--lumo-contrast-5pct)");
        });

        dropZone.getElement().addEventListener("drop", e -> {
            addFieldToForm();
            dropZone.getStyle().setBackgroundColor("var(--lumo-contrast-5pct)");
        }).addEventData("event.preventDefault()");
    }

    private void addFieldToForm() {
        fieldCounter++;
        
        HorizontalLayout fieldRow = new HorizontalLayout();
        fieldRow.setWidthFull();
        fieldRow.setAlignItems(Alignment.CENTER);

        TextField newField = new TextField();
        newField.setLabel("Field " + fieldCounter);
        newField.setPlaceholder("Enter value");
        newField.setWidthFull();

        Button removeBtn = new Button("Remove", event -> {
            dropZone.remove(fieldRow);
            formFields.remove(newField);
        });
        removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        fieldRow.add(newField, removeBtn);
        fieldRow.setFlexGrow(1, newField);

        formFields.add(newField);
        
        // Remove hint if first field
        if (formFields.size() == 1) {
            dropZone.removeAll();
        }
        
        dropZone.add(fieldRow);
    }

    private void submitForm() {
        String title = formTitle.getValue();
        
        if (title == null || title.trim().isEmpty()) {
            Notification.show("Please enter a form title", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (formFields.isEmpty()) {
            Notification.show("Please add at least one field", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        FormDefinitionDTO form = FormDefinitionDTO.builder().title(title).build();
        for(TextField field: formFields) {
        	FieldDefinitionDTO formField = FieldDefinitionDTO.builder().
        	form.getFields()
        }

        // Clear form
        formTitle.clear();
        formFields.clear();
        fieldCounter = 0;
        dropZone.removeAll();
        Span dropZoneHint = new Span("Drop form fields here");
        dropZoneHint.getStyle()
                .setColor("var(--lumo-contrast-50pct)");
        dropZone.add(dropZoneHint);
    }
}
