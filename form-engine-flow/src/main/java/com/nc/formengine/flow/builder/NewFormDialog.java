package com.nc.formengine.flow.builder;

import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Asks for what a form needs before it can exist.
 *
 * <p>The code is here and nowhere else. It names the form across every version it will ever have,
 * and {@code FormDefinitionService.update} replaces whatever a caller sends with the stored one — so
 * offering it in the editor would be offering a field that silently does nothing. This is the only
 * moment it can be chosen.
 */
final class NewFormDialog extends Dialog {

    private final TextField code = new TextField("Code");
    private final TextField title = new TextField("Title");
    private final TextArea description = new TextArea("Description");
    private final Div problems = new Div();

    private final Predicate<String> codeTaken;
    private final Consumer<FormDefinitionDTO> onCreate;
    private final Runnable onCancel;

    private boolean creating;

    /**
     * @param codeTaken whether a code is already claimed, asked before saving rather than after
     * @param onCreate  receives the form to create
     * @param onCancel  run when the dialog is dismissed without creating anything
     */
    NewFormDialog(Predicate<String> codeTaken,
                  Consumer<FormDefinitionDTO> onCreate,
                  Runnable onCancel) {
        this.codeTaken = codeTaken;
        this.onCreate = onCreate;
        this.onCancel = onCancel;

        setHeaderTitle("New form");
        setCloseOnOutsideClick(false);
        setWidth("32em");

        code.setHelperText("Lowercase letters, digits and underscores. This cannot be changed later.");
        code.setWidthFull();
        title.setWidthFull();
        description.setWidthFull();
        description.setMaxLength(1000);

        problems.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
        problems.setVisible(false);

        var layout = new VerticalLayout(problems, code, title, description);
        layout.setPadding(false);
        layout.setSpacing(false);
        add(layout);

        var create = new Button("Create", event -> submit());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(new Button("Cancel", event -> close()), create);

        // Escape and the cancel button take the same way out, so a dismissed dialog never strands
        // the editor on a form that was never created.
        addOpenedChangeListener(event -> {
            if (!event.isOpened() && !creating) {
                onCancel.run();
            }
        });
    }

    private void submit() {
        List<String> found = new ArrayList<>(
                BuilderValidation.validateNewForm(code.getValue(), title.getValue()));
        if (found.isEmpty() && codeTaken.test(code.getValue())) {
            found.add("A form with the code '" + code.getValue() + "' already exists. "
                    + "Use 'New version' on it instead.");
        }

        if (!found.isEmpty()) {
            showProblems(found);
            return;
        }

        creating = true;
        close();
        onCreate.accept(FormDefinitionDTO.builder()
                .code(code.getValue().trim())
                .title(title.getValue().trim())
                .description(description.getValue())
                .version(1)
                .build());
    }

    private void showProblems(List<String> found) {
        problems.removeAll();
        found.forEach(problem -> problems.add(new Div(new Span(problem))));
        problems.setVisible(true);
    }
}
