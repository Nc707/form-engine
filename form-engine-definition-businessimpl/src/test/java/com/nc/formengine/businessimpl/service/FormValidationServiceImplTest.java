package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.DependencyEvaluationService;
import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.validation.FieldValidationError;
import com.nc.formengine.model.validation.ValidationErrorCause;
import com.nc.formengine.model.validation.ValidationMode;
import com.nc.formengine.model.validation.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Which rules the service decides to run, and what it makes of the answers.
 *
 * <p>The specifications are tested on their own in the model module; what is pinned down here is the
 * part only this class knows: presence, draft versus submit, and staying out of the way of fields the
 * user cannot see.
 */
class FormValidationServiceImplTest {

    private static final Long FORM_ID = 1L;

    private FieldDefinitionDao fieldDefinitionDao;
    private DependencyEvaluationService dependencyEvaluationService;
    private FormValidationServiceImpl service;

    @BeforeEach
    void setUp() {
        fieldDefinitionDao = mock(FieldDefinitionDao.class);
        dependencyEvaluationService = mock(DependencyEvaluationService.class);
        service = new FormValidationServiceImpl(fieldDefinitionDao, dependencyEvaluationService);
    }

    @Test
    void acceptsAnswersThatBreakNothing() {
        givenFields(text("nickname", true, minLength(3, null)));
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of("nickname", "abcd"), ValidationMode.SUBMIT);

        assertThat(report.valid()).isTrue();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void demandsAnAnswerForARequiredFieldOnSubmit() {
        givenFields(text("nickname", true));
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of(), ValidationMode.SUBMIT);

