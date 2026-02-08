package com.nc.formengine.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
class FieldSubmissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long formDefinitionId;
    private Long formSubmissionId;
    private Long emailFieldDefinitionId;
    private Long nameFieldDefinitionId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Crear un formulario
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("SURVEY_FORM");
        form.setTitle("Survey Form");
        form.setVersion(1);

        MvcResult formResult = mockMvc.perform(post("/api/v1/form-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated())
                .andReturn();

        FormDefinitionDTO createdForm = objectMapper.readValue(
                formResult.getResponse().getContentAsString(),
                FormDefinitionDTO.class);
        formDefinitionId = createdForm.getId();

        // 2. Crear campos del formulario
        FieldDefinitionDTO emailField = new FieldDefinitionDTO();
        emailField.setFormDefinitionId(formDefinitionId);
        emailField.setName("email");
        emailField.setLabel("Email");
        emailField.setType(FieldType.TEXT);
        emailField.setRequired(true);
        emailField.setOrderIndex(1);

        MvcResult emailResult = mockMvc.perform(post("/api/v1/field-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailField)))
                .andExpect(status().isCreated())
                .andReturn();

        FieldDefinitionDTO createdEmailField = objectMapper.readValue(
                emailResult.getResponse().getContentAsString(),
                FieldDefinitionDTO.class);
        emailFieldDefinitionId = createdEmailField.getId();

        FieldDefinitionDTO nameField = new FieldDefinitionDTO();
        nameField.setFormDefinitionId(formDefinitionId);
        nameField.setName("name");
        nameField.setLabel("Full Name");
        nameField.setType(FieldType.TEXT);
        nameField.setRequired(true);
        nameField.setOrderIndex(2);

        MvcResult nameResult = mockMvc.perform(post("/api/v1/field-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nameField)))
                .andExpect(status().isCreated())
                .andReturn();

        FieldDefinitionDTO createdNameField = objectMapper.readValue(
                nameResult.getResponse().getContentAsString(),
                FieldDefinitionDTO.class);
        nameFieldDefinitionId = createdNameField.getId();

        // 3. Crear una submission del formulario
        FormSubmissionDTO formSubmission = new FormSubmissionDTO();
        formSubmission.setFormDefinitionId(formDefinitionId);
        formSubmission.setFormCode("SURVEY_FORM");
        formSubmission.setSubmittedBy("testuser@example.com");
        formSubmission.setStatus(SubmissionStatus.DRAFT);

        MvcResult submissionResult = mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formSubmission)))
                .andExpect(status().isCreated())
                .andReturn();

        FormSubmissionDTO createdSubmission = objectMapper.readValue(
                submissionResult.getResponse().getContentAsString(),
                FormSubmissionDTO.class);
        formSubmissionId = createdSubmission.getId();
    }

    @Test
    void shouldCreateFieldSubmissionAndRetrieveIt() throws Exception {
        // Given - Crear una field submission
        FieldSubmissionDTO fieldSubmissionToCreate = new FieldSubmissionDTO();
        fieldSubmissionToCreate.setFormSubmissionId(formSubmissionId);
        fieldSubmissionToCreate.setFieldDefinitionId(emailFieldDefinitionId);
        fieldSubmissionToCreate.setValue("user@example.com");

        String fieldSubmissionJson = objectMapper.writeValueAsString(fieldSubmissionToCreate);

        // When - POST para crear la field submission
        MvcResult createResult = mockMvc.perform(post("/api/v1/field-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(fieldSubmissionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.formSubmissionId").value(formSubmissionId))
                .andExpect(jsonPath("$.fieldDefinitionId").value(emailFieldDefinitionId))
                .andExpect(jsonPath("$.value").value("user@example.com"))
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        FieldSubmissionDTO createdFieldSubmission = objectMapper.readValue(responseJson, FieldSubmissionDTO.class);
        Long fieldSubmissionId = createdFieldSubmission.getId();

        // Then - Verificar que se guardó en la BD con GET por ID
        mockMvc.perform(get("/api/v1/field-submissions/{id}", fieldSubmissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fieldSubmissionId))
                .andExpect(jsonPath("$.value").value("user@example.com"));

        // Then - Verificar que aparece en la lista por form submission
        mockMvc.perform(get("/api/v1/field-submissions/by-form-submission/{formSubmissionId}", formSubmissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + fieldSubmissionId + ")].value").value("user@example.com"));
    }

    @Test
    void shouldCreateCompleteFormSubmissionWithAllFields() throws Exception {
        // Given - Crear field submissions para todos los campos
        FieldSubmissionDTO emailSubmission = new FieldSubmissionDTO();
        emailSubmission.setFormSubmissionId(formSubmissionId);
        emailSubmission.setFieldDefinitionId(emailFieldDefinitionId);
        emailSubmission.setValue("john.doe@example.com");

        FieldSubmissionDTO nameSubmission = new FieldSubmissionDTO();
        nameSubmission.setFormSubmissionId(formSubmissionId);
        nameSubmission.setFieldDefinitionId(nameFieldDefinitionId);
        nameSubmission.setValue("John Doe");

        // When - Crear las field submissions
        mockMvc.perform(post("/api/v1/field-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailSubmission)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value("john.doe@example.com"));

        mockMvc.perform(post("/api/v1/field-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nameSubmission)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value("John Doe"));

        // Then - Verificar que ambas submissions están en la BD
        mockMvc.perform(get("/api/v1/field-submissions/by-form-submission/{formSubmissionId}", formSubmissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.fieldDefinitionId == " + emailFieldDefinitionId + ")].value").value("john.doe@example.com"))
                .andExpect(jsonPath("$[?(@.fieldDefinitionId == " + nameFieldDefinitionId + ")].value").value("John Doe"));
    }

    @Test
    void shouldFilterFieldSubmissionsByFieldDefinition() throws Exception {
        // Given - Crear otra form submission
        FormSubmissionDTO formSubmission2 = new FormSubmissionDTO();
        formSubmission2.setFormDefinitionId(formDefinitionId);
        formSubmission2.setFormCode("SURVEY_FORM");
        formSubmission2.setSubmittedBy("user2@example.com");
        formSubmission2.setStatus(SubmissionStatus.SUBMITTED);

        MvcResult submission2Result = mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(formSubmission2)))
                .andExpect(status().isCreated())
                .andReturn();

        FormSubmissionDTO createdSubmission2 = objectMapper.readValue(
                submission2Result.getResponse().getContentAsString(),
                FormSubmissionDTO.class);
        Long formSubmissionId2 = createdSubmission2.getId();

        // Crear field submissions para el campo email en ambas form submissions
        FieldSubmissionDTO emailSubmission1 = new FieldSubmissionDTO();
        emailSubmission1.setFormSubmissionId(formSubmissionId);
        emailSubmission1.setFieldDefinitionId(emailFieldDefinitionId);
        emailSubmission1.setValue("first@example.com");

        FieldSubmissionDTO emailSubmission2 = new FieldSubmissionDTO();
        emailSubmission2.setFormSubmissionId(formSubmissionId2);
        emailSubmission2.setFieldDefinitionId(emailFieldDefinitionId);
        emailSubmission2.setValue("second@example.com");

        // When - Crear las submissions
        mockMvc.perform(post("/api/v1/field-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailSubmission1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/field-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailSubmission2)))
                .andExpect(status().isCreated());

        // Then - Verificar que ambas submissions del campo email están en la BD
        mockMvc.perform(get("/api/v1/field-submissions/by-field-definition/{fieldDefinitionId}", emailFieldDefinitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.value == 'first@example.com')]").exists())
                .andExpect(jsonPath("$[?(@.value == 'second@example.com')]").exists());
    }

    @Test
    void shouldUpdateFieldSubmission() throws Exception {
        // Given - Crear una field submission
        FieldSubmissionDTO fieldSubmissionToCreate = new FieldSubmissionDTO();
        fieldSubmissionToCreate.setFormSubmissionId(formSubmissionId);
        fieldSubmissionToCreate.setFieldDefinitionId(emailFieldDefinitionId);
        fieldSubmissionToCreate.setValue("old@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/field-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fieldSubmissionToCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        FieldSubmissionDTO createdFieldSubmission = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                FieldSubmissionDTO.class);
        Long fieldSubmissionId = createdFieldSubmission.getId();

        // When - Actualizar el valor
        FieldSubmissionDTO fieldSubmissionToUpdate = new FieldSubmissionDTO();
        fieldSubmissionToUpdate.setFormSubmissionId(formSubmissionId);
        fieldSubmissionToUpdate.setFieldDefinitionId(emailFieldDefinitionId);
        fieldSubmissionToUpdate.setValue("new@example.com");

        mockMvc.perform(put("/api/v1/field-submissions/{id}", fieldSubmissionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fieldSubmissionToUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fieldSubmissionId))
                .andExpect(jsonPath("$.value").value("new@example.com"));

        // Then - Verificar cambios en la BD
        mockMvc.perform(get("/api/v1/field-submissions/{id}", fieldSubmissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("new@example.com"));
    }

    @Test
    void shouldDeleteFieldSubmission() throws Exception {
        // Given - Crear una field submission
        FieldSubmissionDTO fieldSubmissionToCreate = new FieldSubmissionDTO();
        fieldSubmissionToCreate.setFormSubmissionId(formSubmissionId);
        fieldSubmissionToCreate.setFieldDefinitionId(emailFieldDefinitionId);
        fieldSubmissionToCreate.setValue("temp@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/field-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fieldSubmissionToCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        FieldSubmissionDTO createdFieldSubmission = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                FieldSubmissionDTO.class);
        Long fieldSubmissionId = createdFieldSubmission.getId();

        // When - Eliminar la field submission
        mockMvc.perform(delete("/api/v1/field-submissions/{id}", fieldSubmissionId))
                .andExpect(status().isNoContent());

        // Then - Verificar que no existe en la BD
        mockMvc.perform(get("/api/v1/field-submissions/{id}", fieldSubmissionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteAllFieldSubmissionsByFormSubmission() throws Exception {
        // Given - Crear varias field submissions
        FieldSubmissionDTO emailSubmission = new FieldSubmissionDTO();
        emailSubmission.setFormSubmissionId(formSubmissionId);
        emailSubmission.setFieldDefinitionId(emailFieldDefinitionId);
        emailSubmission.setValue("user@example.com");

        FieldSubmissionDTO nameSubmission = new FieldSubmissionDTO();
        nameSubmission.setFormSubmissionId(formSubmissionId);
        nameSubmission.setFieldDefinitionId(nameFieldDefinitionId);
        nameSubmission.setValue("User Name");

        mockMvc.perform(post("/api/v1/field-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emailSubmission)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/field-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nameSubmission)))
                .andExpect(status().isCreated());

        // Verificar que hay 2 field submissions
        mockMvc.perform(get("/api/v1/field-submissions/by-form-submission/{formSubmissionId}", formSubmissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // When - Eliminar todas las field submissions de la form submission
        mockMvc.perform(delete("/api/v1/field-submissions/by-form-submission/{formSubmissionId}", formSubmissionId))
                .andExpect(status().isNoContent());

        // Then - Verificar que no hay field submissions en la BD
        mockMvc.perform(get("/api/v1/field-submissions/by-form-submission/{formSubmissionId}", formSubmissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
