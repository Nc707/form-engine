package com.nc.formengine.flow.view.builder;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.business.service.FieldDependencyService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.business.service.FormLayoutService;
import com.nc.formengine.flow.autoconfigure.FormEngineRouteRegistrar;
import com.nc.formengine.flow.components.builder.DependencyDialog;
import com.nc.formengine.flow.components.builder.DependencyListPanel;
import com.nc.formengine.flow.components.builder.FieldDialog;
import com.nc.formengine.flow.components.builder.FieldListPanel;
import com.nc.formengine.flow.components.builder.FormPreview;
import com.nc.formengine.flow.components.builder.NewFormDialog;
import com.nc.formengine.flow.components.common.ViewToolbar;
import com.nc.formengine.flow.utils.Notifications;
import com.nc.formengine.flow.utils.builder.DependencyText;
import com.nc.formengine.flow.utils.builder.FormActions;
import com.nc.formengine.flow.utils.builder.FormDraftSession;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Edits one form definition.
 *
 * <p>The route's id is optional in shape only. Arriving without one opens {@link NewFormDialog} and
 * navigates straight to the form it creates, so the editor is never looking at a form that has not
 * been saved. That is not a convenience: a dependency references its fields by id and cannot be
 * created before they have one, and the only way to guarantee every field on screen has an id is for
 * the form itself to always be persisted.
 *
 * <p><b>A frozen form is disabled, not refused.</b> Everything that edits the definition lives in a
 * single pane, which is disabled outright when the form is no longer a draft — Flow propagates that
 * to children added afterwards and rejects server-side calls from disabled components, so no control
 * added here later can forget to check. What stays outside it is the preview, which is readable
 * rather than greyed out, and the banner and "New version" button, which are how a published form is
 * meant to be changed.
 */
// The value is only the default; the path actually used comes from
// formengine.flow.routes and is registered by FormEngineRouteRegistrar, which is also
// why this must not register itself at startup.
@Route(value = "form-engine/builder/:formId?", registerAtStartup = false)
@PageTitle("Form builder")
public class FormBuilderView extends VerticalLayout implements BeforeEnterObserver, BeforeLeaveObserver {

    static final String FORM_ID = "formId";

    private final FormDefinitionService formService;
    private final FieldDefinitionService fieldService;
    private final FieldDependencyService dependencyService;
    private final FormLayoutService layoutService;

    private final TextField title = new TextField("Title");
    private final TextArea description = new TextArea("Description");
    private final Button saveDetails = new Button("Save details");

    private final Div banner = new Div();
    private final VerticalLayout editorPane = new VerticalLayout();
    private final FormPreview preview = new FormPreview();
    private final FieldListPanel fieldList = new FieldListPanel(
            this::addField, this::editField, this::moveField, this::deleteField);
    private final DependencyListPanel dependencyList = new DependencyListPanel(
            this::addDependency, this::editDependency, this::deleteDependency);

    private final Button publish = new Button("Publish");
    private final Button newVersion = new Button("New version");
    private final Button delete = new Button("Delete");

    private FormDraftSession session;

    FormBuilderView(FormDefinitionService formService,
                    FieldDefinitionService fieldService,
                    FieldDependencyService dependencyService,
                    FormLayoutService layoutService) {
        this.formService = formService;
        this.fieldService = fieldService;
        this.dependencyService = dependencyService;
        this.layoutService = layoutService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var workspace = workspace();
        add(toolbar(), banner, workspace);
        setFlexGrow(1, workspace);
    }

    // --- layout ------------------------------------------------------------------------------

