package com.nc.formengine.demo;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.spring.annotation.EnableVaadin;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Vaadin front end for the form engine.
 *
 * <p>This application consumes the same business layer as {@code form-engine-rest}: it injects the
 * {@code *Service} interfaces from the {@code -business} modules and lets Spring wire the
 * {@code -businessimpl} beans. No engine logic is duplicated here.
 */
@SpringBootApplication(scanBasePackages = {
        "com.nc.formengine.demo",
        "com.nc.formengine.ui",
        "com.nc.formengine.businessimpl",
        "com.nc.formengine.dataimpl",
        "com.nc.formengine.submission.businessimpl",
        "com.nc.formengine.submission.dataimpl"
})
@EnableJpaRepositories(basePackages = {
        "com.nc.formengine.dataimpl.repository",
        "com.nc.formengine.submission.dataimpl.repository"
})
@EntityScan(basePackages = {
        "com.nc.formengine.dataimpl.entity",
        "com.nc.formengine.submission.dataimpl.entity"
})
// Component scanning is not enough to find a @Route: under Spring Boot, Vaadin looks for routes and
// layouts only in the package of this class, and every view lives in com.nc.formengine.ui. Without
// this the application starts perfectly well and serves "No views found" for every URL.
@EnableVaadin({"com.nc.formengine.demo", "com.nc.formengine.ui"})
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
public class FormEngineDemoApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(FormEngineDemoApplication.class, args);
    }
}
