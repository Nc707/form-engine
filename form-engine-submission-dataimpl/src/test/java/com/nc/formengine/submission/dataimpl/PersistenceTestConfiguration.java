package com.nc.formengine.submission.dataimpl;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * The Spring configuration the submission persistence tests boot against.
 *
 * <p>This module is a library and has no application class of its own, so the slice needs one here.
 * It deliberately says nothing about where the entities and repositories are: the module's own
 * auto-configuration contributes those packages, and these tests are what proves it does.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class PersistenceTestConfiguration {
}
