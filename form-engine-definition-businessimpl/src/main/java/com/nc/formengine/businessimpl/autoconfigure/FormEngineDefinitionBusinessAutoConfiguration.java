package com.nc.formengine.businessimpl.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Registers the definition services, so an application only has to depend on the jar.
 * Nothing here needs to appear in the application's own {@code @ComponentScan}.
 *
 * <p>An application that wants to supply its own implementations excludes this class with
 * {@code spring.autoconfigure.exclude}. There is no per-bean opt-out: Spring rejects a
 * configuration class that both component-scans and carries a bean-level condition.
 */
@AutoConfiguration
// Scanning rather than importing a list of classes: this package belongs entirely to this
// module, so there is nothing of the application's to accidentally pick up, and the services
// collaborate with package-private helpers an @Import could not name.
@ComponentScan(basePackages = "com.nc.formengine.businessimpl.service")
public class FormEngineDefinitionBusinessAutoConfiguration {
}
