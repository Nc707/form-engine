package com.nc.formengine.rest.integration;

import tools.jackson.databind.ObjectMapper;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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
        // Given - Crear un formulario
        FormDefinitionDTO formToCreate = new FormDefinitionDTO();
        formToCreate.setCode("CONTACT_FORM");
        formToCreate.setTitle("Contact Form");
        formToCreate.setDescription("A form to collect contact information");
        formToCreate.setVersion(1);

        String formJson = objectMapper.writeValueAsString(formToCreate);

        // When - POST para crear el formulario
        MvcResult createResult = mockMvc.perform(post("/api/v1/form-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(formJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value("CONTACT_FORM"))
                .andExpect(jsonPath("$.title").value("Contact Form"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        FormDefinitionDTO createdForm = objectMapper.readValue(responseJson, FormDefinitionDTO.class);
        Long formId = createdForm.getId();

        // Then - Verificar que se guardó en la base de datos con GET por ID
        mockMvc.perform(get("/api/v1/form-definitions/{id}", formId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(formId))
                .andExpect(jsonPath("$.code").value("CONTACT_FORM"))
                .andExpect(jsonPath("$.title").value("Contact Form"))
                .andExpect(jsonPath("$.description").value("A form to collect contact information"))
                .andExpect(jsonPath("$.version").value(1));

        // Then - Verificar que se puede obtener por código
        mockMvc.perform(get("/api/v1/form-definitions/by-code/{code}", "CONTACT_FORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(formId))
                .andExpect(jsonPath("$.code").value("CONTACT_FORM"));

        // Then - Verificar que aparece en la lista de todos los formularios
        mockMvc.perform(get("/api/v1/form-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + formId + ")].code").value("CONTACT_FORM"));
    }

    @Test
    void shouldUpdateFormDefinition() throws Exception {
        // Given - Crear un formulario
        FormDefinitionDTO formToCreate = new FormDefinitionDTO();
        formToCreate.setCode("UPDATE_TEST");
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

        // When - Actualizar el formulario
        FormDefinitionDTO formToUpdate = new FormDefinitionDTO();
        formToUpdate.setCode("UPDATE_TEST");
        formToUpdate.setTitle("Updated Name");
        formToUpdate.setDescription("Updated description");
        formToUpdate.setVersion(2);

        mockMvc.perform(put("/api/v1/form-definitions/{id}", formId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formToUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(formId))
                .andExpect(jsonPath("$.title").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.version").value(2));

        // Then - Verificar que los cambios se guardaron en la BD
        mockMvc.perform(get("/api/v1/form-definitions/{id}", formId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void shouldDeleteFormDefinition() throws Exception {
        // Given - Crear un formulario
        FormDefinitionDTO formToCreate = new FormDefinitionDTO();
        formToCreate.setCode("DELETE_TEST");
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

        // When - Eliminar el formulario
        mockMvc.perform(delete("/api/v1/form-definitions/{id}", formId))
                .andExpect(status().isNoContent());

        // Then - Verificar que ya no existe en la BD
        mockMvc.perform(get("/api/v1/form-definitions/{id}", formId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCheckIfFormCodeExists() throws Exception {
        // Given - Crear un formulario
        FormDefinitionDTO formToCreate = new FormDefinitionDTO();
        formToCreate.setCode("EXISTS_TEST");
        formToCreate.setTitle("Exists Test");
        formToCreate.setVersion(1);

        mockMvc.perform(post("/api/v1/form-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formToCreate)))
                .andExpect(status().isCreated());

        // Then - Verificar que el código existe
        MvcResult existsResult = mockMvc.perform(get("/api/v1/form-definitions/exists-by-code/{code}", "EXISTS_TEST"))
                .andExpect(status().isOk())
                .andReturn();

        String existsResponse = existsResult.getResponse().getContentAsString();
        assertThat(Boolean.parseBoolean(existsResponse)).isTrue();

        // Then - Verificar que un código inexistente retorna false
        MvcResult notExistsResult = mockMvc.perform(get("/api/v1/form-definitions/exists-by-code/{code}", "NON_EXISTENT"))
                .andExpect(status().isOk())
                .andReturn();

        String notExistsResponse = notExistsResult.getResponse().getContentAsString();
        assertThat(Boolean.parseBoolean(notExistsResponse)).isFalse();
    }
}
