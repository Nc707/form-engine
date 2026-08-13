package com.nc.formengine.rest.integration;

import tools.jackson.databind.ObjectMapper;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bean Validation used to be a no-op: the controllers carried {@code @Valid} but no DTO carried a
 * single constraint. These tests pin the constraints down so they cannot quietly disappear again.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectAFormDefinitionWithoutACode() throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setTitle("No code");
        form.setVersion(1);

        postExpectingValidationFailure("/api/v1/form-definitions", form)
                .andExpect(jsonPath("$.type").value("https://form-engine/errors/validation-failed"))
                .andExpect(jsonPath("$.title").value("Request Validation Failed"))
                .andExpect(jsonPath("$.errors[0].field").value("code"))
                .andExpect(jsonPath("$.errors[0].message").value("code is required"));
    }

    @Test
    void shouldRejectAFormDefinitionWithABlankTitle() throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("BLANK_TITLE");
        form.setTitle("   ");
        form.setVersion(1);

        postExpectingValidationFailure("/api/v1/form-definitions", form)
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].message").value("title is required"));
    }

    @Test
    void shouldRejectAFormDefinitionWithoutAVersion() throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("NO_VERSION");
        form.setTitle("No version");

        postExpectingValidationFailure("/api/v1/form-definitions", form)
                .andExpect(jsonPath("$.errors[0].field").value("version"))
                .andExpect(jsonPath("$.errors[0].message").value("version is required"));
    }

    @Test
    void shouldRejectANonPositiveVersion() throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("BAD_VERSION");
        form.setTitle("Bad version");
        form.setVersion(0);

        postExpectingValidationFailure("/api/v1/form-definitions", form)
                .andExpect(jsonPath("$.errors[0].field").value("version"))
                .andExpect(jsonPath("$.errors[0].message").value("version must be greater than 0"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value("0"));
    }

    @Test
    void shouldReportEveryViolationAtOnce() throws Exception {
        // Empty body: code, title and version are all missing.
        mockMvc.perform(post("/api/v1/form-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(3))
                // Sorted by field name, so the order is stable to assert on.
                .andExpect(jsonPath("$.errors[0].field").value("code"))
                .andExpect(jsonPath("$.errors[1].field").value("title"))
                .andExpect(jsonPath("$.errors[2].field").value("version"));
    }

    @Test
    void shouldRejectAFormSubmissionWithoutAFormDefinitionId() throws Exception {
        FormSubmissionDTO submission = new FormSubmissionDTO();
        submission.setFormCode("some_form");
        submission.setAuthor("user@example.com");

        postExpectingValidationFailure("/api/v1/form-submissions", submission)
                .andExpect(jsonPath("$.errors[0].field").value("formDefinitionId"))
                .andExpect(jsonPath("$.errors[0].message").value("formDefinitionId is required"));
    }

    /**
     * The column is NOT NULL, so leaving it out has to be caught on the way in. It used to reach the
     * database and come back as a 500 for what is plainly a bad request.
     */
    @Test
    void shouldRejectAFormSubmissionWithoutAFormCode() throws Exception {
        FormSubmissionDTO submission = new FormSubmissionDTO();
        submission.setFormDefinitionId(1L);
        submission.setAuthor("user@example.com");

        postExpectingValidationFailure("/api/v1/form-submissions", submission)
                .andExpect(jsonPath("$.errors[0].field").value("formCode"))
                .andExpect(jsonPath("$.errors[0].message").value("formCode is required"));
    }

    /**
     * A status is not the caller's to supply: the lifecycle owns it. Demanding one used to force every
     * client to name a state it had no business choosing.
     */
    @Test
    void shouldAcceptAFormSubmissionThatNamesNoStatus() throws Exception {
        FormSubmissionDTO submission = new FormSubmissionDTO();
        submission.setFormDefinitionId(1L);
        submission.setFormCode("some_form");
        submission.setAuthor("user@example.com");

        mockMvc.perform(post("/api/v1/form-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldRejectAFieldSubmissionWithoutItsForeignKeys() throws Exception {
        FieldSubmissionDTO answer = new FieldSubmissionDTO();
        answer.setValue("orphan");

        postExpectingValidationFailure("/api/v1/field-submissions", answer)
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("fieldDefinitionId"))
                .andExpect(jsonPath("$.errors[1].field").value("formSubmissionId"));
    }

    @Test
    void shouldRejectAFieldOptionWithoutALabelOrValue() throws Exception {
        FieldOptionDTO option = new FieldOptionDTO();
        option.setFieldDefinitionId(1L);

        postExpectingValidationFailure("/api/v1/field-options", option)
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("label"))
                .andExpect(jsonPath("$.errors[1].field").value("value"));
    }

    /** The constraints on FieldLayoutDTO only bite because FormLayoutDTO cascades with @Valid. */
    @Test
    void shouldRejectANestedFieldLayoutWithoutAFieldDefinitionId() throws Exception {
        FormLayoutDTO layout = FormLayoutDTO.builder()
                .formDefinitionId(1L)
                .fieldLayouts(List.of(FieldLayoutDTO.builder()
                        .row(0).column(0).colspan(12).rowspan(1)
                        .build()))
                .build();

        postExpectingValidationFailure("/api/v1/form-layouts", layout)
                .andExpect(jsonPath("$.errors[0].field").value("fieldLayouts[0].fieldDefinitionId"))
                .andExpect(jsonPath("$.errors[0].message").value("fieldDefinitionId is required"));
    }

    @Test
    void shouldRejectANegativeGridPosition() throws Exception {
        FormLayoutDTO layout = FormLayoutDTO.builder()
                .formDefinitionId(1L)
                .fieldLayouts(List.of(FieldLayoutDTO.builder()
                        .fieldDefinitionId(1L)
                        .row(-1).column(0).colspan(12).rowspan(1)
                        .build()))
                .build();

        postExpectingValidationFailure("/api/v1/form-layouts", layout)
                .andExpect(jsonPath("$.errors[0].field").value("fieldLayouts[0].row"))
                .andExpect(jsonPath("$.errors[0].message").value("row must not be negative"));
    }

    private org.springframework.test.web.servlet.ResultActions postExpectingValidationFailure(
            String path, Object body) throws Exception {
        return mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
