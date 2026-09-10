package com.nc.formengine.demo;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.spring.annotation.EnableVaadin;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Vaadin front end for the form engine.
 *
 * <p>This application consumes the same business layer as {@code form-engine-rest}: it injects the
 * {@code *Service} interfaces from the {@code -business} modules and lets Spring wire the
 * {@code -businessimpl} beans. No engine logic is duplicated here.
 *
 * <p>It names none of the engine's packages either: every module it depends on auto-configures its
 * own beans, entities and repositories. The one package it does name is the views', which are not
 * beans of this application but classes Spring has to find to instantiate.
 */
@SpringBootApplication(scanBasePackages = {"com.nc.formengine.demo", "com.nc.formengine.flow"})
/**
 * Where Vaadin looks for {@code @Route} classes.
 *
 * <p>Component scanning is not enough. Without this, Vaadin scans the auto-configuration package —
 * the package of this class, {@code com.nc.formengine.demo} — and every view lives in
 * {@code com.nc.formengine.flow}, so it finds none of them: the application starts perfectly well and
 * serves "No views found" for every URL. The {@code vaadin.allowed-packages} property does not
 * cover this; it filters what the frontend resource scan reads, it does not widen the set of
 * packages the route scan starts from.
 *
 * <p>Vaadin's own packages have to stay on the list, since overriding this replaces the default set
 * rather than adding to it.
 */
@EnableVaadin({"com.vaadin", "org.vaadin", "com.nc.formengine"})
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
public class FormEngineDemoApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(FormEngineDemoApplication.class, args);
    }
}
