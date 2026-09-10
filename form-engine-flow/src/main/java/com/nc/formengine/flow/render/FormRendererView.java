package com.nc.formengine.flow.render;

import com.nc.formengine.business.service.DependencyEvaluationService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.business.service.FormValidationService;
import com.nc.formengine.business.service.LayoutResolutionService;
import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.validation.FieldValidationError;
import com.nc.formengine.model.validation.ValidationReport;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.business.service.FormSubmissionWorkflowService;
import com.nc.formengine.submission.business.service.SubmissionResult;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.nc.formengine.flow.shared.FieldComponentFactory;
import com.nc.formengine.flow.shared.FieldEditor;
import com.nc.formengine.flow.shared.Notifications;
import com.nc.formengine.flow.shared.ViewToolbar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fills in a published form and sends it.
 *
 * <p>The engine already guarantees that a submission stored as {@link SubmissionStatus#SUBMITTED} is
 * one its own definition would accept: {@code FormSubmissionWorkflowService.submit} validates and
 * writes in a single transaction, and a rejected submit writes nothing at all. So this view does not
 * re-implement any rule. Its job is the other half of that bargain — that a rejection never leaves
 * the user without knowing which field to fix. Hence the error summary above the form, whose entries
 * jump to the field they are about, alongside the message on the field itself.
 *
 * <p>The same route serves a fresh form and a draft being resumed; the optional {@code submissionId}
 * is what tells them apart.
 */
@Route("fill/:formId/:submissionId?")
@PageTitle("Fill a form")
public class FormRendererView extends VerticalLayout implements BeforeEnterObserver {

    /** There is no authentication in this demo, so every submission is filed under one name. */
    static final String DEMO_USER = "demo";

    private final FormDefinitionService formService;
    private final LayoutResolutionService layoutService;
    private final DependencyEvaluationService dependencyService;
    private final FormValidationService validationService;
    private final FormSubmissionWorkflowService workflowService;
    private final FormSubmissionService submissionService;

    /** Keyed by field name, which is the key both the validator and the dependency engine speak. */
    private final Map<String, FieldEditor> editors = new LinkedHashMap<>();
    private final Div errorSummary = new Div();
    private final Button discardButton = new Button("Discard draft", event -> confirmDiscard());

    private FormDefinitionDTO form;
    private Long submissionId;

    /**
     * Guards against re-entering dependency evaluation. Applying a state clears the input of a field
     * that has just been hidden, which is itself a value change; without this, a chain of
     * dependencies would evaluate itself once per link on every keystroke's worth of change.
     */
    private boolean applyingDependencies;

    FormRendererView(FormDefinitionService formService,
                     LayoutResolutionService layoutService,
                     DependencyEvaluationService dependencyService,
                     FormValidationService validationService,
                     FormSubmissionWorkflowService workflowService,
                     FormSubmissionService submissionService) {
        this.formService = formService;
        this.layoutService = layoutService;
        this.dependencyService = dependencyService;
        this.validationService = validationService;
        this.workflowService = workflowService;
        this.submissionService = submissionService;

        discardButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        discardButton.setVisible(false);

        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        editors.clear();
        submissionId = null;

        Optional<Long> formId = event.getRouteParameters().get("formId")
                .flatMap(FormRendererView::parseId);
        if (formId.isEmpty()) {
            showProblem("That is not a form id.");
            return;
        }

        Optional<FormDefinitionDTO> found = formService.findById(formId.get());
        if (found.isEmpty()) {
            showProblem("There is no form with id " + formId.get() + ".");
            return;
        }

        form = found.get();
        if (form.getStatus() != FormDefinitionStatus.PUBLISHED) {
            // Saying why beats an empty page: the engine would refuse the submission anyway.
            showProblem("\"" + form.getTitle() + "\" is " + form.getStatus()
                    + ", and only a published form accepts answers.");
            return;
        }

        buildForm();
        event.getRouteParameters().get("submissionId")
                .flatMap(FormRendererView::parseId)
                .ifPresent(this::resumeDraft);
        showDiscardWhenThereIsADraft();

        // The draft may already carry answers that hide fields, so the first evaluation happens
        // before the user has touched anything.
        applyDependencies();
    }

    private void buildForm() {
        List<FieldEditor> ordered = new ArrayList<>();
        fieldsInOrder().forEach(field -> {
            FieldEditor editor = FieldComponentFactory.create(field);
            editors.put(field.getName(), editor);
            ordered.add(editor);
            FieldEditors.onValueChange(editor, this::applyDependencies);
            FieldEditors.onBlur(editor, () -> validateOne(editor));
        });

        errorSummary.setVisible(false);
        errorSummary.getStyle()
                .set("border", "1px solid var(--lumo-error-color-50pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "var(--lumo-error-color-10pct)")
                .set("padding", "var(--lumo-space-m)");

        var body = new VerticalLayout(errorSummary,
                LayoutGridRenderer.render(ordered, resolveLayout()));
        body.setPadding(true);

        add(new ViewToolbar(form.getTitle(), ViewToolbar.group(
                        discardButton,
                        new Button("Save draft", event -> saveDraft()),
                        submitButton())),
                body);
    }

    private Button submitButton() {
        var submit = new Button("Submit", event -> submit());
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return submit;
    }

    /**
     * Offered only once there is a stored draft to give up on.
     *
     * <p>Discarding belongs here, next to the form being filled in, because it is the respondent's own
     * decision about their own unsent draft. The responses view offers it too, but that is someone
     * looking at a list — the person who started the draft should not have to go find it there.
     */
    private void showDiscardWhenThereIsADraft() {
        discardButton.setVisible(submissionId != null);
    }

    private List<FieldDefinitionDTO> fieldsInOrder() {
        if (form.getFields() == null) {
            return List.of();
        }
        return form.getFields().stream()
                .filter(field -> field.getName() != null)
                .sorted(Comparator.comparing(FieldDefinitionDTO::getOrderIndex,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * The layout for this device, or null to let {@link LayoutGridRenderer} stack the fields.
     */
    private FormLayoutDTO resolveLayout() {
        return layoutService.resolveLayout(form.getId(), deviceType()).orElse(null);
    }

    private static DeviceType deviceType() {
        WebBrowser browser = VaadinSession.getCurrent() == null ? null : VaadinSession.getCurrent().getBrowser();
        if (browser == null) {
            return DeviceType.DESKTOP;
        }
        if (browser.isAndroid() || browser.isIPhone() || browser.isWindowsPhone()) {
            return DeviceType.MOBILE;
        }
        return DeviceType.DESKTOP;
    }

    // --- live behaviour -------------------------------------------------------------------------

    /**
     * Asks the engine what every field's state is now, and applies it.
     *
     * <p>Hooked to the value-change event and not to input, so a form with dependencies is evaluated
     * once per answer rather than once per keystroke. Hiding a field also clears it, which
     * {@link FieldEditor#applyState} does on its own, so an answer typed before a dependency hid the
     * field is never submitted behind the user's back.
     */
    private void applyDependencies() {
        if (applyingDependencies) {
            return;
        }
        applyingDependencies = true;
        try {
            Map<String, FieldState> states = dependencyService.evaluate(form.getId(), currentValues());
            states.forEach((name, state) -> {
                FieldEditor editor = editors.get(name);
                if (editor != null) {
                    editor.applyState(state);
                }
            });
        } finally {
            applyingDependencies = false;
        }
    }

    /** Validates one answer as the user leaves it, with the rest of the answers for context. */
    private void validateOne(FieldEditor editor) {
        ValidationReport report =
                validationService.validateField(editor.field().getId(), editor.value(), currentValues());
        editor.setErrors(report.errors());
    }

    /** Every answer as it stands, including the unanswered ones: absent and null mean the same thing. */
    private Map<String, Object> currentValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        editors.forEach((name, editor) -> values.put(name, editor.value()));
        return values;
    }

    // --- saving ---------------------------------------------------------------------------------

    private void saveDraft() {
        try {
            SubmissionResult result = workflowService.saveDraft(submission());
            submissionId = result.submission().getId();
            // Keep the URL pointing at the draft, so a reload resumes it instead of starting over.
            getUI().ifPresent(ui -> ui.getPage().getHistory()
                    .replaceState(null, "fill/" + form.getId() + "/" + submissionId));

            showErrors(result.report());
            // There is something to give up on now, which there was not before the first save.
            showDiscardWhenThereIsADraft();
            // A draft is saved whatever the report says: the errors are what is still missing, not a
            // refusal. Saying so is the difference between "saved" and "silently ignored".
            Notifications.success(result.report().valid()
                    ? "Draft saved."
                    : "Draft saved, with " + result.report().errors().size() + " still to fix.");
        } catch (RuntimeException ex) {
            Notifications.error("Could not save the draft: " + ex.getMessage());
        }
    }

    /** Asks first: the draft is kept but can never be worked on again. */
    private void confirmDiscard() {
        var dialog = new ConfirmDialog();
        dialog.setHeader("Discard this draft?");
        dialog.setText("The answers so far are kept, but the draft is never sent and cannot be "
                + "picked up again.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Discard draft");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> discard());
        dialog.open();
    }

    private void discard() {
        try {
            workflowService.discard(submissionId);
            Notifications.success("Draft discarded.");
            getUI().ifPresent(ui -> ui.navigate("forms"));
        } catch (RuntimeException ex) {
            // The workflow refuses what it does not allow; what it says is what the user needs.
            Notifications.error("Could not discard the draft: " + ex.getMessage());
        }
    }

    private void submit() {
        try {
            SubmissionResult result = workflowService.submit(submission());
            if (!result.persisted()) {
                showErrors(result.report());
                Notifications.error("Not sent: " + result.report().errors().size()
                        + " answer(s) need fixing.");
                return;
            }
            Notifications.success("Sent. Submission #" + result.submission().getId() + ".");
            getUI().ifPresent(ui -> ui.navigate("forms"));
        } catch (RuntimeException ex) {
            Notifications.error("Could not send the form: " + ex.getMessage());
        }
    }

    /**
     * The answers as the engine wants them: one row per field, carrying the field's id and its name.
     *
     * <p>Hidden fields are included as the empty answers they are. The engine skips them when
     * validating, and sending them keeps a resumed draft from inheriting a value the user can no
     * longer see.
     */
    private FormSubmissionDTO submission() {
        List<FieldSubmissionDTO> answers = new ArrayList<>();
        editors.forEach((name, editor) -> answers.add(FieldSubmissionDTO.builder()
                .fieldDefinitionId(editor.field().getId())
                .fieldName(name)
                .value(editor.value())
                .build()));

        return FormSubmissionDTO.builder()
                .id(submissionId)
                .formDefinitionId(form.getId())
                .formCode(form.getCode())
                .author(DEMO_USER)
                .fieldSubmissions(answers)
                .build();
    }

    private void resumeDraft(Long id) {
        Optional<FormSubmissionDTO> found = submissionService.findById(id);
        if (found.isEmpty()) {
            Notifications.error("There is no draft #" + id + "; starting a new one.");
            return;
        }

        FormSubmissionDTO draft = found.get();
        if (draft.getStatus() != SubmissionStatus.DRAFT) {
            Notifications.error("Submission #" + id + " is " + draft.getStatus()
                    + " and can no longer be edited; starting a new one.");
            return;
        }
        if (!form.getId().equals(draft.getFormDefinitionId())) {
            Notifications.error("Draft #" + id + " belongs to another form; starting a new one.");
            return;
        }

        submissionId = id;
        if (draft.getFieldSubmissions() != null) {
            draft.getFieldSubmissions().forEach(answer -> {
                FieldEditor editor = editors.get(answer.getFieldName());
                if (editor != null) {
                    editor.setValue(answer.getValue());
                }
            });
        }
    }

    // --- reporting errors -----------------------------------------------------------------------

    /**
     * Shows a report on the fields it is about, and as a list the user can walk.
     *
     * <p>The list is the point of this view. A message under an input is easy to miss on a long form,
     * and an error naming a field that has no editor — one the layout hid, or one that was removed
     * from the definition — would otherwise be invisible while still blocking the submission.
     */
    private void showErrors(ValidationReport report) {
        editors.values().forEach(editor -> editor.setErrors(report.errorsFor(editor.name())));

        errorSummary.removeAll();
        if (report.valid()) {
            errorSummary.setVisible(false);
            return;
        }

        var heading = new H3(report.errors().size() == 1
                ? "One answer needs fixing"
                : report.errors().size() + " answers need fixing");
        heading.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.Top.NONE);
        errorSummary.add(heading);

        report.errors().forEach(error -> errorSummary.add(errorEntry(error)));
        errorSummary.setVisible(true);
        errorSummary.scrollIntoView();
    }

    private Div errorEntry(FieldValidationError error) {
        FieldEditor editor = editors.get(error.fieldName());
        String label = editor != null ? editor.field().getLabel() : error.fieldName();

        var entry = new Div(new Span(label + " — " + error.message()));
        if (editor == null) {
            return entry;
        }

        entry.getStyle().set("cursor", "pointer").set("text-decoration", "underline");
        entry.addClickListener(event -> FieldEditors.focus(editor));
        return entry;
    }

    // --- dead ends ------------------------------------------------------------------------------

    private void showProblem(String message) {
        var explanation = new Span(message);
        var back = new Anchor("forms", "Back to the list of forms");

        var body = new VerticalLayout(explanation, back);
        body.setPadding(true);
        add(new ViewToolbar("Fill a form"), body);
    }

    private static Optional<Long> parseId(String raw) {
        try {
            return Optional.of(Long.valueOf(raw));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