        assertThat(report.valid()).isFalse();
        assertThat(report.errors()).singleElement().satisfies(error -> {
            assertThat(error.fieldName()).isEqualTo("nickname");
            assertThat(error.fieldDefinitionId()).isEqualTo(10L);
            assertThat(error.cause()).isEqualTo(ValidationErrorCause.REQUIRED);
            // Not attributed to any restriction: no restriction asked for it.
            assertThat(error.restriction()).isNull();
            assertThat(error.message()).isEqualTo("This field is required");
        });
    }

    @Test
    void treatsBlanksAndEmptySelectionsAsNoAnswerAtAll() {
        givenFields(text("nickname", true));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, mapWithNull("nickname"), ValidationMode.SUBMIT).valid()).isFalse();
        assertThat(service.validate(FORM_ID, Map.of("nickname", "   "), ValidationMode.SUBMIT).valid()).isFalse();
        assertThat(service.validate(FORM_ID, Map.of("nickname", List.of()), ValidationMode.SUBMIT).valid()).isFalse();
    }

    @Test
    void usesTheMessageTheAuthorWroteOnTheFieldItself() {
        FieldDefinitionDTO field = text("nickname", true);
        field.setRequiredMessage("We need your nickname");
        givenFields(field);
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of(), ValidationMode.SUBMIT);

        assertThat(report.errors()).singleElement()
            .extracting(FieldValidationError::message).isEqualTo("We need your nickname");
    }

    /**
     * The old model needed a rule to be skipped here, because a {@code NOT_NULL} restriction demanded
     * a value that draft mode was in the middle of not demanding. Nothing to skip any more.
     */
    @Test
    void aDraftDemandsNothingEvenFromAFieldCarryingRules() {
        givenFields(text("nickname", true, minLength(3, "Too short"), pattern("^[A-Z]+$")));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of("nickname", ""), ValidationMode.DRAFT).valid()).isTrue();
    }

    /** An optional field left blank has nothing to judge, so its rules do not fire. */
    @Test
    void anOptionalFieldLeftBlankBreaksNoRules() {
        givenFields(text("nickname", false, minLength(3, "Too short"), pattern("^[A-Z]+$")));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of("nickname", "  "), ValidationMode.SUBMIT).valid()).isTrue();
    }

    @Test
    void aDraftIsAllowedToBeIncomplete() {
        givenFields(text("nickname", true));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of(), ValidationMode.DRAFT).valid()).isTrue();
    }

    @Test
    void aDraftStillJudgesTheAnswersItDoesHave() {
        givenFields(text("nickname", true, minLength(3, "Too short")));
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of("nickname", "ab"), ValidationMode.DRAFT);

        assertThat(report.errors()).singleElement()
            .extracting(FieldValidationError::message).isEqualTo("Too short");
    }

    /**
     * A missing answer breaks every rule at once; saying so eight times helps nobody.
     */
    @Test
    void reportsAMissingAnswerOnceAndNotOncePerRule() {
        givenFields(text("nickname", true, minLength(3, null), pattern("^[A-Z]+$")));
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of(), ValidationMode.SUBMIT);

        assertThat(report.errors()).hasSize(1);
    }

    @Test
    void reportsEveryRuleAnAnswerBreaks() {
        givenFields(text("nickname", false, minLength(5, "Too short"), pattern("^[A-Z]+$")));
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of("nickname", "ab"), ValidationMode.SUBMIT);

        assertThat(report.errors())
            .extracting(FieldValidationError::restriction)
            .containsExactly(RestrictionType.MIN_LENGTH, RestrictionType.PATTERN);
    }

    @Test
    void ignoresAFieldTheAnswersHaveHidden() {
        givenFields(text("nickname", true));
        when(dependencyEvaluationService.evaluate(anyLong(), any()))
            .thenReturn(Map.of("nickname", new FieldState(false, true)));

        assertThat(service.validate(FORM_ID, Map.of(), ValidationMode.SUBMIT).valid()).isTrue();
    }

    @Test
    void demandsAnAnswerForAFieldTheAnswersHaveMadeRequired() {
        givenFields(text("nickname", false));
        when(dependencyEvaluationService.evaluate(anyLong(), any()))
            .thenReturn(Map.of("nickname", new FieldState(true, true)));

        assertThat(service.validate(FORM_ID, Map.of(), ValidationMode.SUBMIT).valid()).isFalse();
    }

    @Test
    void groupsTheErrorsOfEachFieldSeparately() {
        givenFields(text("nickname", true), text("email", true));
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of(), ValidationMode.SUBMIT);

        assertThat(report.errorsFor("nickname")).hasSize(1);
        assertThat(report.errorsFor("email")).hasSize(1);
    }

    @Test
    void ignoresAnswersThatBelongToNoField() {
        givenFields(text("nickname", false));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of("stale", "value"), ValidationMode.SUBMIT).valid()).isTrue();
    }

    @Test
    void hasNothingToSayAboutAFormThatDoesNotExist() {
        when(fieldDefinitionDao.findByFormDefinitionId(anyLong())).thenReturn(List.of());

        assertThat(service.validate(FORM_ID, Map.of(), ValidationMode.SUBMIT).valid()).isTrue();
        assertThat(service.validate(null, Map.of(), ValidationMode.SUBMIT).valid()).isTrue();
    }

    @Test
    void treatsMissingAnswersAsAnEmptySetOfThem() {
        givenFields(text("nickname", true));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, null, ValidationMode.SUBMIT).valid()).isFalse();
    }

    @Test
    void judgesASingleAnswerAgainstTheRulesOfItsField() {
        FieldDefinitionDTO field = text("nickname", true, minLength(3, "Too short"));
        when(fieldDefinitionDao.findById(10L)).thenReturn(Optional.of(field));
        givenNoDependencies();

        assertThat(service.validateField(10L, "abcd", Map.of()).valid()).isTrue();
        assertThat(service.validateField(10L, "ab", Map.of()).errors())
            .extracting(FieldValidationError::message).containsExactly("Too short");
    }

    @Test
    void judgingASingleAnswerDemandsItWhenTheFieldIsRequired() {
        when(fieldDefinitionDao.findById(10L)).thenReturn(Optional.of(text("nickname", true)));
        givenNoDependencies();

        assertThat(service.validateField(10L, null, Map.of()).errors())
            .extracting(FieldValidationError::cause).containsExactly(ValidationErrorCause.REQUIRED);
    }

    @Test
    void judgingASingleAnswerSkipsAFieldTheAnswersHaveHidden() {
        when(fieldDefinitionDao.findById(10L)).thenReturn(Optional.of(text("nickname", true)));
        when(dependencyEvaluationService.evaluate(anyLong(), any()))
            .thenReturn(Map.of("nickname", new FieldState(false, true)));

        assertThat(service.validateField(10L, null, Map.of()).valid()).isTrue();
    }

    /**
     * Without the other answers there is nothing to resolve the dependencies against, so asking would
     * only produce a state computed from an empty form.
     */
    @Test
    void judgingASingleAnswerWithoutTheOtherAnswersDoesNotResolveDependencies() {
        when(fieldDefinitionDao.findById(10L)).thenReturn(Optional.of(text("nickname", true)));

        assertThat(service.validateField(10L, null, null).valid()).isFalse();
        verify(dependencyEvaluationService, never()).evaluate(anyLong(), any());
    }

    @Test
    void hasNothingToSayAboutAFieldThatDoesNotExist() {
        when(fieldDefinitionDao.findById(anyLong())).thenReturn(Optional.empty());

        assertThat(service.validateField(99L, "whatever", Map.of()).valid()).isTrue();
        assertThat(service.validateField(null, "whatever", Map.of()).valid()).isTrue();
    }

    // --- choices the field does not offer ---------------------------------------------------------

    /**
     * Nothing but the widget used to be stopping this. Through the API any string at all could be
     * stored as the answer to a SELECT and validated clean.
     */
    @Test
    void refusesAChoiceTheFieldDoesNotOffer() {
        givenFields(select("country", FieldType.SELECT, "ar", "uy"));
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of("country", "br"), ValidationMode.SUBMIT);

        assertThat(report.errors()).singleElement().satisfies(error -> {
            assertThat(error.cause()).isEqualTo(ValidationErrorCause.NOT_AN_OPTION);
            assertThat(error.restriction()).isNull();
            assertThat(error.message()).contains("br");
        });
    }

    @Test
    void acceptsAChoiceTheFieldDoesOffer() {
        givenFields(select("country", FieldType.SELECT, "ar", "uy"));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of("country", "ar"), ValidationMode.SUBMIT).valid()).isTrue();
    }

    @Test
    void judgesEverySelectionOfAMultiSelectSeparately() {
        givenFields(select("stack", FieldType.MULTI_SELECT, "java", "spring"));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of("stack", "java,spring"), ValidationMode.SUBMIT)
            .valid()).isTrue();
        assertThat(service.validate(FORM_ID, Map.of("stack", "java,cobol,fortran"), ValidationMode.SUBMIT)
            .errors()).hasSize(2);
    }

    /** The renderer validates live, before anything is stored, so the raw widget value arrives. */
    @Test
    void judgesAMultiSelectGivenAsACollectionToo() {
        givenFields(select("stack", FieldType.MULTI_SELECT, "java", "spring"));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of("stack", List.of("java")), ValidationMode.SUBMIT)
            .valid()).isTrue();
        assertThat(service.validate(FORM_ID, Map.of("stack", List.of("cobol")), ValidationMode.SUBMIT)
            .valid()).isFalse();
    }

    /**
     * A draft is allowed to be incomplete, not to hold an answer the field never offered — that one is
     * not going to become valid by finishing the form.
     */
    @Test
    void refusesAChoiceTheFieldDoesNotOfferEvenInADraft() {
        givenFields(select("country", FieldType.SELECT, "ar"));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of("country", "br"), ValidationMode.DRAFT).valid()).isFalse();
    }

    @Test
    void aSelectWithNoOptionsOffersNothingToChoose() {
        givenFields(select("country", FieldType.SELECT));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of("country", "ar"), ValidationMode.SUBMIT).valid()).isFalse();
    }

    private void givenFields(FieldDefinitionDTO... fields) {
        when(fieldDefinitionDao.findByFormDefinitionId(FORM_ID)).thenReturn(List.of(fields));
    }

    private FieldDefinitionDTO select(String name, FieldType type, String... optionValues) {
        List<FieldOptionDTO> options = new ArrayList<>();
        for (int index = 0; index < optionValues.length; index++) {
            options.add(FieldOptionDTO.builder()
                .label(optionValues[index]).value(optionValues[index]).orderIndex(index).build());
        }
        return FieldDefinitionDTO.builder()
            .id(12L)
            .formDefinitionId(FORM_ID)
            .name(name)
            .label(name)
            .type(type)
            .required(false)
            .restrictions(List.of())
            .options(options)
            .build();
    }

    private void givenNoDependencies() {
        when(dependencyEvaluationService.evaluate(anyLong(), any())).thenReturn(Map.of());
    }

    private FieldDefinitionDTO text(String name, boolean required, FieldRestrictionDTO... restrictions) {
        return FieldDefinitionDTO.builder()
            .id(name.equals("nickname") ? 10L : 11L)
            .formDefinitionId(FORM_ID)
            .name(name)
            .label(name)
            .type(FieldType.TEXT)
            .required(required)
            .restrictions(List.of(restrictions))
            .build();
    }

    private FieldRestrictionDTO minLength(int minLength, String errorMessage) {
        return FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MIN_LENGTH)
            .parameters(Map.of("minLength", minLength))
            .errorMessage(errorMessage)
            .orderIndex(0)
            .build();
    }

    private FieldRestrictionDTO pattern(String pattern) {
        return FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.PATTERN)
            .parameters(Map.of("pattern", pattern))
            .orderIndex(1)
            .build();
    }

    /** {@link Map#of} rejects null values, and a null answer is exactly what this checks. */
    private Map<String, Object> mapWithNull(String key) {
        Map<String, Object> values = new java.util.HashMap<>();
        values.put(key, null);
        return values;
    }
}
