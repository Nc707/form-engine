package com.nc.formengine.submission.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.nc.formengine.submission.rest",
    "com.nc.formengine.submission.businessimpl",
    "com.nc.formengine.submission.dataimpl",
    "com.nc.formengine.businessimpl"
})
public class FormEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormEngineApplication.class, args);
    }
}
