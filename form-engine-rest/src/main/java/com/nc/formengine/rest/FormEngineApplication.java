package com.nc.formengine.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The REST API.
 *
 * <p>It names none of the engine's packages: every module it depends on auto-configures its
 * own beans, entities and repositories, which is the same thing that happens to anyone who
 * adds those jars to an application of their own.
 */
@SpringBootApplication
public class FormEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormEngineApplication.class, args);
    }
}
