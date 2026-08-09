package com.nc.formengine.ui.builder;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.ui.shared.FieldComponentFactory;
import com.nc.formengine.ui.shared.FieldEditor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The form as the person filling it in will see it.
 *
 * <p>Built with the same {@code FieldComponentFactory} the renderer uses, so what the builder shows
 * is not an impression of the form but the form itself, only read-only. Anything that looks wrong
 * here will look wrong there.
 *
 * <p>Fields are stacked in {@code orderIndex} order and nothing more: placing them on the
 * twelve-column grid is the layout editor's job, and guessing at it here would show a form that does
 * not exist.
 */
final class FormPreview extends VerticalLayout {

    private final H3 heading = new H3();
    private final Paragraph description = new Paragraph();
    private final VerticalLayout stack = new VerticalLayout();
    private final List<FieldEditor> editors = new ArrayList<>();

    FormPreview() {
        setPadding(true);
        setSpacing(false);

        heading.addClassNames(LumoUtility.Margin.Bottom.NONE);
        description.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.XSMALL);

        stack.setPadding(false);
        stack.setSpacing(true);
        stack.setWidthFull();

        add(heading, description, stack);
    }

    /**
     * Updates the title and description without touching the fields.
     *
     * <p>Typing in the header must not rebuild the form: every input would be thrown away and
     * rebuilt on each keystroke, taking the scroll position with it.
     */
    void setHeading(String title, String descriptionText) {
        heading.setText(title == null || title.isBlank() ? "Untitled form" : title);
        description.setText(descriptionText == null ? "" : descriptionText);
        description.setVisible(descriptionText != null && !descriptionText.isBlank());
    }

    /**
     * Rebuilds the preview from the stored form.
     *
     * @param form         the form being edited
     * @param dependencies its conditional dependencies, noted under the fields they affect
     */
    void refresh(FormDefinitionDTO form, List<FieldDependencyDTO> dependencies) {
        stack.removeAll();
        editors.clear();

        if (form == null) {
            return;
        }
        setHeading(form.getTitle(), form.getDescription());

        List<FieldDefinitionDTO> fields = new ArrayList<>(
                form.getFields() == null ? List.of() : form.getFields());
        fields.removeIf(Objects::isNull);
        fields.sort(OrderIndexes.byOrder());

        if (fields.isEmpty()) {
            var empty = new Span("This form has no fields yet.");
            empty.addClassNames(LumoUtility.TextColor.SECONDARY);
            stack.add(empty);
            return;
        }

        Map<Long, String> labels = labelsById(fields);
        Map<Long, List<FieldDependencyDTO>> affecting = affectingByDependent(dependencies);

        for (FieldDefinitionDTO field : fields) {
            // The factory switches on the type and would fail on a field that has none. Validation
            // makes that unreachable, but the preview must not be the thing that breaks the page.
            if (field.getType() == null) {
                continue;
            }
            FieldEditor editor = FieldComponentFactory.create(field);
            editor.setReadOnly(true);
            editors.add(editor);

            var slot = new Div(editor.component());
            slot.setWidthFull();
            for (FieldDependencyDTO dependency : affecting.getOrDefault(field.getId(), List.of())) {
                // A conditional field looks like any other field until something says otherwise.
                var note = new Span(DependencyText.note(
                        dependency, labels.get(dependency.getTriggerFieldId())));
                note.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
                slot.add(note);
            }
            stack.add(slot);
        }
    }

    /** The read-only editors currently on show, in the order they render. */
    List<FieldEditor> editors() {
        return List.copyOf(editors);
    }

    private static Map<Long, String> labelsById(List<FieldDefinitionDTO> fields) {
        Map<Long, String> labels = new LinkedHashMap<>();
        for (FieldDefinitionDTO field : fields) {
            labels.put(field.getId(),
                    field.getLabel() == null ? field.getName() : field.getLabel());
        }
        return labels;
    }

    private static Map<Long, List<FieldDependencyDTO>> affectingByDependent(
            List<FieldDependencyDTO> dependencies) {
        Map<Long, List<FieldDependencyDTO>> byDependent = new LinkedHashMap<>();
        if (dependencies == null) {
            return byDependent;
        }
        for (FieldDependencyDTO dependency : dependencies) {
            if (dependency != null && dependency.getDependentFieldId() != null) {
                byDependent.computeIfAbsent(dependency.getDependentFieldId(), key -> new ArrayList<>())
                        .add(dependency);
            }
        }
        return byDependent;
    }
}
