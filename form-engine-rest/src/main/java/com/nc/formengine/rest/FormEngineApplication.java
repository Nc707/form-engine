package com.nc.formengine.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.nc.formengine.rest",
    "com.nc.formengine.submission.businessimpl",
    "com.nc.formengine.submission.dataimpl",
    "com.nc.formengine.businessimpl",
    "com.nc.formengine.dataimpl"
})
@EnableJpaRepositories(basePackages = {
    "com.nc.formengine.submission.dataimpl.repository",
    "com.nc.formengine.dataimpl.repository"
})
@EntityScan(basePackages = {
		"com.nc.formengine.dataimpl.entity",
		"com.nc.formengine.submission.dataimpl.entity"
})
public class FormEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormEngineApplication.class, args);
    }
}
