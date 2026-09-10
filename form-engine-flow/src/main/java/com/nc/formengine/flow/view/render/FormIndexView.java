package com.nc.formengine.flow.view.render;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.flow.autoconfigure.FormEngineRouteRegistrar;
import com.nc.formengine.flow.components.common.ViewToolbar;
import com.nc.formengine.flow.spi.SubmissionAuthorProvider;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The way in to filling a form: the forms that are open for answers, and the drafts already started.
 *
 * <p>Only {@link FormDefinitionStatus#PUBLISHED} definitions are listed, because they are the only
 * ones the engine accepts submissions for — {@code FormSubmissionWorkflowService.submit} refuses
 * anything else. Offering a draft here would be offering a form that cannot be sent.
 */
// The value is only the default; the path actually used comes from
// formengine.flow.routes and is registered by FormEngineRouteRegistrar, which is also
// why this must not register itself at startup.
@Route(value = "form-engine/forms", registerAtStartup = false)
@PageTitle("Fill a form")
public class FormIndexView extends VerticalLayout {

    private final FormDefinitionService formService;
    private final FormSubmissionService submissionService;
    private final SubmissionAuthorProvider authorProvider;

    FormIndexView(FormDefinitionService formService,
                  FormSubmissionService submissionService,
                  SubmissionAuthorProvider authorProvider) {
        this.formService = formService;
        this.submissionService = submissionService;
        this.authorProvider = authorProvider;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(new ViewToolbar("Fill a form"), content());
    }

    private VerticalLayout content() {
        var content = new VerticalLayout();
        content.setPadding(true);
        content.add(new H3("Published forms"), publishedForms());

        List<FormSubmissionDTO> drafts = myDrafts();
        content.add(new H3("My drafts"));
        content.add(drafts.isEmpty() ? emptyDraftsHint() : draftsGrid(drafts));
        return content;
    }

    private Grid<FormDefinitionDTO> publishedForms() {
        var grid = new Grid<FormDefinitionDTO>();
        grid.addColumn(FormDefinitionDTO::getTitle).setHeader("Title").setAutoWidth(true);
        grid.addColumn(FormDefinitionDTO::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(FormDefinitionDTO::getVersion).setHeader("Version").setAutoWidth(true);
        grid.addColumn(form -> form.getFields() == null ? 0 : form.getFields().size())
                .setHeader("Fields").setAutoWidth(true);
        grid.addComponentColumn(form -> {
            var fill = new Button("Fill", event -> getUI().ifPresent(ui -> ui.navigate(
                    FormRendererView.class,
                    new RouteParameters(FormRendererView.FORM_ID, String.valueOf(form.getId())))));
            fill.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            return fill;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);

        grid.setItems(formService.findByStatus(FormDefinitionStatus.PUBLISHED));
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("No form has been published yet.");
        return grid;
    }

    private Grid<FormSubmissionDTO> draftsGrid(List<FormSubmissionDTO> drafts) {
        Map<Long, String> titles = titlesOf(drafts);

        var grid = new Grid<FormSubmissionDTO>();
        grid.addColumn(draft -> titles.getOrDefault(draft.getFormDefinitionId(), draft.getFormCode()))
                .setHeader("Form").setAutoWidth(true);
        // "Started", not "last saved": the entity stamps this on insert and a later save leaves it be.
        grid.addColumn(FormSubmissionDTO::getSubmittedAt).setHeader("Started").setAutoWidth(true);
        grid.addColumn(draft -> draft.getFieldSubmissions() == null ? 0 : draft.getFieldSubmissions().size())
                .setHeader("Answers").setAutoWidth(true);
        grid.addComponentColumn(draft -> {
            var resume = new Button("Resume", event -> getUI().ifPresent(ui -> ui.navigate(
                    FormRendererView.class,
                    new RouteParameters(Map.of(
                            FormRendererView.FORM_ID, String.valueOf(draft.getFormDefinitionId()),
                            FormRendererView.SUBMISSION_ID, String.valueOf(draft.getId()))))));
            resume.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return resume;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);

        grid.setItems(drafts);
        grid.setAllRowsVisible(true);
        return grid;
    }

    /**
     * The drafts of whoever is looking. Both this and the renderer that writes them go through the
     * same {@link SubmissionAuthorProvider}, so the two cannot drift apart no matter what the
     * application answers.
     */
    private List<FormSubmissionDTO> myDrafts() {
        return submissionService.findByAuthor(authorProvider.currentAuthor()).stream()
                .filter(submission -> submission.getStatus() == SubmissionStatus.DRAFT)
                .toList();
    }

    /** One lookup for the whole page rather than one per row. */
    private Map<Long, String> titlesOf(List<FormSubmissionDTO> drafts) {
        return drafts.stream()
                .map(FormSubmissionDTO::getFormDefinitionId)
                .distinct()
                .map(formService::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(FormDefinitionDTO::getId, FormDefinitionDTO::getTitle,
                        (first, second) -> first));
    }

    private Span emptyDraftsHint() {
        var hint = new Span("Nothing saved yet. A form you start filling in shows up here.");
        hint.addClassNames(LumoUtility.TextColor.SECONDARY);
        return hint;
    }
}
