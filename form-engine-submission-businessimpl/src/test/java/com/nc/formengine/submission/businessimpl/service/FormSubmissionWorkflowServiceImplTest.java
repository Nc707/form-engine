package com.nc.formengine.submission.businessimpl.service;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.business.service.FormValidationService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.validation.FieldValidationError;
import com.nc.formengine.model.validation.ValidationMode;
import com.nc.formengine.model.validation.ValidationReport;
import com.nc.formengine.submission.business.service.SubmissionResult;
import com.nc.formengine.submission.data.dao.FormSubmissionDao;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.nc.formengine.submission.model.exception.FormNotAcceptingSubmissionsException;
import com.nc.formengine.submission.model.exception.FormSubmissionNotFoundException;
import com.nc.formengine.submission.model.exception.IllegalSubmissionTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Which moves the submission lifecycle allows, and what it refuses to write.
 *
 * <p>The validation rules themselves belong to the definition slice and are tested there; what
 * matters here is that a rejected submit leaves nothing behind, that a draft is judged leniently
 * while a submit is not, and that a form nobody published cannot be answered.
 */
class FormSubmissionWorkflowServiceImplTest {

    private static final Long FORM_ID = 1L;
    private static final Long SUBMISSION_ID = 10L;

    private FormSubmissionDao formSubmissionDao;
    private FormValidationService formValidationService;
    private FormDefinitionService formDefinitionService;
    private FieldDefinitionService fieldDefinitionService;
    private FormSubmissionWorkflowServiceImpl service;

    @BeforeEach
    void setUp() {
        formSubmissionDao = mock(FormSubmissionDao.class);
        formValidationService = mock(FormValidationService.class);
        formDefinitionService = mock(FormDefinitionService.class);
        fieldDefinitionService = mock(FieldDefinitionService.class);
        service = new FormSubmissionWorkflowServiceImpl(
                formSubmissionDao, formValidationService, formDefinitionService, fieldDefinitionService);

        when(formSubmissionDao.save(any(FormSubmissionDTO.class))).thenAnswer(call -> call.getArgument(0));
        publishedForm();
        when(formValidationService.validate(anyLong(), any(), any())).thenReturn(ValidationReport.noErrors());
    }

    // ---------- drafts ----------

    @Test
    void aDraftIsSavedAndJudgedLeniently() {
        SubmissionResult result = service.saveDraft(submission(null, null));

        assertThat(result.persisted()).isTrue();
        assertThat(result.submission().getStatus()).isEqualTo(SubmissionStatus.DRAFT);
        verify(formValidationService).validate(eq(FORM_ID), any(), eq(ValidationMode.DRAFT));
    }

    /** Being incomplete is the normal state of a draft, so errors do not stop it being saved. */
    @Test
    void aDraftIsSavedEvenWhenItDoesNotValidate() {
        when(formValidationService.validate(anyLong(), any(), any())).thenReturn(someErrors());

        SubmissionResult result = service.saveDraft(submission(null, null));

        assertThat(result.persisted()).isTrue();
        assertThat(result.valid()).isFalse();
        verify(formSubmissionDao).save(any());
    }

    @Test
    void aSubmittedFormCannotBeEditedAsADraft() {
        storedWithStatus(SubmissionStatus.SUBMITTED);

        assertThatThrownBy(() -> service.saveDraft(submission(SUBMISSION_ID, null)))
                .isInstanceOf(IllegalSubmissionTransitionException.class);

        verify(formSubmissionDao, never()).save(any());
    }

    // ---------- submitting ----------

