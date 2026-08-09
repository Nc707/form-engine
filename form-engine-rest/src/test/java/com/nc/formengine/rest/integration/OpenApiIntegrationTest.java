package com.nc.formengine.rest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Guards the springdoc integration. springdoc is not part of the Spring Boot BOM, so nothing but a
 * test tells us the pinned version still starts on the Boot version this project is on.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldServeTheOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Form Engine API"))
                .andExpect(jsonPath("$.paths['/api/v1/form-definitions']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/form-submissions/by-status/{status}']").exists());
    }

    @Test
    void shouldDocumentEveryControllerAsATag() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(7));
    }

    @Test
    void shouldDocumentTheSharedProblemDetailResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/form-definitions/{id}'].get.responses['404']"
                                + ".content['application/problem+json']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/form-definitions'].post.responses['409']").exists());
    }

    @Test
    void shouldServeSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
