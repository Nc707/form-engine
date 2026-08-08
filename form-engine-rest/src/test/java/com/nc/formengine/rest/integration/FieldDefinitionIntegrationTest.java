package com.nc.formengine.rest.integration;

import tools.jackson.databind.ObjectMapper;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FieldDefinitionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long formDefinitionId;

    @BeforeEach
    void setUp() throws Exception {
        // Crear un formulario para asociar los campos
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("TEST_FORM");
        form.setTitle("Test Form");
        form.setVersion(1);

        MvcResult result = mockMvc.perform(post("/api/v1/form-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated())
                .andReturn();

        FormDefinitionDTO createdForm = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                FormDefinitionDTO.class);
        formDefinitionId = createdForm.getId();
    }

    @Test
    void shouldCreateFieldDefinitionAndRetrieveIt() throws Exception {
        // Given - Crear un campo
        FieldDefinitionDTO fieldToCreate = new FieldDefinitionDTO();
        fieldToCreate.setFormDefinitionId(formDefinitionId);
        fieldToCreate.setName("email");
        fieldToCreate.setLabel("Email Address");
        fieldToCreate.setType(FieldType.TEXT);
        fieldToCreate.setRequired(true);
        fieldToCreate.setOrderIndex(1);

        String fieldJson = objectMapper.writeValueAsString(fieldToCreate);

        // When - POST para crear el campo
        MvcResult createResult = mockMvc.perform(post("/api/v1/field-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(fieldJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("email"))
                .andExpect(jsonPath("$.label").value("Email Address"))
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.orderIndex").value(1))
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        FieldDefinitionDTO createdField = objectMapper.readValue(responseJson, FieldDefinitionDTO.class);
        Long fieldId = createdField.getId();

        // Then - Verificar que se guardó en la BD con GET por ID
        mockMvc.perform(get("/api/v1/field-definitions/{id}", fieldId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fieldId))
                .andExpect(jsonPath("$.name").value("email"))
                .andExpect(jsonPath("$.label").value("Email Address"))
                .andExpect(jsonPath("$.formDefinitionId").value(formDefinitionId));

        // Then - Verificar que aparece en la lista por form definition
        mockMvc.perform(get("/api/v1/field-definitions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + fieldId + ")].name").value("email"));
    }

    @Test
    void shouldCreateMultipleFieldsAndRetrieveByFormDefinition() throws Exception {
        // Given - Crear varios campos para el mismo formulario
        FieldDefinitionDTO field1 = new FieldDefinitionDTO();
        field1.setFormDefinitionId(formDefinitionId);
        field1.setName("name");
        field1.setLabel("Full Name");
        field1.setType(FieldType.TEXT);
        field1.setRequired(true);
        field1.setOrderIndex(1);

        FieldDefinitionDTO field2 = new FieldDefinitionDTO();
        field2.setFormDefinitionId(formDefinitionId);
        field2.setName("age");
        field2.setLabel("Age");
        field2.setType(FieldType.NUMBER);
        field2.setRequired(false);
        field2.setOrderIndex(2);

        FieldDefinitionDTO field3 = new FieldDefinitionDTO();
        field3.setFormDefinitionId(formDefinitionId);
        field3.setName("comments");
        field3.setLabel("Comments");
        field3.setType(FieldType.TEXT);
        field3.setRequired(false);
        field3.setOrderIndex(3);

        // When - Crear los tres campos
        mockMvc.perform(post("/api/v1/field-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(field1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/field-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(field2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/field-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(field3)))
                .andExpect(status().isCreated());

        // Then - Verificar que los 3 campos aparecen en la BD
        mockMvc.perform(get("/api/v1/field-definitions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.name == 'name')].label").value("Full Name"))
                .andExpect(jsonPath("$[?(@.name == 'age')].label").value("Age"))
                .andExpect(jsonPath("$[?(@.name == 'comments')].label").value("Comments"));
    }

    @Test
    void shouldUpdateFieldDefinition() throws Exception {
        // Given - Crear un campo
        FieldDefinitionDTO fieldToCreate = new FieldDefinitionDTO();
        fieldToCreate.setFormDefinitionId(formDefinitionId);
        fieldToCreate.setName("phone");
        fieldToCreate.setLabel("Phone");
        fieldToCreate.setType(FieldType.TEXT);
        fieldToCreate.setRequired(false);
        fieldToCreate.setOrderIndex(1);

        MvcResult createResult = mockMvc.perform(post("/api/v1/field-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fieldToCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        FieldDefinitionDTO createdField = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                FieldDefinitionDTO.class);
        Long fieldId = createdField.getId();

        // When - Actualizar el campo
        FieldDefinitionDTO fieldToUpdate = new FieldDefinitionDTO();
        fieldToUpdate.setFormDefinitionId(formDefinitionId);
        fieldToUpdate.setName("phone");
        fieldToUpdate.setLabel("Phone Number");
        fieldToUpdate.setType(FieldType.TEXT);
        fieldToUpdate.setRequired(true);
        fieldToUpdate.setOrderIndex(2);

        mockMvc.perform(put("/api/v1/field-definitions/{id}", fieldId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fieldToUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fieldId))
                .andExpect(jsonPath("$.label").value("Phone Number"))
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.required").value(true));

        // Then - Verificar cambios en la BD
        mockMvc.perform(get("/api/v1/field-definitions/{id}", fieldId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Phone Number"))
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.required").value(true));
    }

    @Test
    void shouldDeleteFieldDefinition() throws Exception {
        // Given - Crear un campo
        FieldDefinitionDTO fieldToCreate = new FieldDefinitionDTO();
        fieldToCreate.setFormDefinitionId(formDefinitionId);
        fieldToCreate.setName("temp");
        fieldToCreate.setLabel("Temporary Field");
        fieldToCreate.setType(FieldType.TEXT);
        fieldToCreate.setOrderIndex(1);

        MvcResult createResult = mockMvc.perform(post("/api/v1/field-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fieldToCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        FieldDefinitionDTO createdField = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                FieldDefinitionDTO.class);
        Long fieldId = createdField.getId();

        // When - Eliminar el campo
        mockMvc.perform(delete("/api/v1/field-definitions/{id}", fieldId))
                .andExpect(status().isNoContent());

        // Then - Verificar que no existe en la BD
        mockMvc.perform(get("/api/v1/field-definitions/{id}", fieldId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteAllFieldsByFormDefinition() throws Exception {
        // Given - Crear varios campos
        for (int i = 1; i <= 3; i++) {
            FieldDefinitionDTO field = new FieldDefinitionDTO();
            field.setFormDefinitionId(formDefinitionId);
            field.setName("field" + i);
            field.setLabel("Field " + i);
            field.setType(FieldType.TEXT);
            field.setOrderIndex(i);

            mockMvc.perform(post("/api/v1/field-definitions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(field)))
                    .andExpect(status().isCreated());
        }

        // Verificar que hay 3 campos
        mockMvc.perform(get("/api/v1/field-definitions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // When - Eliminar todos los campos del formulario
        mockMvc.perform(delete("/api/v1/field-definitions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isNoContent());

        // Then - Verificar que no hay campos en la BD
        mockMvc.perform(get("/api/v1/field-definitions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
