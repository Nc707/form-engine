package com.nc.formengine.demo;

import com.nc.formengine.flow.spi.SubmissionAuthorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * What this application tells the library about itself.
 */
@Configuration
public class DemoConfiguration {

    /**
     * There is no authentication here, so everything is filed under one name. A real application
     * would answer from its security context; the point of the seam is that the library never has
     * to know which of the two it is talking to.
     */
    @Bean
    SubmissionAuthorProvider submissionAuthorProvider() {
        return () -> "demo";
    }
}
