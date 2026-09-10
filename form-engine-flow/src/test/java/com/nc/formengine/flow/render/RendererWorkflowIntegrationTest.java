package com.nc.formengine.flow.render;

import com.nc.formengine.business.service.DependencyEvaluationService;
import com.nc.formengine.business.service.FieldDependencyService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.business.service.FormLayoutService;
import com.nc.formengine.business.service.FormValidationService;
import com.nc.formengine.business.service.LayoutResolutionService;
import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.business.service.FormSubmissionWorkflowService;
import com.nc.formengine.submission.business.service.SubmissionResult;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.nc.formengine.submission.model.exception.IllegalSubmissionTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The engine calls {@code FormRendererView} makes, over the form {@link PublishedFormFixture}
 * publishes.
 *
 * <p>The view itself is a thin wiring of these calls onto widgets, and the widget half is covered by
 * {@link LayoutGridRendererTest}. What is worth pinning down is that the sequence the view performs
 * behaves the way the view assumes: a field validated on blur, dependencies re-evaluated on a value
 * change, a draft that survives being resumed, and a rejected submit that says which fields to fix
 * without writing anything.
 */
// The context stays a web one — Vaadin's auto-configuration autowires a WebApplicationContext — but
// nothing is served: no servlet is initialised, so no front-end dev server is started either.
@SpringBootTest
class RendererWorkflowIntegrationTest {

    /** Whatever the application answers; the default provider answers this. */
    private static final String AUTHOR = "anonymous";

    @Autowired
    private FormDefinitionService formService;

    @Autowired
    private LayoutResolutionService layoutService;

    @Autowired
    private DependencyEvaluationService dependencyService;

    @Autowired
    private FormValidationService validationService;

    @Autowired
    private FormSubmissionWorkflowService workflowService;

    @Autowired
    private FormSubmissionService submissionService;

    @Autowired
    private FormLayoutService formLayoutService;

    @Autowired
    private FieldDependencyService fieldDependencyService;

    private FormDefinitionDTO form;

    @BeforeEach
    void setUp() {
        form = PublishedFormFixture.publish(formService, formLayoutService, fieldDependencyService);
    }

    @Test
    void theSeededFormComesWithALayoutTheRendererCanUse() {
        var layout = layoutService.resolveLayout(form.getId(), DeviceType.DESKTOP).orElseThrow();

        assertThat(layout.getFieldLayouts()).hasSize(form.getFields().size());
        // Not one field per row: the point of the layout is that the grid is visibly doing something.
        assertThat(layout.getFieldLayouts()).anySatisfy(placement ->
                assertThat(placement.getColspan()).isLessThan(12));
    }

    @Test
    void validatingOneAnswerReportsOnlyThatField() {
        var report = validationService.validateField(fieldId("email"), "not-an-email", values());

        assertThat(report.valid()).isFalse();
        assertThat(report.errors()).singleElement().satisfies(error -> {
            assertThat(error.fieldName()).isEqualTo("email");
            assertThat(error.restriction()).isEqualTo(RestrictionType.EMAIL);
        });
    }

    @Test
    void aConditionalFieldIsHiddenUntilItsTriggerSaysOtherwise() {
        Map<String, FieldState> hidden = dependencyService.evaluate(form.getId(), values());
        assertThat(hidden.get("visa_country")).isEqualTo(new FieldState(false, false));

        Map<String, Object> answers = values();
        answers.put("needs_visa", "true");
        Map<String, FieldState> shown = dependencyService.evaluate(form.getId(), answers);

        // Declared required, and only demanded once it is on screen.
        assertThat(shown.get("visa_country")).isEqualTo(new FieldState(true, true));
    }

    @Test
    void anIncompleteDraftIsSavedAnywayAndCanBeResumed() {
        SubmissionResult saved = workflowService.saveDraft(submission(null, Map.of(
                "full_name", "Ada Lovelace",
                "email", "ada@example.com")));

        assertThat(saved.persisted()).isTrue();
        // Every required field but these two is still empty, and a draft is not asked about those:
        // the view shows the report as advice, so there is nothing to warn about yet.
        assertThat(saved.report().valid()).isTrue();

        FormSubmissionDTO resumed = submissionService.findById(saved.submission().getId()).orElseThrow();
        assertThat(resumed.getStatus()).isEqualTo(SubmissionStatus.DRAFT);
        assertThat(answersOf(resumed)).containsEntry("full_name", "Ada Lovelace");
    }

