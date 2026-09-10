package com.nc.formengine.flow.view.responses;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.flow.autoconfigure.FormEngineRouteRegistrar;
import com.nc.formengine.flow.components.common.ViewToolbar;
import com.nc.formengine.flow.components.responses.SubmissionBrowser;
import com.nc.formengine.flow.utils.Notifications;
import com.nc.formengine.flow.utils.responses.AnswerResolver;
import com.nc.formengine.flow.utils.responses.ResolvedAnswer;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.business.service.FormSubmissionWorkflowService;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/**
 * One submission, read back.
 *
 * <p>The answers are labelled from the definition the submission was filled in against — which may
 * well be an archived one — so what is on screen is what the person filling the form actually saw,
 * not what the form asks today.
 */
// The value is only the default; the path actually used comes from
// formengine.flow.routes and is registered by FormEngineRouteRegistrar, which is also
// why this must not register itself at startup.
@Route(value = "form-engine/responses/submission/:submissionId([0-9]+)", registerAtStartup = false)
@PageTitle("Submission detail")
public class SubmissionDetailView extends VerticalLayout implements BeforeEnterObserver {

    public static final String SUBMISSION_ID = "submissionId";

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

        // Which ending applies follows from where the submission is, so the view offers one or none
        // rather than one button that means different things.
        Button ending = endingButton(submission);
        add(ending == null
                        ? new ViewToolbar("Submission detail")
                        : new ViewToolbar("Submission detail", ending),
                metadata(submission),
                answers(submission));
    }

    /**
     * The action available from this submission's state, or null when it has already ended.
     *
     * <p>A draft can only be discarded, by whoever was filling it in. A submitted response can only be
     * voided, by whoever owns the form. Neither is the other, and offering both would ask the user to
     * pick which of two meanings they meant.
     */
    private Button endingButton(FormSubmissionDTO submission) {
        return switch (submission.getStatus()) {
            case DRAFT -> endingButton("Discard draft",
                    "Discard this draft?",
                    "The draft by " + orDash(submission.getAuthor())
                            + " will be discarded. Its answers are kept, but it is never sent.",
                    () -> workflowService.discard(submission.getId()),
                    "Draft discarded");
            case SUBMITTED -> endingButton("Void response",
                    "Void this response?",
                    "The response by " + orDash(submission.getAuthor())
                            + " will stop counting. Its answers stay readable, and this is final.",
                    () -> workflowService.voidSubmission(submission.getId()),
                    "Response voided");
            case DISCARDED, VOIDED -> null;
        };
    }

    private FormLayout metadata(FormSubmissionDTO submission) {
        FormDefinitionDTO form = submission.getFormDefinitionId() == null
                ? null
                : formDefinitionService.findById(submission.getFormDefinitionId()).orElse(null);

        var layout = new FormLayout();
        layout.addClassNames(LumoUtility.Padding.MEDIUM);
        layout.addFormItem(new Span(SubmissionBrowser.describe(form)), "Form");
        layout.addFormItem(new Span(orDash(submission.getFormCode())), "Code");
        layout.addFormItem(new Span(orDash(submission.getAuthor())), "Author");
        layout.addFormItem(new Span(timestamp(submission.getCreatedAt())), "Started");
        // Null when it was never sent, which is now something the column can say for itself.
        layout.addFormItem(new Span(timestamp(submission.getSubmittedAt())), "Submitted");
        layout.addFormItem(SubmissionBrowser.statusBadge(submission.getStatus()), "Status");

        if (form != null && form.getStatus() == FormDefinitionStatus.ARCHIVED) {
            layout.addFormItem(
                    note("This answer belongs to an archived version. The labels below are that "
                            + "version's, not those of the version published today."),
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

        var retired = new Span("retired field");
        retired.getElement().setAttribute("theme", "badge contrast small");

        var withMark = new HorizontalLayout(text, retired);
        withMark.setSpacing(true);
        withMark.setPadding(false);
        withMark.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        return withMark;
    }

    private Button endingButton(String label, String question, String consequence,
                                Supplier<FormSubmissionDTO> action, String done) {
        var button = new Button(label, click -> confirm(label, question, consequence, action, done));
        button.addThemeVariants(ButtonVariant.LUMO_ERROR);
        return button;
    }

    /** Asks before doing it, in a dialog the application owns rather than a browser prompt. */
    private void confirm(String label, String question, String consequence,
                         Supplier<FormSubmissionDTO> action, String done) {
        var dialog = new Dialog();
        dialog.setHeaderTitle(question);
        dialog.add(new Span(consequence));

        var proceed = new Button(label, click -> {
            dialog.close();
            apply(action, done);
        });
        proceed.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        var dismiss = new Button("Back", click -> dialog.close());
        dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(dismiss, proceed);
        dialog.open();
    }

    private void apply(Supplier<FormSubmissionDTO> action, String done) {
        try {
            FormSubmissionDTO updated = action.get();
            Notifications.success(done);
            show(updated);
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

    private String timestamp(LocalDateTime moment) {
        return moment == null ? "—" : moment.format(TIMESTAMP);
    }
}
