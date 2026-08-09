package com.nc.formengine.dataimpl;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Spring configuration the persistence tests boot against.
 *
 * <p>This module is a library and has no application class of its own, so the slice needs one here:
 * it points at the entities and repositories the same way the applications that use the module do.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.nc.formengine.dataimpl.entity")
@EnableJpaRepositories(basePackages = "com.nc.formengine.dataimpl.repository")
public class PersistenceTestConfiguration {

    /**
     * The JPA slice does not configure Jackson, and the mappers that store JSON-valued parameters
     * need it.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }
}
