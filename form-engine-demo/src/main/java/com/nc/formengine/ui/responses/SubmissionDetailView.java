package com.nc.formengine.ui.responses;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.business.service.FormSubmissionWorkflowService;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.nc.formengine.ui.shared.Notifications;
import com.nc.formengine.ui.shared.ViewToolbar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * One submission, read back.
 *
 * <p>The answers are labelled from the definition the submission was filled in against — which may
 * well be an archived one — so what is on screen is what the person filling the form actually saw,
 * not what the form asks today.
 */
@Route("responses/submission/:submissionId([0-9]+)")
@PageTitle("Submission detail")
class SubmissionDetailView extends VerticalLayout implements BeforeEnterObserver {

    static final String SUBMISSION_ID = "submissionId";

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FormSubmissionService submissionService;
    private final FormSubmissionWorkflowService workflowService;
    private final FormDefinitionService formDefinitionService;
    private final AnswerResolver answerResolver;

    SubmissionDetailView(FormSubmissionService submissionService,
                         FormSubmissionWorkflowService workflowService,
                         FormDefinitionService formDefinitionService,
                         AnswerResolver answerResolver) {
        this.submissionService = submissionService;
        this.workflowService = workflowService;
        this.formDefinitionService = formDefinitionService;
        this.answerResolver = answerResolver;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Long id = event.getRouteParameters().get(SUBMISSION_ID).map(Long::valueOf).orElse(null);
        FormSubmissionDTO submission = id == null ? null : submissionService.findById(id).orElse(null);

        if (submission == null) {
            event.rerouteToError(NotFoundException.class);
            return;
        }
        show(submission);
    }

    private void show(FormSubmissionDTO submission) {
        removeAll();

        var cancelButton = new Button("Cancel submission", click -> confirmCancel(submission));
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        // CANCELED is terminal, so there is nothing this button could do from there.
        cancelButton.setEnabled(submission.getStatus() != SubmissionStatus.CANCELED);

        add(new ViewToolbar("Submission detail", cancelButton),
                metadata(submission),
                answers(submission));
    }

    private FormLayout metadata(FormSubmissionDTO submission) {
        FormDefinitionDTO form = submission.getFormDefinitionId() == null
                ? null
                : formDefinitionService.findById(submission.getFormDefinitionId()).orElse(null);

        var layout = new FormLayout();
        layout.addClassNames(LumoUtility.Padding.MEDIUM);
        layout.addFormItem(new Span(SubmissionBrowser.describe(form)), "Form");
        layout.addFormItem(new Span(orDash(submission.getFormCode())), "Code");
        layout.addFormItem(new Span(orDash(submission.getSubmittedBy())), "Author");
        layout.addFormItem(new Span(submission.getStatus() == SubmissionStatus.DRAFT
                        || submission.getSubmittedAt() == null
                        ? "—"
                        : submission.getSubmittedAt().format(TIMESTAMP)),
                "Submitted");
        layout.addFormItem(SubmissionBrowser.statusBadge(submission.getStatus()), "Status");

        if (form != null && form.getStatus() == FormDefinitionStatus.ARCHIVED) {
            layout.addFormItem(
                    note("This answer belongs to an archived version. The labels below are "
                            + "las de esa versión, no las de la versión publicada hoy."),
                    "");
        }
        return layout;
    }

    private VerticalLayout answers(FormSubmissionDTO submission) {
        List<FieldDefinitionDTO> fields = answerResolver.definitionOf(submission);
        List<ResolvedAnswer> resolved = answerResolver.resolve(submission, fields);

        var layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(false);

        if (fields.isEmpty() && !resolved.isEmpty()) {
            layout.add(note("This form's definition no longer exists, so no field could be "
                    + "resolved. What the submission stored is shown instead."));
        }
        if (resolved.isEmpty()) {
            layout.add(note("This submission has no answers."));
            return layout;
        }

        var answers = new FormLayout();
        answers.setWidthFull();
        for (ResolvedAnswer answer : resolved) {
            answers.addFormItem(value(answer), answer.label());
        }
        layout.add(answers);
        return layout;
    }

    /**
     * The "retired" mark rides with the value, not the label: the label column of a
     * {@link FormLayout} is narrow, and a badge next to the text there squeezes it until it wraps
     * one character per line.
     */
    private com.vaadin.flow.component.Component value(ResolvedAnswer answer) {
        Span text;
        if (!answer.answered()) {
            text = new Span("(no answer)");
            text.addClassNames(LumoUtility.TextColor.SECONDARY);
        } else {
            text = new Span(answer.displayValue());
        }

        if (!answer.retired()) {
            return text;
        }

        var retired = new Span("campo retirado");
        retired.getElement().setAttribute("theme", "badge contrast small");

        var withMark = new HorizontalLayout(text, retired);
        withMark.setSpacing(true);
        withMark.setPadding(false);
        withMark.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        return withMark;
    }

    /** Asks before doing it, in a dialog the application owns rather than a browser prompt. */
    private void confirmCancel(FormSubmissionDTO submission) {
        var dialog = new Dialog();
        dialog.setHeaderTitle("Cancel this submission?");
        dialog.add(new Span("The submission by " + orDash(submission.getSubmittedBy())
                + " will be canceled. That is final: there is no way back."));

        var confirm = new Button("Cancel submission", click -> {
            dialog.close();
            cancel(submission);
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        var dismiss = new Button("Back", click -> dialog.close());
        dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(dismiss, confirm);
        dialog.open();
    }

    private void cancel(FormSubmissionDTO submission) {
        try {
            FormSubmissionDTO canceled = workflowService.cancel(submission.getId());
            Notifications.success("Submission canceled");
            show(canceled);
        } catch (RuntimeException ex) {
            // The workflow refuses transitions it does not allow; what it says is what the user needs.
            Notifications.error(ex.getMessage());
        }
    }

    private Span note(String message) {
        var span = new Span(message);
        span.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        return span;
    }

    private String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
