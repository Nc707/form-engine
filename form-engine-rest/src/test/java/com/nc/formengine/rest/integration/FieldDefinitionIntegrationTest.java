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
        // Create a form to attach the fields to
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("test_form");
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
        // Given - a field
        FieldDefinitionDTO fieldToCreate = new FieldDefinitionDTO();
        fieldToCreate.setFormDefinitionId(formDefinitionId);
        fieldToCreate.setName("email");
        fieldToCreate.setLabel("Email Address");
        fieldToCreate.setType(FieldType.TEXT);
        fieldToCreate.setRequired(true);
        fieldToCreate.setOrderIndex(1);

        String fieldJson = objectMapper.writeValueAsString(fieldToCreate);

        // When - POST to create the field
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

        // Then - it is persisted, fetched by id
        mockMvc.perform(get("/api/v1/field-definitions/{id}", fieldId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fieldId))
                .andExpect(jsonPath("$.name").value("email"))
                .andExpect(jsonPath("$.label").value("Email Address"))
                .andExpect(jsonPath("$.formDefinitionId").value(formDefinitionId));

        // Then - it shows up in the list for the form definition
        mockMvc.perform(get("/api/v1/field-definitions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + fieldId + ")].name").value("email"));
    }

    @Test
    void shouldCreateMultipleFieldsAndRetrieveByFormDefinition() throws Exception {
        // Given - several fields on the same form
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

        // When - creating the three fields
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

        // Then - all 3 fields are persisted
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
        // Given - a field
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

        // When - updating the field
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

        // Then - the changes are persisted
        mockMvc.perform(get("/api/v1/field-definitions/{id}", fieldId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Phone Number"))
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.required").value(true));
    }

    @Test
    void shouldDeleteFieldDefinition() throws Exception {
        // Given - a field
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

        // When - deleting the field
        mockMvc.perform(delete("/api/v1/field-definitions/{id}", fieldId))
                .andExpect(status().isNoContent());

        // Then - it is gone
        mockMvc.perform(get("/api/v1/field-definitions/{id}", fieldId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteAllFieldsByFormDefinition() throws Exception {
        // Given - several fields
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

        // Confirm there are 3 fields
        mockMvc.perform(get("/api/v1/field-definitions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // When - deleting every field of the form
        mockMvc.perform(delete("/api/v1/field-definitions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isNoContent());

        // Then - the form has no fields left
        mockMvc.perform(get("/api/v1/field-definitions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
