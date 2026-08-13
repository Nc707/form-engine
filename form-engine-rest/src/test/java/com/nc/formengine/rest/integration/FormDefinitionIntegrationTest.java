package com.nc.formengine.rest.integration;

import tools.jackson.databind.ObjectMapper;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormDefinitionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateFormDefinitionAndRetrieveIt() throws Exception {
        // Given - a form
        FormDefinitionDTO formToCreate = new FormDefinitionDTO();
        formToCreate.setCode("contact_form");
        formToCreate.setTitle("Contact Form");
        formToCreate.setDescription("A form to collect contact information");
        formToCreate.setVersion(1);

        String formJson = objectMapper.writeValueAsString(formToCreate);

        // When - POST to create the form
        MvcResult createResult = mockMvc.perform(post("/api/v1/form-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(formJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value("contact_form"))
                .andExpect(jsonPath("$.title").value("Contact Form"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        FormDefinitionDTO createdForm = objectMapper.readValue(responseJson, FormDefinitionDTO.class);
        Long formId = createdForm.getId();

        // Then - it is persisted, fetched by id
        mockMvc.perform(get("/api/v1/form-definitions/{id}", formId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(formId))
                .andExpect(jsonPath("$.code").value("contact_form"))
                .andExpect(jsonPath("$.title").value("Contact Form"))
                .andExpect(jsonPath("$.description").value("A form to collect contact information"))
                .andExpect(jsonPath("$.version").value(1));

        // Then - it can be fetched by code
        mockMvc.perform(get("/api/v1/form-definitions/by-code/{code}", "contact_form"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(formId))
                .andExpect(jsonPath("$.code").value("contact_form"));

        // Then - it appears in the list of all forms
        mockMvc.perform(get("/api/v1/form-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + formId + ")].code").value("contact_form"));
    }

    @Test
    void shouldUpdateFormDefinition() throws Exception {
        // Given - a form
        FormDefinitionDTO formToCreate = new FormDefinitionDTO();
        formToCreate.setCode("update_test");
        formToCreate.setTitle("Original Name");
        formToCreate.setVersion(1);

        MvcResult createResult = mockMvc.perform(post("/api/v1/form-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formToCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        FormDefinitionDTO createdForm = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), 
                FormDefinitionDTO.class);
        Long formId = createdForm.getId();

        // When - updating the form, asking for a version bump along the way
        FormDefinitionDTO formToUpdate = new FormDefinitionDTO();
        formToUpdate.setCode("update_test");
        formToUpdate.setTitle("Updated Name");
        formToUpdate.setDescription("Updated description");
        formToUpdate.setVersion(2);

        // Then - the editable fields change, and the version does not: (code, version) is the
        // identity of this row, and a new version is made by copying the form, not by renumbering
        // it in place.
        mockMvc.perform(put("/api/v1/form-definitions/{id}", formId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formToUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(formId))
                .andExpect(jsonPath("$.title").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // And - the changes are persisted
        mockMvc.perform(get("/api/v1/form-definitions/{id}", formId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void shouldDeleteFormDefinition() throws Exception {
        // Given - a form
        FormDefinitionDTO formToCreate = new FormDefinitionDTO();
        formToCreate.setCode("delete_test");
        formToCreate.setTitle("To Be Deleted");
        formToCreate.setVersion(1);

        MvcResult createResult = mockMvc.perform(post("/api/v1/form-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formToCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        FormDefinitionDTO createdForm = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), 
                FormDefinitionDTO.class);
        Long formId = createdForm.getId();

        // When - deleting the form
        mockMvc.perform(delete("/api/v1/form-definitions/{id}", formId))
                .andExpect(status().isNoContent());

        // Then - it is gone
        mockMvc.perform(get("/api/v1/form-definitions/{id}", formId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCheckIfFormCodeExists() throws Exception {
        // Given - a form
        FormDefinitionDTO formToCreate = new FormDefinitionDTO();
        formToCreate.setCode("exists_test");
        formToCreate.setTitle("Exists Test");
        formToCreate.setVersion(1);

        mockMvc.perform(post("/api/v1/form-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formToCreate)))
                .andExpect(status().isCreated());

        // Then - the code is reported as taken
        MvcResult existsResult = mockMvc.perform(get("/api/v1/form-definitions/exists-by-code/{code}", "exists_test"))
                .andExpect(status().isOk())
                .andReturn();

        String existsResponse = existsResult.getResponse().getContentAsString();
        assertThat(Boolean.parseBoolean(existsResponse)).isTrue();

        // Then - an unknown code is reported as free
        MvcResult notExistsResult = mockMvc.perform(get("/api/v1/form-definitions/exists-by-code/{code}", "NON_EXISTENT"))
                .andExpect(status().isOk())
                .andReturn();

        String notExistsResponse = notExistsResult.getResponse().getContentAsString();
        assertThat(Boolean.parseBoolean(notExistsResponse)).isFalse();
    }
}
