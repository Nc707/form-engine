package com.nc.formengine.rest.integration;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.business.service.FormSubmissionWorkflowService;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The answers of a submission survive a save, and a second save does not duplicate them.
 *
 * <p>Deliberately not {@code @Transactional}: sharing one persistence context with the services
 * would let a first-level cache hit pass for a row that was never written, which is exactly the bug
 * this covers.
 */
@SpringBootTest
class SubmissionAnswerPersistenceIntegrationTest {

    @Autowired
    private FormDefinitionService formDefinitionService;

    @Autowired
    private FormSubmissionWorkflowService workflowService;

    @Autowired
    private FormSubmissionService submissionService;

    private FormDefinitionDTO form;

    @BeforeEach
    void setUp() {
        form = formDefinitionService.create(FormDefinitionDTO.builder()
                .code("ANSWER_PERSISTENCE_" + System.nanoTime())
                .title("Answer persistence")
                .version(1)
                .fields(new ArrayList<>(List.of(
                        field("full_name", "Full name", 0),
                        field("city", "City", 1))))
                .build());
    }

    @Test
    void savingADraftPersistsItsAnswers() {
        FormSubmissionDTO draft = draftWith(
                answer("full_name", "Ada Lovelace"),
                answer("city", "London"));

        Long id = workflowService.saveDraft(draft).submission().getId();

        assertThat(answersOf(id))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "full_name", "Ada Lovelace",
                        "city", "London"));
    }

    @Test
    void savingADraftAgainUpdatesItsAnswersInsteadOfDuplicatingThem() {
        FormSubmissionDTO draft = draftWith(
                answer("full_name", "Ada Lovelace"),
                answer("city", "London"));
        FormSubmissionDTO saved = workflowService.saveDraft(draft).submission();

        // What the renderer does on a second "save draft": the same submission, edited.
        saved.getFieldSubmissions().stream()
                .filter(answer -> "city".equals(answer.getFieldName()))
                .forEach(answer -> answer.setValue("Paris"));
        workflowService.saveDraft(saved);

        assertThat(answersOf(saved.getId()))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "full_name", "Ada Lovelace",
                        "city", "Paris"));
    }

    @Test
    void anAnswerLeftOutOfASaveIsDeleted() {
        FormSubmissionDTO draft = draftWith(
                answer("full_name", "Ada Lovelace"),
                answer("city", "London"));
        FormSubmissionDTO saved = workflowService.saveDraft(draft).submission();

        saved.getFieldSubmissions().removeIf(answer -> "city".equals(answer.getFieldName()));
        workflowService.saveDraft(saved);

        assertThat(answersOf(saved.getId()))
                .containsExactlyInAnyOrderEntriesOf(Map.of("full_name", "Ada Lovelace"));
    }

    private Map<String, String> answersOf(Long submissionId) {
        return submissionService.findById(submissionId)
                .orElseThrow()
                .getFieldSubmissions()
                .stream()
                .collect(Collectors.toMap(FieldSubmissionDTO::getFieldName, FieldSubmissionDTO::getValue));
    }

    private FormSubmissionDTO draftWith(FieldSubmissionDTO... answers) {
        return FormSubmissionDTO.builder()
                .formDefinitionId(form.getId())
                .formCode(form.getCode())
                .author("tester")
                .fieldSubmissions(new ArrayList<>(List.of(answers)))
                .build();
    }

    private FieldSubmissionDTO answer(String fieldName, String value) {
        Long fieldId = form.getFields().stream()
                .filter(field -> fieldName.equals(field.getName()))
                .map(FieldDefinitionDTO::getId)
                .findFirst()
                .orElseThrow();
        return FieldSubmissionDTO.builder()
                .fieldDefinitionId(fieldId)
                .fieldName(fieldName)
                .value(value)
                .build();
    }

    private static FieldDefinitionDTO field(String name, String label, int orderIndex) {
        return FieldDefinitionDTO.builder()
                .name(name)
                .label(label)
                .type(FieldType.TEXT)
                .orderIndex(orderIndex)
                .required(false)
                .build();
    }
}
