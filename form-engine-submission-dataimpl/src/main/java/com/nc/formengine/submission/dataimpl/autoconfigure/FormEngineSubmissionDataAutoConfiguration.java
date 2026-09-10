package com.nc.formengine.submission.dataimpl.autoconfigure;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Registers this module's persistence beans, so an application only has to depend on the
 * jar. Nothing here needs to appear in the application's own {@code @ComponentScan},
 * {@code @EntityScan} or {@code @EnableJpaRepositories}.
 */
@AutoConfiguration(before = DataJpaRepositoriesAutoConfiguration.class)
@ConditionalOnClass(JpaRepository.class)
@Import(FormEngineSubmissionDataAutoConfiguration.PersistencePackages.class)
// Scanning rather than importing a list of classes: these two packages belong entirely to
// this module, so there is nothing of the application's to accidentally pick up, and the
// DAO implementations include package-private collaborators an @Import could not name.
@ComponentScan(basePackages = {
        "com.nc.formengine.submission.dataimpl.daoimpl",
        "com.nc.formengine.submission.dataimpl.mapper" })
public class FormEngineSubmissionDataAutoConfiguration {

    /**
     * Contributes this module's package to the auto-configuration packages, which is where
     * Boot looks for entities and Spring Data looks for repositories. Adding to that list is
     * additive and de-duplicating, so it leaves whatever the application contributed alone.
     * <p>
     * Declaring {@code @EntityScan} or {@code @EnableJpaRepositories} here instead would be
     * actively harmful to whoever depends on this jar: both replace the mechanism the
     * application's own entities and repositories are found by, so ours would be registered
     * and theirs would silently disappear.
     * <p>
     * The package registered is the module root rather than the {@code entity} and
     * {@code repository} packages under it: de-duplication is by exact name, so contributing a
     * package that something else already covers through a parent would scan it twice and fail
     * on the duplicate bean definitions.
     */
    static class PersistencePackages implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, "com.nc.formengine.submission.dataimpl");
        }
    }
}