    /**
     * The path the renderer's own Discard button takes. It belongs beside the form being filled in
     * because it is the respondent's decision about their own unsent draft — the answers are kept as a
     * record that someone started and stopped, but the draft is terminal and cannot be resumed.
     */
    @Test
    void aDraftCanBeDiscardedAndThenNoLongerResumed() {
        SubmissionResult saved = workflowService.saveDraft(submission(null, Map.of(
                "full_name", "Ada Lovelace",
                "email", "ada@example.com")));
        Long draftId = saved.submission().getId();

        FormSubmissionDTO discarded = workflowService.discard(draftId);

        assertThat(discarded.getStatus()).isEqualTo(SubmissionStatus.DISCARDED);
        assertThat(answersOf(submissionService.findById(draftId).orElseThrow()))
                .containsEntry("full_name", "Ada Lovelace");
        // Terminal: saving over it is refused, which is what the view relies on after navigating away.
        assertThatThrownBy(() -> workflowService.saveDraft(submission(draftId, Map.of(
                "full_name", "Someone else"))))
                .isInstanceOf(IllegalSubmissionTransitionException.class);
    }

    @Test
    void aDraftIsSavedEvenWhenAnAnswerItDoesCarryIsWrong() {
        SubmissionResult saved = workflowService.saveDraft(submission(null, Map.of(
                "full_name", "Ada Lovelace",
                "email", "not-an-email")));

        // Saved regardless: the errors are what is still to fix, not a refusal to store the work.
        assertThat(saved.persisted()).isTrue();
        assertThat(saved.report().errors())
                .extracting(error -> error.fieldName())
                .containsExactly("email");
    }

    @Test
    void aRejectedSubmitWritesNothingAndNamesEveryFieldToFix() {
        SubmissionResult rejected = workflowService.submit(submission(null, Map.of(
                "full_name", "Ada Lovelace",
                "email", "not-an-email")));

        assertThat(rejected.persisted()).isFalse();
        assertThat(rejected.submission().getId()).isNull();
        // What the error summary lists: the bad answer and the required ones left empty.
        assertThat(rejected.report().errors())
                .extracting(error -> error.fieldName())
                .contains("email", "years_experience", "role");
    }

    @Test
    void aDraftCanBeCompletedAndSubmitted() {
        Long draftId = workflowService.saveDraft(submission(null, Map.of(
                "full_name", "Ada Lovelace"))).submission().getId();

        SubmissionResult sent = workflowService.submit(submission(draftId, Map.of(
                "full_name", "Ada Lovelace",
                "email", "ada@example.com",
                "years_experience", "12",
                "role", "backend")));

        assertThat(sent.persisted()).isTrue();
        assertThat(sent.submission().getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        // The same row, moved on: submitting a draft does not start a second one.
        assertThat(sent.submission().getId()).isEqualTo(draftId);
        assertThat(answersOf(submissionService.findById(draftId).orElseThrow()))
                .containsEntry("email", "ada@example.com");
    }

    /** What the view sends: one row per field, answered or not. */
    private FormSubmissionDTO submission(Long id, Map<String, String> answers) {
        List<FieldSubmissionDTO> rows = new ArrayList<>();
        form.getFields().forEach(field -> rows.add(FieldSubmissionDTO.builder()
                .fieldDefinitionId(field.getId())
                .fieldName(field.getName())
                .value(answers.get(field.getName()))
                .build()));

        return FormSubmissionDTO.builder()
                .id(id)
                .formDefinitionId(form.getId())
                .formCode(form.getCode())
                .author(AUTHOR)
                .fieldSubmissions(rows)
                .build();
    }

    /** What the view passes around: every field name, with a null for the unanswered ones. */
    private Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>();
        form.getFields().forEach(field -> values.put(field.getName(), null));
        return values;
    }

    private Map<String, String> answersOf(FormSubmissionDTO submission) {
        Map<String, String> answers = new LinkedHashMap<>();
        submission.getFieldSubmissions()
                .forEach(answer -> answers.put(answer.getFieldName(), answer.getValue()));
        return answers;
    }

    private Long fieldId(String name) {
        return form.getFields().stream()
                .filter(field -> name.equals(field.getName()))
                .map(FieldDefinitionDTO::getId)
                .findFirst()
                .orElseThrow();
    }
}
