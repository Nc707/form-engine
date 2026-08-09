package com.nc.formengine.submission.dataimpl;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * The Spring configuration the submission persistence tests boot against.
 *
 * <p>This module is a library and has no application class of its own, so the slice needs one here:
 * it points at the entities and repositories the same way the applications that use the module do.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.nc.formengine.submission.dataimpl.entity")
@EnableJpaRepositories(basePackages = "com.nc.formengine.submission.dataimpl.repository")
public class PersistenceTestConfiguration {
}
