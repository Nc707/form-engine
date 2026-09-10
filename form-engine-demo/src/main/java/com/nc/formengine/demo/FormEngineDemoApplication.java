package com.nc.formengine.demo;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A sample application built on {@code form-engine-flow}.
 *
 * <p>What is worth reading here is how little there is. It names no package of the engine and none
 * of the library: the modules it depends on auto-configure their own beans, entities and
 * repositories, and the views mount themselves on the paths {@code formengine.flow.routes}
 * describes. Everything else in this package is the application’s own: the shell, the sample
 * data, and who submissions are attributed to.
 *
 * <p>The one thing a library cannot supply is this class: Vaadin allows exactly one
 * {@link AppShellConfigurator} per application, so the stylesheets are declared by whoever owns the
 * application and not by a jar.
 */
@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
public class FormEngineDemoApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(FormEngineDemoApplication.class, args);
    }
}
