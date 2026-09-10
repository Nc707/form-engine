package com.nc.formengine.dataimpl;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Spring configuration the persistence tests boot against.
 *
 * <p>This module is a library and has no application class of its own, so the slice needs one here.
 * It deliberately says nothing about where the entities and repositories are: the module's own
 * auto-configuration contributes those packages, and these tests are what proves it does.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
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
