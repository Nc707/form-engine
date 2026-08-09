package com.nc.formengine.ui.responses;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.ui.shared.ViewToolbar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** Every submission the engine has stored, across all forms and versions. */
@Route("responses")
@PageTitle("Respuestas")
@Menu(order = 1, icon = "vaadin:records", title = "Respuestas")
class SubmissionListView extends VerticalLayout {

    SubmissionListView(FormSubmissionService submissionService,
                       FormDefinitionService formDefinitionService,
                       FieldDefinitionService fieldDefinitionService,
                       AnswerResolver answerResolver) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var browser = new SubmissionBrowser(
                submissionService, formDefinitionService, fieldDefinitionService, answerResolver);

        add(new ViewToolbar("Respuestas"), browser);
        setFlexGrow(1, browser);
    }
}