    @Test
    void aValidSubmissionIsStoredAsSubmitted() {
        SubmissionResult result = service.submit(submission(null, null));

        assertThat(result.persisted()).isTrue();
        assertThat(result.submission().getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(result.submission().getSubmittedAt()).isNotNull();
        verify(formValidationService).validate(eq(FORM_ID), any(), eq(ValidationMode.SUBMIT));
    }

    /** The point of validating at submit time: a stored SUBMITTED row is one the form would accept. */
    @Test
    void anInvalidSubmissionWritesNothing() {
        when(formValidationService.validate(anyLong(), any(), any())).thenReturn(someErrors());

        SubmissionResult result = service.submit(submission(null, null));

        assertThat(result.persisted()).isFalse();
        assertThat(result.valid()).isFalse();
        assertThat(result.report().errorsFor("full_name")).hasSize(1);
        verify(formSubmissionDao, never()).save(any());
    }

    @Test
    void aDraftFormCannotBeSubmittedTo() {
        formWithStatus(FormDefinitionStatus.DRAFT);

        assertThatThrownBy(() -> service.submit(submission(null, null)))
                .isInstanceOf(FormNotAcceptingSubmissionsException.class);

        verify(formSubmissionDao, never()).save(any());
    }

    @Test
    void anArchivedFormCannotBeSubmittedTo() {
        formWithStatus(FormDefinitionStatus.ARCHIVED);

        assertThatThrownBy(() -> service.submit(submission(null, null)))
                .isInstanceOf(FormNotAcceptingSubmissionsException.class);
    }

    @Test
    void aFormThatIsNotThereCannotBeSubmittedTo() {
        when(formDefinitionService.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(submission(null, null)))
                .isInstanceOf(FormNotAcceptingSubmissionsException.class);
    }

    @Test
    void aSubmittedFormCannotBeSubmittedAgain() {
        storedWithStatus(SubmissionStatus.SUBMITTED);

        assertThatThrownBy(() -> service.submit(submission(SUBMISSION_ID, null)))
                .isInstanceOf(IllegalSubmissionTransitionException.class);
    }

    @Test
    void aStoredDraftCanBeSubmitted() {
        storedWithStatus(SubmissionStatus.DRAFT);

        SubmissionResult result = service.submit(submission(SUBMISSION_ID, null));

        assertThat(result.persisted()).isTrue();
        assertThat(result.submission().getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    // ---------- cancelling ----------

    @Test
    void aDraftCanBeCanceled() {
        storedWithStatus(SubmissionStatus.DRAFT);

        assertThat(service.cancel(SUBMISSION_ID).getStatus()).isEqualTo(SubmissionStatus.CANCELED);
    }

    @Test
    void aSubmittedFormCanBeCanceled() {
        storedWithStatus(SubmissionStatus.SUBMITTED);

        assertThat(service.cancel(SUBMISSION_ID).getStatus()).isEqualTo(SubmissionStatus.CANCELED);
    }

    @Test
    void cancellingTwiceIsRefused() {
        storedWithStatus(SubmissionStatus.CANCELED);

        assertThatThrownBy(() -> service.cancel(SUBMISSION_ID))
                .isInstanceOf(IllegalSubmissionTransitionException.class);

        verify(formSubmissionDao, never()).save(any());
    }

    @Test
    void cancellingSomethingThatIsNotThereIsRefused() {
        when(formSubmissionDao.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(SUBMISSION_ID))
                .isInstanceOf(FormSubmissionNotFoundException.class);
    }

    // ---------- the answers handed to the validator ----------

    @Test
    void answersAreKeyedByFieldName() {
        service.submit(submission(null, List.of(answer(100L, "full_name", "Ada"))));

        assertThat(captureAnswers()).containsEntry("full_name", "Ada");
    }

    /** fieldName is denormalised and callers legitimately omit it, so it is looked up instead. */
    @Test
    void aMissingFieldNameIsResolvedFromTheDefinition() {
        when(fieldDefinitionService.findById(100L)).thenReturn(Optional.of(
                FieldDefinitionDTO.builder().id(100L).name("full_name").build()));

        service.submit(submission(null, List.of(answer(100L, null, "Ada"))));

        assertThat(captureAnswers()).containsEntry("full_name", "Ada");
    }

    @Test
    void anAnswerToAFieldThatNoLongerExistsIsDropped() {
        when(fieldDefinitionService.findById(anyLong())).thenReturn(Optional.empty());

        service.submit(submission(null, List.of(answer(404L, null, "orphan"))));

        assertThat(captureAnswers()).isEmpty();
    }

    // ---------- helpers ----------

    private void publishedForm() {
        formWithStatus(FormDefinitionStatus.PUBLISHED);
    }

    private void formWithStatus(FormDefinitionStatus status) {
        when(formDefinitionService.findById(FORM_ID)).thenReturn(Optional.of(FormDefinitionDTO.builder()
                .id(FORM_ID).code("FORM").title("Form").version(1).status(status).build()));
    }

    private void storedWithStatus(SubmissionStatus status) {
        when(formSubmissionDao.findById(SUBMISSION_ID)).thenReturn(Optional.of(FormSubmissionDTO.builder()
                .id(SUBMISSION_ID)
                .formDefinitionId(FORM_ID)
                .submittedBy("ada")
                .status(status)
                .build()));
    }

    private FormSubmissionDTO submission(Long id, List<FieldSubmissionDTO> answers) {
        FormSubmissionDTO submission = FormSubmissionDTO.builder()
                .id(id)
                .formDefinitionId(FORM_ID)
                .formCode("FORM")
                .submittedBy("ada")
                .build();
        if (answers != null) {
            submission.setFieldSubmissions(answers);
        }
        return submission;
    }

    private FieldSubmissionDTO answer(Long fieldDefinitionId, String fieldName, String value) {
        return FieldSubmissionDTO.builder()
                .fieldDefinitionId(fieldDefinitionId)
                .fieldName(fieldName)
                .value(value)
                .build();
    }

    private ValidationReport someErrors() {
        return ValidationReport.of(List.of(
                new FieldValidationError("full_name", 100L, RestrictionType.NOT_NULL, "is required")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureAnswers() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(formValidationService).validate(eq(FORM_ID), captor.capture(), any());
        return captor.getValue();
    }
}
