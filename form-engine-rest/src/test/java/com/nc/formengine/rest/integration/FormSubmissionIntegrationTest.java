package com.nc.formengine.rest.integration;

import tools.jackson.databind.ObjectMapper;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormSubmissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long formDefinitionId;
    private String formCode;

    @BeforeEach
    void setUp() throws Exception {
        // Create a form for the submissions
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode("CONTACT_FORM");
        form.setTitle("Contact Form");
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
        formCode = createdForm.getCode();
    }

    @Test
    void shouldCreateFormSubmissionAndRetrieveIt() throws Exception {
        // Given - a submission
        FormSubmissionDTO submissionToCreate = new FormSubmissionDTO();
        submissionToCreate.setFormDefinitionId(formDefinitionId);
        submissionToCreate.setFormCode(formCode);
        submissionToCreate.setSubmittedBy("user@example.com");
        submissionToCreate.setStatus(SubmissionStatus.DRAFT);
        submissionToCreate.setSubmittedAt(LocalDateTime.now());

        String submissionJson = objectMapper.writeValueAsString(submissionToCreate);

        // When - POST to create the submission
        MvcResult createResult = mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(submissionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.formDefinitionId").value(formDefinitionId))
                .andExpect(jsonPath("$.formCode").value(formCode))
                .andExpect(jsonPath("$.submittedBy").value("user@example.com"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        FormSubmissionDTO createdSubmission = objectMapper.readValue(responseJson, FormSubmissionDTO.class);
        Long submissionId = createdSubmission.getId();

        // Then - it is persisted, fetched by id
        mockMvc.perform(get("/api/v1/form-submissions/{id}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissionId))
                .andExpect(jsonPath("$.formDefinitionId").value(formDefinitionId))
                .andExpect(jsonPath("$.submittedBy").value("user@example.com"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // Then - it shows up in the list for the form definition
        mockMvc.perform(get("/api/v1/form-submissions/by-form-definition/{formDefinitionId}", formDefinitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + submissionId + ")].submittedBy").value("user@example.com"));

        // Then - it can be found by form code
        mockMvc.perform(get("/api/v1/form-submissions/by-form-code/{formCode}", formCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + submissionId + ")].formCode").value(formCode));
    }

    @Test
    void shouldCreateMultipleSubmissionsAndFilterByUser() throws Exception {
        // Given - submissions from different users
        String user1 = "alice@example.com";
        String user2 = "bob@example.com";

        FormSubmissionDTO submission1 = new FormSubmissionDTO();
        submission1.setFormDefinitionId(formDefinitionId);
        submission1.setFormCode(formCode);
        submission1.setSubmittedBy(user1);
        submission1.setStatus(SubmissionStatus.SUBMITTED);

        FormSubmissionDTO submission2 = new FormSubmissionDTO();
        submission2.setFormDefinitionId(formDefinitionId);
        submission2.setFormCode(formCode);
        submission2.setSubmittedBy(user1);
        submission2.setStatus(SubmissionStatus.SUBMITTED);

        FormSubmissionDTO submission3 = new FormSubmissionDTO();
        submission3.setFormDefinitionId(formDefinitionId);
        submission3.setFormCode(formCode);
        submission3.setSubmittedBy(user2);
        submission3.setStatus(SubmissionStatus.SUBMITTED);

        // When - creating the submissions
        mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submission1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submission2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submission3)))
                .andExpect(status().isCreated());

        // Then - filtering by user returns alice's 2
        mockMvc.perform(get("/api/v1/form-submissions/by-submitted-by/{submittedBy}", user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].submittedBy").value(user1))
                .andExpect(jsonPath("$[1].submittedBy").value(user1));

        // Then - filtering by user returns bob's 1
        mockMvc.perform(get("/api/v1/form-submissions/by-submitted-by/{submittedBy}", user2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].submittedBy").value(user2));
    }

    @Test
    void shouldFilterSubmissionsByStatus() throws Exception {
        // Given - submissions in different statuses
        FormSubmissionDTO draftSubmission = new FormSubmissionDTO();
        draftSubmission.setFormDefinitionId(formDefinitionId);
        draftSubmission.setFormCode(formCode);
        draftSubmission.setSubmittedBy("user1@example.com");
        draftSubmission.setStatus(SubmissionStatus.DRAFT);

        FormSubmissionDTO submittedSubmission = new FormSubmissionDTO();
        submittedSubmission.setFormDefinitionId(formDefinitionId);
        submittedSubmission.setFormCode(formCode);
        submittedSubmission.setSubmittedBy("user2@example.com");
        submittedSubmission.setStatus(SubmissionStatus.SUBMITTED);

        FormSubmissionDTO completedSubmission = new FormSubmissionDTO();
        completedSubmission.setFormDefinitionId(formDefinitionId);
        completedSubmission.setFormCode(formCode);
        completedSubmission.setSubmittedBy("user3@example.com");
        completedSubmission.setStatus(SubmissionStatus.CANCELED);

        // When - creating the submissions
        mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(draftSubmission)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submittedSubmission)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completedSubmission)))
                .andExpect(status().isCreated());

        // Then - filtering by DRAFT
        mockMvc.perform(get("/api/v1/form-submissions/by-status/{status}", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));

        // Then - filtering by SUBMITTED
        mockMvc.perform(get("/api/v1/form-submissions/by-status/{status}", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));
    }

    @Test
    void shouldUpdateFormSubmission() throws Exception {
        // Given - a submission
        FormSubmissionDTO submissionToCreate = new FormSubmissionDTO();
        submissionToCreate.setFormDefinitionId(formDefinitionId);
        submissionToCreate.setFormCode(formCode);
        submissionToCreate.setSubmittedBy("user@example.com");
        submissionToCreate.setStatus(SubmissionStatus.DRAFT);

        MvcResult createResult = mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submissionToCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        FormSubmissionDTO createdSubmission = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                FormSubmissionDTO.class);
        Long submissionId = createdSubmission.getId();

        // When - moving the status to SUBMITTED
        FormSubmissionDTO submissionToUpdate = new FormSubmissionDTO();
        submissionToUpdate.setFormDefinitionId(formDefinitionId);
        submissionToUpdate.setFormCode(formCode);
        submissionToUpdate.setSubmittedBy("user@example.com");
        submissionToUpdate.setStatus(SubmissionStatus.SUBMITTED);

        mockMvc.perform(put("/api/v1/form-submissions/{id}", submissionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submissionToUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissionId))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        // Then - the changes are persisted
        mockMvc.perform(get("/api/v1/form-submissions/{id}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void shouldDeleteFormSubmission() throws Exception {
        // Given - a submission
        FormSubmissionDTO submissionToCreate = new FormSubmissionDTO();
        submissionToCreate.setFormDefinitionId(formDefinitionId);
        submissionToCreate.setFormCode(formCode);
        submissionToCreate.setSubmittedBy("temp@example.com");
        submissionToCreate.setStatus(SubmissionStatus.DRAFT);

        MvcResult createResult = mockMvc.perform(post("/api/v1/form-submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submissionToCreate)))
                .andExpect(status().isCreated())
                .andReturn();

        FormSubmissionDTO createdSubmission = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                FormSubmissionDTO.class);
        Long submissionId = createdSubmission.getId();

        // When - deleting the submission
        mockMvc.perform(delete("/api/v1/form-submissions/{id}", submissionId))
                .andExpect(status().isNoContent());

        // Then - it is gone
        mockMvc.perform(get("/api/v1/form-submissions/{id}", submissionId))
                .andExpect(status().isNotFound());
    }
}
