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
 * That the paths are the application's to choose.
 *
 * <p>The prefix here nests two segments deep and renames one view, which is the case that would
 * break if anything went back to reading the {@code @Route} annotations: those are compile-time
 * constants and cannot say this.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "vaadin.productionMode=true",
                "formengine.flow.routes.prefix=admin/forms",
                "formengine.flow.routes.forms=fill-in"
        })
class ConfiguredRoutePrefixTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void mountsTheViewsWhereTheApplicationAsked() {
        assertThat(registeredTemplates())
                .contains("admin/forms/definitions", "admin/forms/fill-in")
                .doesNotContain("form-engine/definitions", "form-engine/forms");
    }

    private java.util.List<String> registeredTemplates() {
        var registry = ApplicationRouteRegistry.getInstance(
                new VaadinServletContext(context.getServletContext()));
        return registry.getRegisteredRoutes().stream().map(RouteData::getTemplate).toList();
    }
}
