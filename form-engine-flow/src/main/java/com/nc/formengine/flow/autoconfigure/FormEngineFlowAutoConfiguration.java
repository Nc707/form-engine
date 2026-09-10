package com.nc.formengine.flow.autoconfigure;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.flow.responses.AnswerResolver;
import com.nc.formengine.flow.spi.SubmissionAuthorProvider;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Everything an application gets by depending on this jar and nothing else.
 *
 * <p>The views are not beans and are not registered here: Vaadin instantiates a route target
 * through the autowire-capable bean factory, so their constructor injection works whether or not
 * the application scans this package. What is registered is the listener that mounts them, and the
 * two beans they need.
 */
@AutoConfiguration
@ConditionalOnClass(Component.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(FormEngineFlowProperties.class)
public class FormEngineFlowAutoConfiguration {

    /**
     * Declared here rather than annotated {@code @Component} so it is a bean of this module
     * regardless of what the application scans, and so an application can replace it.
     */
    @Bean
    @ConditionalOnMissingBean
    public AnswerResolver formEngineAnswerResolver(FieldDefinitionService fieldService) {
        return new AnswerResolver(fieldService);
    }

    /**
     * A single answer for everyone, which is the honest default for a module that knows nothing
     * about how the application authenticates. Guessing at Spring Security here instead would
     * write whatever it guessed into every submission.
     */
    @Bean
    @ConditionalOnMissingBean
    public SubmissionAuthorProvider formEngineSubmissionAuthorProvider(FormEngineFlowProperties properties) {
        return properties::getDefaultAuthor;
    }

    /**
     * Setting {@code formengine.flow.routes.enabled} to false leaves the beans and the components
     * available while mounting no routes at all. That is the whole opt-out: because the views are
     * registered here rather than found by a scan, not registering them is enough — there is
     * nothing to undo.
     */
    @Bean
    @ConditionalOnBooleanProperty(name = "formengine.flow.routes.enabled", matchIfMissing = true)
    public VaadinServiceInitListener formEngineRouteRegistrar(FormEngineFlowProperties properties) {
        return new FormEngineRouteRegistrar(properties);
    }
}
