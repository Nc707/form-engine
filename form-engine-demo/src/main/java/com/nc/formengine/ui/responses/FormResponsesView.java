package com.nc.formengine.ui.responses;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.model.dto.SubmissionFilter;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.nc.formengine.ui.shared.ViewToolbar;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Map;

/**
 * One form's submissions, with how many are in each state.
 *
 * <p>The form is a version, not a code: two versions of the same code have different fields, so
 * counting or exporting them together would mean columns that only some rows have.
 */
@Route("responses/form/:formId?")
@PageTitle("Responses by form")
@Menu(order = 3, icon = "vaadin:clipboard-text", title = "Responses by form")
class FormResponsesView extends VerticalLayout implements BeforeEnterObserver {

    static final String FORM_ID = "formId";

    private final FormSubmissionService submissionService;
    private final FormDefinitionService formDefinitionService;
    private final FieldDefinitionService fieldDefinitionService;
    private final AnswerResolver answerResolver;

    private final ComboBox<FormDefinitionDTO> formPicker = new ComboBox<>();
    private final HorizontalLayout summary = new HorizontalLayout();
    private final VerticalLayout body = new VerticalLayout();

    FormResponsesView(FormSubmissionService submissionService,
                      FormDefinitionService formDefinitionService,
                      FieldDefinitionService fieldDefinitionService,
                      AnswerResolver answerResolver) {
        this.submissionService = submissionService;
        this.formDefinitionService = formDefinitionService;
        this.fieldDefinitionService = fieldDefinitionService;
        this.answerResolver = answerResolver;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        formPicker.setPlaceholder("Pick a form");
        formPicker.setWidth("22em");
        formPicker.setItems(formDefinitionService.findAll());
        formPicker.setItemLabelGenerator(SubmissionBrowser::describe);
        formPicker.addValueChangeListener(event -> {
            if (event.isFromClient() && event.getValue() != null) {
                getUI().ifPresent(ui -> ui.navigate(FormResponsesView.class,
                        new RouteParameters(FORM_ID, String.valueOf(event.getValue().getId()))));
            }
        });

        summary.setSpacing(true);
        summary.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);

        body.setSizeFull();
        body.setPadding(false);
        body.setSpacing(false);

        add(new ViewToolbar("Por formulario", formPicker), summary, body);
        setFlexGrow(1, body);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        body.removeAll();
        summary.removeAll();

        Long formId = event.getRouteParameters().get(FORM_ID).map(Long::valueOf).orElse(null);
        if (formId == null) {
            body.add(hint("Pick a form to see its submissions."));
            return;
        }

        FormDefinitionDTO form = formDefinitionService.findById(formId).orElse(null);
        if (form == null) {
            body.add(hint("No form exists with id " + formId + "."));
            return;
        }

        formPicker.setValue(form);

        var browser = new SubmissionBrowser(
                submissionService, formDefinitionService, fieldDefinitionService, answerResolver);
        browser.onFilterChange(this::showSummary);
        browser.lockToForm(form);

        body.add(browser);
        body.setFlexGrow(1, browser);
    }

    /**
     * Recomputed on every filter change, so the counts always describe what the grid is showing —
     * except for the status filter, which is dropped: a breakdown by status that only counts one
     * status would just be that status and two zeroes.
     */
    private void showSummary(SubmissionFilter filter) {
        summary.removeAll();
        Map<SubmissionStatus, Long> counts = submissionService.countByStatus(
                new SubmissionFilter(filter.formDefinitionId(), null, filter.submittedBy()));
        for (SubmissionStatus status : SubmissionStatus.values()) {
            var badge = SubmissionBrowser.statusBadge(status);
            badge.setText(status.name() + ": " + counts.getOrDefault(status, 0L));
            summary.add(badge);
        }
    }

    private Span hint(String message) {
        var span = new Span(message);
        span.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.TextColor.SECONDARY);
        return span;
    }
}
