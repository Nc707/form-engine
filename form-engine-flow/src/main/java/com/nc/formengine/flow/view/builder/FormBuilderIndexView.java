package com.nc.formengine.flow;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Every form the builder knows about, and what may be done with each.
 *
 * <p>Versions are listed under their code rather than mixed together, newest first. A code is the
 * thing that has a history — the same form re-published four times is four rows here — and reading
 * that history is the only way to tell which version is live and which one to branch from.
 *
 * <p>No action on a row is ever offered when the engine would refuse it: what is available comes
 * from {@link FormActions}, the same function the editor uses, so the two cannot disagree about what
 * a published form allows.
 */
// The value is only the default; the path actually used comes from
// formengine.flow.routes and is registered by FormEngineRouteRegistrar, which is also
// why this must not register itself at startup.
@Route(value = "form-engine/definitions", registerAtStartup = false)
@PageTitle("Form builder")
public class FormBuilderIndexView extends VerticalLayout {

    private final FormDefinitionService formService;
    private final Grid<FormDefinitionDTO> grid = new Grid<>();

    FormBuilderIndexView(FormDefinitionService formService) {
        this.formService = formService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var newForm = new Button("New form", VaadinIcon.PLUS.create(),
                event -> navigateToNewForm());
        newForm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        buildGrid();
        add(new ViewToolbar("Form builder", newForm), grid);
        setFlexGrow(1, grid);
        refresh();
    }

    private void buildGrid() {
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);

        // None of these are sortable: the rows are grouped by code on purpose, and one click on a
        // header would scatter each form's versions across the table.
        grid.addColumn(FormDefinitionDTO::getCode).setHeader("Code").setAutoWidth(true).setSortable(false);
        grid.addColumn(FormDefinitionDTO::getTitle).setHeader("Title").setFlexGrow(1).setSortable(false);
        grid.addColumn(form -> "v" + form.getVersion()).setHeader("Version").setAutoWidth(true).setSortable(false);
        grid.addComponentColumn(FormBuilderIndexView::statusBadge).setHeader("Status").setAutoWidth(true);
        grid.addColumn(form -> fieldCount(form) + (fieldCount(form) == 1 ? " field" : " fields"))
                .setHeader("Fields").setAutoWidth(true).setSortable(false);
        grid.addComponentColumn(this::rowActions).setHeader("Actions").setAutoWidth(true)
                .setFlexGrow(0);
    }

    private static Span statusBadge(FormDefinitionDTO form) {
        var status = form.getStatus();
        var badge = new Span(status == null ? "UNKNOWN" : status.name());
        badge.getElement().getThemeList().add("badge");
        if (status == FormDefinitionStatus.PUBLISHED) {
            badge.getElement().getThemeList().add("success");
        } else if (status == FormDefinitionStatus.ARCHIVED) {
            badge.getElement().getThemeList().add("contrast");
        }
        return badge;
    }

    private HorizontalLayout rowActions(FormDefinitionDTO form) {
        FormActions actions = FormActions.of(form.getStatus(), fieldCount(form));

        var edit = action("Edit", actions.editable(), actions.reason(),
                () -> navigateToForm(form.getId()));

        var publish = action("Publish", actions.publishable(), actions.publishBlockedReason(),
                () -> confirm("Publish this form?",
                        "Once published it can no longer be edited, and any version of '"
                                + form.getCode() + "' that is live now will be archived.",
                        "Publish",
                        () -> run(() -> {
                            formService.publish(form.getId());
                            Notifications.success("Published '" + form.getTitle() + "'.");
                        })));

        var newVersion = action("New version", actions.versionable(),
                "Only a published or archived form can be versioned.",
                () -> run(() -> {
                    var draft = formService.createNewVersion(form.getId());
                    Notifications.success("Started version " + draft.getVersion() + ".");
                    navigateToForm(draft.getId());
                }));

        var delete = action("Delete", actions.deletable(), actions.reason(),
                () -> confirm("Delete this draft?",
                        "'" + form.getTitle() + "' and everything in it will be removed. "
                                + "This cannot be undone.",
                        "Delete",
                        () -> run(() -> {
                            formService.deleteById(form.getId());
                            Notifications.success("Deleted '" + form.getTitle() + "'.");
                        })));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        var actionsRow = new HorizontalLayout(edit, publish, newVersion, delete);
        actionsRow.setSpacing(true);
        return actionsRow;
    }

    private Button action(String label, boolean enabled, String disabledReason, Runnable onClick) {
        var button = new Button(label, event -> onClick.run());
        button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        button.setEnabled(enabled);
        if (!enabled && disabledReason != null) {
            // A disabled button with no explanation reads as a bug in the builder.
            button.getElement().setAttribute("title", disabledReason);
        }
        return button;
    }

    /**
     * The forms, with each code's versions kept together and newest first.
     *
     * <p>{@code findAllVersionsByCode} returns them oldest first, which is the right order for a
     * history and the wrong one for a list whose first row should be the version anyone would act on.
     */
    private List<FormDefinitionDTO> groupedByCode() {
        List<String> codes = formService.findAll().stream()
                .map(FormDefinitionDTO::getCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        List<FormDefinitionDTO> rows = new ArrayList<>();
        for (String code : codes) {
            List<FormDefinitionDTO> versions = new ArrayList<>(formService.findAllVersionsByCode(code));
            versions.sort(Comparator.comparing(FormDefinitionDTO::getVersion,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            rows.addAll(versions);
        }
        return rows;
    }

    private void refresh() {
        grid.setItems(groupedByCode());
    }

    /** Runs a lifecycle action, showing whatever the domain says when it refuses. */
    private void run(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            Notifications.error(ex.getMessage());
        } finally {
            refresh();
        }
    }

    private void confirm(String header, String text, String confirmLabel, Runnable onConfirm) {
        var dialog = new ConfirmDialog();
        dialog.setHeader(header);
        dialog.setText(text);
        dialog.setCancelable(true);
        dialog.setConfirmText(confirmLabel);
        dialog.addConfirmListener(event -> onConfirm.run());
        dialog.open();
    }

    private void navigateToNewForm() {
        getUI().ifPresent(ui -> ui.navigate(FormBuilderView.class));
    }

    private void navigateToForm(Long formId) {
        getUI().ifPresent(ui -> ui.navigate(FormBuilderView.class,
                new RouteParameters(FormBuilderView.FORM_ID, String.valueOf(formId))));
    }

    private static int fieldCount(FormDefinitionDTO form) {
        return form.getFields() == null ? 0 : form.getFields().size();
    }
}
