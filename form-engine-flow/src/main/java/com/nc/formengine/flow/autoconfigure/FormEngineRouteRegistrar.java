package com.nc.formengine.flow.autoconfigure;

import com.nc.formengine.flow.view.builder.FormBuilderIndexView;
import com.nc.formengine.flow.view.builder.FormBuilderView;
import com.nc.formengine.flow.view.render.FormIndexView;
import com.nc.formengine.flow.view.render.FormRendererView;
import com.nc.formengine.flow.view.responses.FormResponsesView;
import com.nc.formengine.flow.view.responses.SubmissionDetailView;
import com.nc.formengine.flow.view.responses.SubmissionListView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mounts the views on the paths the application asked for.
 *
 * <p>The views carry a {@code @Route} whose value is only the default, marked {@code
 * registerAtStartup = false} so Vaadin never registers it. This does, at the path built from {@link
 * FormEngineFlowProperties.Routes} — which is what makes the paths configurable at all, since an
 * annotation value has to be a compile-time constant.
 *
 * <p>The annotation still has to be there, and not for documentation: Vaadin only applies the
 * application's {@code @Layout} to a route whose target is annotated {@code @Route}. Registering
 * with no parent chain and leaving the annotation in place is what gets the views wrapped in
 * whatever shell the application has, with nothing asked of the application.
 */
public class FormEngineRouteRegistrar implements VaadinServiceInitListener {

    private final FormEngineFlowProperties.Routes routes;

    FormEngineRouteRegistrar(FormEngineFlowProperties properties) {
        this.routes = properties.getRoutes();
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        RouteConfiguration configuration = RouteConfiguration.forRegistry(
                ApplicationRouteRegistry.getInstance(event.getSource().getContext()));

        register(configuration, FormBuilderIndexView.class, routes.getDefinitions());
        register(configuration, FormBuilderView.class, routes.getBuilder(), ":formId?");
        register(configuration, FormIndexView.class, routes.getForms());
        register(configuration, FormRendererView.class, routes.getFill(), ":formId", ":submissionId?");
        register(configuration, SubmissionListView.class, routes.getResponses());
        register(configuration, FormResponsesView.class, routes.getResponses(), "form", ":formId?");
        register(configuration, SubmissionDetailView.class,
                routes.getResponses(), "submission", ":submissionId([0-9]+)");
    }

    private void register(RouteConfiguration configuration, Class<? extends Component> view,
                          String... segments) {
        // serviceInit fires once per service, but a registration that ran twice would throw rather
        // than be ignored, so this stays cheap to be sure.
        if (configuration.isRouteRegistered(view)) {
            return;
        }
        configuration.setRoute(template(segments), view);
    }

    /** Joins the prefix and the segments, dropping any that are empty so a blank prefix mounts at the root. */
    private String template(String... segments) {
        return Stream.concat(Stream.of(routes.getPrefix()), Arrays.stream(segments))
                .filter(segment -> segment != null && !segment.isBlank())
                .map(segment -> segment.replaceAll("^/+|/+$", ""))
                .filter(segment -> !segment.isEmpty())
                .collect(Collectors.joining("/"));
    }
}
