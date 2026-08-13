package com.nc.formengine.rest.integration;

import tools.jackson.databind.ObjectMapper;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the error model: every failure comes back as an RFC 7807 problem detail with the right
 * status, instead of the bare 500 the API used to return.
 *
 * <p>Each test does its setup first and triggers the failure last: an exception thrown inside a
 * service marks the surrounding test transaction rollback-only, so nothing useful can follow it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ErrorHandlingIntegrationTest {

    private static final long UNKNOWN_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnProblemDetailWhenFormDefinitionIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/form-definitions/{id}", UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://form-engine/errors/not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Form not found with id: " + UNKNOWN_ID))
                .andExpect(jsonPath("$.instance").value("/api/v1/form-definitions/" + UNKNOWN_ID))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnNotFoundWhenLookingUpAnUnknownFormCode() throws Exception {
        mockMvc.perform(get("/api/v1/form-definitions/by-code/{code}", "NO_SUCH_CODE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Form not found with code: NO_SUCH_CODE"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingAnUnknownFormDefinition() throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("GHOST_FORM");
        form.setTitle("Ghost");
        form.setVersion(1);

        mockMvc.perform(put("/api/v1/form-definitions/{id}", UNKNOWN_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnNotFoundForUnknownSubmissionResources() throws Exception {
        mockMvc.perform(get("/api/v1/form-submissions/{id}", UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Form submission not found with id: " + UNKNOWN_ID));

        mockMvc.perform(get("/api/v1/field-submissions/{id}", UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Field submission not found with id: " + UNKNOWN_ID));
    }

    /** Reaches the DAO, which used to throw a bare RuntimeException and surface as a 500. */
    @Test
    void shouldReturnNotFoundWhenCreatingAFieldUnderAnUnknownForm() throws Exception {
        FieldDefinitionDTO field = FieldDefinitionDTO.builder()
                .formDefinitionId(UNKNOWN_ID)
                .name("orphan")
                .label("Orphan")
                .type(FieldType.TEXT)
                .orderIndex(0)
                .build();

        mockMvc.perform(post("/api/v1/field-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(field)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Form not found with id: " + UNKNOWN_ID));
    }

    @Test
    void shouldReturnNotFoundWhenCreatingAnAnswerUnderAnUnknownSubmission() throws Exception {
        FieldSubmissionDTO answer = FieldSubmissionDTO.builder()
                .formSubmissionId(UNKNOWN_ID)
                .fieldDefinitionId(1L)
                .value("orphan")
                .build();

        mockMvc.perform(post("/api/v1/field-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Form submission not found with id: " + UNKNOWN_ID));
    }

    @Test
    void shouldReturnBadRequestWhenCreatingWithAnId() throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setId(42L);
        form.setCode("HAS_ID");
        form.setTitle("Has an id");
        form.setVersion(1);

        mockMvc.perform(post("/api/v1/form-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://form-engine/errors/invalid-request"))
                .andExpect(jsonPath("$.detail").value("New form should not have an ID"));
    }

    @Test
    void shouldReturnConflictWhenTheFormCodeIsAlreadyTaken() throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("DUPLICATE_CODE");
        form.setTitle("First");
        form.setVersion(1);
        String json = objectMapper.writeValueAsString(form);

        mockMvc.perform(post("/api/v1/form-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // Same code again: 409, not a 500 from the database.
        mockMvc.perform(post("/api/v1/form-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://form-engine/errors/duplicate-resource"))
                .andExpect(jsonPath("$.title").value("Duplicate Resource"))
                .andExpect(jsonPath("$.field").value("code"))
                .andExpect(jsonPath("$.value").value("DUPLICATE_CODE"));
    }

    @Test
    void shouldAllowAFormToKeepItsOwnCodeOnUpdate() throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("KEEPS_CODE");
        form.setTitle("Original");
        form.setVersion(1);

        MvcResult created = mockMvc.perform(post("/api/v1/form-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readValue(created.getResponse().getContentAsString(),
                FormDefinitionDTO.class).getId();

        FormDefinitionDTO update = new FormDefinitionDTO();
        update.setCode("KEEPS_CODE");
        update.setTitle("Renamed");
        update.setVersion(2);

        mockMvc.perform(put("/api/v1/form-definitions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"));
    }

    @Test
    void shouldReturnBadRequestForAnUnknownSubmissionStatus() throws Exception {
        mockMvc.perform(get("/api/v1/form-submissions/by-status/{status}", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://form-engine/errors/invalid-request"))
                .andExpect(jsonPath("$.detail").value("Unknown submission status: 'NOT_A_STATUS'. "
                        + "Allowed values are DRAFT, SUBMITTED, DISCARDED, VOIDED."))
                .andExpect(jsonPath("$.allowedValues").isArray())
                .andExpect(jsonPath("$.allowedValues.length()").value(4));
    }

    @Test
    void shouldReturnBadRequestWhenAPathVariableHasTheWrongType() throws Exception {
        mockMvc.perform(get("/api/v1/form-definitions/{id}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnBadRequestForAMalformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/form-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenALayoutReferencesAnotherFormsField() throws Exception {
        Long targetFormId = createFormWithFields("LAYOUT_TARGET").getId();
        FormDefinitionDTO otherForm = createFormWithFields("LAYOUT_OTHER");
        Long foreignFieldId = otherForm.getFields().get(0).getId();

        FormLayoutDTO layout = FormLayoutDTO.builder()
                .formDefinitionId(targetFormId)
                .deviceType(DeviceType.MOBILE)
                .fieldLayouts(List.of(FieldLayoutDTO.builder()
                        .fieldDefinitionId(foreignFieldId)
                        .row(0).column(0).colspan(12).rowspan(1)
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/form-layouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(layout)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://form-engine/errors/domain-validation-failed"))
                .andExpect(jsonPath("$.title").value("Domain Validation Failed"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").value(
                        "Field " + foreignFieldId + " does not belong to form " + targetFormId));
    }

    @Test
    void shouldReturnNotFoundWhenNoLayoutCanBeResolved() throws Exception {
        Long formId = createFormWithFields("NO_LAYOUT").getId();

        mockMvc.perform(get("/api/v1/form-layouts/form/{id}/resolve", formId)
                        .param("deviceType", "MOBILE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(
                        "No layout found for form " + formId + " and device type MOBILE"));
    }

    private FormDefinitionDTO createFormWithFields(String code) throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode(code);
        form.setTitle("Form " + code);
        form.setVersion(1);
        form.setFields(List.of(
                FieldDefinitionDTO.builder()
                        .name("name").label("Name").type(FieldType.TEXT).orderIndex(0).required(true).build(),
                FieldDefinitionDTO.builder()
                        .name("email").label("Email").type(FieldType.TEXT).orderIndex(1).required(true).build()));

        MvcResult result = mockMvc.perform(post("/api/v1/form-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), FormDefinitionDTO.class);
    }
}
