package com.nc.formengine.flow.responses;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.flow.shared.ViewToolbar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** Every submission the engine has stored, across all forms and versions. */
// The value is only the default; the path actually used comes from
// formengine.flow.routes and is registered by FormEngineRouteRegistrar, which is also
// why this must not register itself at startup.
@Route(value = "form-engine/responses", registerAtStartup = false)
@PageTitle("Responses")
public class SubmissionListView extends VerticalLayout {

    SubmissionListView(FormSubmissionService submissionService,
                       FormDefinitionService formDefinitionService,
                       FieldDefinitionService fieldDefinitionService,
                       AnswerResolver answerResolver) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var browser = new SubmissionBrowser(
                submissionService, formDefinitionService, fieldDefinitionService, answerResolver);

        add(new ViewToolbar("Responses"), browser);
        setFlexGrow(1, browser);
    }
}