    private ViewToolbar toolbar() {
        var back = new Button("All forms", event -> navigateToIndex());

        publish.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        publish.addClickListener(event -> confirm("Publish this form?",
                "Once published it can no longer be edited, and any version of '"
                        + session.form().getCode() + "' that is live now will be archived.",
                "Publish",
                () -> mutate(() -> session.publish(), "Published.")));

        newVersion.addClickListener(event -> {
            try {
                Long draftId = session.createNewVersion();
                Notifications.success("Started a new draft from this version.");
                navigateToForm(draftId);
            } catch (RuntimeException ex) {
                Notifications.error(ex.getMessage());
            }
        });

        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> confirm("Delete this draft?",
                "'" + session.form().getTitle() + "' and everything in it will be removed. "
                        + "This cannot be undone.",
                "Delete",
                () -> {
                    try {
                        session.deleteForm();
                        Notifications.success("Deleted the draft.");
                        navigateToIndex();
                    } catch (RuntimeException ex) {
                        Notifications.error(ex.getMessage());
                    }
                }));

        banner.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Background.CONTRAST_5,
                LumoUtility.TextColor.SECONDARY);
        banner.setVisible(false);

        return new ViewToolbar("Form builder",
                ViewToolbar.group(back, publish, newVersion, delete));
    }

    private SplitLayout workspace() {
        editorPane.setPadding(true);
        editorPane.setSpacing(true);

        title.setWidthFull();
        title.setValueChangeMode(ValueChangeMode.LAZY);
        description.setWidthFull();
        description.setMaxLength(1000);
        description.setValueChangeMode(ValueChangeMode.LAZY);

        // The heading follows what is being typed, but nothing else does: rebuilding the preview on
        // every keystroke would throw away and rebuild every input, along with the scroll position.
        title.addValueChangeListener(event -> preview.setHeading(title.getValue(), description.getValue()));
        description.addValueChangeListener(event -> preview.setHeading(title.getValue(), description.getValue()));

        saveDetails.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveDetails.addClickListener(event ->
                mutate(() -> session.saveDetails(title.getValue(), description.getValue()),
                        "Saved."));

        var details = new VerticalLayout(new H4("Details"), title, description,
                new HorizontalLayout(saveDetails));
        details.setPadding(false);
        details.setSpacing(false);
        editorPane.add(details, fieldList, dependencyList);

        var previewPane = new VerticalLayout(new H4("Preview"), preview);
        previewPane.setPadding(true);
        previewPane.setSpacing(false);
        previewPane.setWidthFull();

        // Both halves outgrow the window on any real form, and a split layout hands its children a
        // fixed height. Left to themselves the two vertical layouts would squash their contents to
        // fit rather than overflow, so the bottom of a long form could not be reached at all; a
        // scroller keeps the content at its natural height and scrolls to it instead.
        var split = new SplitLayout(scrolling(editorPane), scrolling(previewPane));
        split.setSizeFull();
        split.setSplitterPosition(55);
        return split;
    }

    private static Scroller scrolling(Component content) {
        var scroller = new Scroller(content);
        scroller.setSizeFull();
        return scroller;
    }

    // --- navigation --------------------------------------------------------------------------

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<String> parameter = event.getRouteParameters().get(FORM_ID);
        if (parameter.isEmpty()) {
            openNewFormDialog();
            return;
        }

        long formId;
        try {
            formId = Long.parseLong(parameter.get());
        } catch (NumberFormatException ex) {
            Notifications.error("'" + parameter.get() + "' is not a form.");
            event.forwardTo("");
            return;
        }

        try {
            session = new FormDraftSession(formService, fieldService, dependencyService,
                    layoutService, formId);
        } catch (RuntimeException ex) {
            Notifications.error(ex.getMessage());
            event.forwardTo("");
            return;
        }
        refreshFromModel();
    }

    private void openNewFormDialog() {
        // Nothing to show behind it: the form does not exist yet.
        editorPane.setVisible(false);
        new NewFormDialog(
                formService::existsByCode,
                form -> {
                    try {
                        var created = formService.create(form);
                        Notifications.success("Created '" + created.getTitle() + "'.");
                        navigateToForm(created.getId());
                    } catch (RuntimeException ex) {
                        Notifications.error(ex.getMessage());
                        navigateToIndex();
                    }
                },
                this::navigateToIndex).open();
    }

    /**
     * Warns before walking away from unsaved details.
     *
     * <p>The two header fields are the only thing in the builder that is edited outside a dialog, so
     * they are the only thing that can be dirty. Everything else is written the moment its dialog is
     * confirmed.
     */
    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (session == null || !session.actions().editable() || !isHeaderDirty()) {
            return;
        }
        BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();
        var dialog = new ConfirmDialog();
        dialog.setHeader("Leave without saving?");
        dialog.setText("The title or description has been changed and not saved yet.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Leave");
        dialog.addConfirmListener(confirmed -> action.proceed());
        dialog.open();
    }

    private boolean isHeaderDirty() {
        return !Objects.equals(nullToEmpty(title.getValue()), nullToEmpty(session.form().getTitle()))
                || !Objects.equals(nullToEmpty(description.getValue()),
                        nullToEmpty(session.form().getDescription()));
    }

    // --- fields ------------------------------------------------------------------------------

    private void addField() {
        new FieldDialog(null, session.fields(),
                field -> mutate(() -> session.addField(field), "Added '" + field.getLabel() + "'."))
                .open();
    }

    private void editField(FieldDefinitionDTO field) {
        // The dialog edits a copy, so cancelling leaves the list showing what is actually stored.
        List<FieldDefinitionDTO> others = session.fields().stream()
                .filter(other -> !Objects.equals(other.getId(), field.getId()))
                .toList();
        new FieldDialog(copyOf(field), others,
                edited -> mutate(() -> session.saveField(edited), "Saved '" + edited.getLabel() + "'."))
                .open();
    }

    private void moveField(FieldDefinitionDTO field, int delta) {
        mutate(() -> session.moveField(field.getId(), delta), "Reordered.");
    }

    private void deleteField(FieldDefinitionDTO field) {
        List<FieldDependencyDTO> affected = session.dependencies().stream()
                .filter(dependency -> Objects.equals(dependency.getTriggerFieldId(), field.getId())
                        || Objects.equals(dependency.getDependentFieldId(), field.getId()))
                .toList();

        String consequence = affected.isEmpty()
                ? "'" + field.getLabel() + "' will be removed from this form."
                : "'" + field.getLabel() + "' will be removed, along with "
                        + affected.size() + " conditional rule" + (affected.size() > 1 ? "s" : "")
                        + " that mention it.";

        confirm("Delete this field?", consequence, "Delete",
                () -> mutate(() -> session.deleteField(field.getId()),
                        "Deleted '" + field.getLabel() + "'."));
    }

    /** A field the dialog can edit freely without touching what the list is showing. */
    private static FieldDefinitionDTO copyOf(FieldDefinitionDTO field) {
        return FieldDefinitionDTO.builder()
                .id(field.getId())
                .formDefinitionId(field.getFormDefinitionId())
                .name(field.getName())
                .label(field.getLabel())
                .type(field.getType())
                .orderIndex(field.getOrderIndex())
                .required(field.getRequired())
                .restrictions(field.getRestrictions() == null ? new ArrayList<>()
                        : field.getRestrictions().stream().map(FormBuilderView::copyOf).collect(
                                Collectors.toCollection(ArrayList::new)))
                .options(field.getOptions() == null ? new ArrayList<>()
                        : field.getOptions().stream().map(FormBuilderView::copyOf).collect(
                                Collectors.toCollection(ArrayList::new)))
                .build();
    }

    private static FieldRestrictionDTO copyOf(FieldRestrictionDTO restriction) {
        return FieldRestrictionDTO.builder()
                .id(restriction.getId())
                .fieldDefinitionId(restriction.getFieldDefinitionId())
                .restrictionType(restriction.getRestrictionType())
                .parameters(restriction.getParameters() == null
                        ? new LinkedHashMap<>() : new LinkedHashMap<>(restriction.getParameters()))
                .errorMessage(restriction.getErrorMessage())
                .orderIndex(restriction.getOrderIndex())
                .build();
    }

    private static FieldOptionDTO copyOf(FieldOptionDTO option) {
        return FieldOptionDTO.builder()
                .id(option.getId())
                .fieldDefinitionId(option.getFieldDefinitionId())
                .label(option.getLabel())
                .value(option.getValue())
                .orderIndex(option.getOrderIndex())
                .build();
    }

    // --- conditional rules ---------------------------------------------------------------------

    private void addDependency() {
        new DependencyDialog(null, session.fields(),
                dependency -> mutate(() -> session.addDependency(dependency), "Added the rule."))
                .open();
    }

    private void editDependency(FieldDependencyDTO dependency) {
        new DependencyDialog(dependency, session.fields(),
                edited -> mutate(() -> session.saveDependency(edited), "Saved the rule."))
                .open();
    }

    private void deleteDependency(FieldDependencyDTO dependency) {
        confirm("Delete this rule?",
                dependencySentence(dependency) + " will no longer apply.",
                "Delete",
                () -> mutate(() -> session.deleteDependency(dependency.getId()), "Deleted the rule."));
    }

    private String dependencySentence(FieldDependencyDTO dependency) {
        var byId = session.fieldsById();
        return DependencyText.sentence(dependency,
                labelOf(byId.get(dependency.getTriggerFieldId())),
                labelOf(byId.get(dependency.getDependentFieldId())));
    }

    private static String labelOf(FieldDefinitionDTO field) {
        if (field == null) {
            return "a deleted field";
        }
        return field.getLabel() == null ? field.getName() : field.getLabel();
    }

    // --- the one place anything is re-rendered -----------------------------------------------

    /**
     * Runs a write and puts the screen back in step with what is stored.
     *
     * <p>The refresh happens whether the write succeeded or not, so a rejected change never leaves
     * the editor showing something the database does not agree with.
     */
    private void mutate(Runnable write, String successMessage) {
        try {
            write.run();
            Notifications.success(successMessage);
        } catch (RuntimeException ex) {
            Notifications.error(ex.getMessage());
            session.reload();
        } finally {
            refreshFromModel();
        }
    }

    private void refreshFromModel() {
        var form = session.form();
        FormActions actions = session.actions();

        title.setValue(nullToEmpty(form.getTitle()));
        description.setValue(nullToEmpty(form.getDescription()));

        fieldList.setFields(session.fields(), actions.editable());
        dependencyList.setDependencies(session.dependencies(), session.fieldsById(), actions.editable());
        preview.refresh(form, session.dependencies());

        // One call, one pane: whatever is added to the editor from now on is covered by it, whether
        // or not whoever adds it remembers to ask. The panels above grey their own buttons on top of
        // this, because a control rendered inside a grid row is drawn from its own enabled flag.
        editorPane.setVisible(true);
        editorPane.setEnabled(actions.editable());

        banner.removeAll();
        banner.setVisible(!actions.editable());
        if (!actions.editable()) {
            banner.add(new Span(actions.reason()));
        }

        publish.setEnabled(actions.publishable());
        setTooltip(publish, actions.publishBlockedReason());
        newVersion.setEnabled(actions.versionable());
        setTooltip(newVersion, actions.versionable()
                ? null : "Only a published or archived form can be versioned.");
        delete.setEnabled(actions.deletable());
        setTooltip(delete, actions.deletable() ? null : actions.reason());
    }

    // --- helpers -----------------------------------------------------------------------------

    private static void setTooltip(Button button, String reason) {
        if (reason == null) {
            button.getElement().removeAttribute("title");
        } else {
            button.getElement().setAttribute("title", reason);
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

    private void navigateToIndex() {
        getUI().ifPresent(ui -> ui.navigate(FormBuilderIndexView.class));
    }

    private void navigateToForm(Long formId) {
        getUI().ifPresent(ui -> ui.navigate(FormBuilderView.class,
                new RouteParameters(FORM_ID, String.valueOf(formId))));
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
