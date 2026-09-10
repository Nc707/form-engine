package com.nc.formengine.flow;

import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * That adding the jar is enough.
 *
 * <p>{@link FormEngineFlowTestApplication} configures nothing at all, so if this passes, so does
 * the claim the README makes to anyone thinking of depending on this module.
 *
 * <p>A real servlet environment is required, not a mock one: Vaadin registers routes from a servlet
 * context initialiser, which a MOCK context never runs, leaving the registry empty for reasons that
 * have nothing to do with whether this works.
 */
// Production mode, and not because anything here is production: the dev-mode listener wants a
// frontend build, and a library jar has none. Nothing is ever requested from this server, so no
// bundle is needed either.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "vaadin.productionMode=true")
class RouteRegistrationTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void mountsEveryViewWithNoConfigurationFromTheApplication() {
        assertThat(registeredTemplates()).contains(
                "form-engine/definitions",
                "form-engine/builder/:formId?",
                "form-engine/forms",
                "form-engine/fill/:formId/:submissionId?",
                "form-engine/responses",
                "form-engine/responses/form/:formId?",
                "form-engine/responses/submission/:submissionId([0-9]+)");
    }

    private java.util.List<String> registeredTemplates() {
        var registry = ApplicationRouteRegistry.getInstance(
                new VaadinServletContext(context.getServletContext()));
        return registry.getRegisteredRoutes().stream().map(RouteData::getTemplate).toList();
    }
}
