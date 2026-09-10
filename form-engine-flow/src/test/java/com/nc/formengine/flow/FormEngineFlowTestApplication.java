package com.nc.formengine.flow;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The application the integration tests boot.
 *
 * <p>It is deliberately bare. It names no packages of its own, because the modules this one depends
 * on auto-configure their services, entities and repositories: if that chain ever breaks, these
 * tests fail with a missing bean rather than the breakage reaching whoever adds the jar to their
 * own application.
 */
@SpringBootApplication
class FormEngineFlowTestApplication {
}
