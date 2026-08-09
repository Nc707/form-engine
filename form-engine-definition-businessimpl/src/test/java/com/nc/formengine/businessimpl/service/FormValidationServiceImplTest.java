package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.DependencyEvaluationService;
import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.validation.FieldValidationError;
import com.nc.formengine.model.validation.ValidationMode;
import com.nc.formengine.model.validation.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
            assertThat(error.restriction()).isEqualTo(RestrictionType.NOT_NULL);
            assertThat(error.message()).isEqualTo("El campo es obligatorio");
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
    void usesTheMessageTheAuthorWroteOnTheFieldsOwnPresenceRule() {
        FieldRestrictionDTO notNull = FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.NOT_NULL)
            .errorMessage("Necesitamos tu apodo")
            .build();
        givenFields(text("nickname", true, notNull));
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of(), ValidationMode.SUBMIT);

        assertThat(report.errors()).singleElement()
            .extracting(FieldValidationError::message).isEqualTo("Necesitamos tu apodo");
    }

    @Test
    void aDraftIsAllowedToBeIncomplete() {
        givenFields(text("nickname", true));
        givenNoDependencies();

        assertThat(service.validate(FORM_ID, Map.of(), ValidationMode.DRAFT).valid()).isTrue();
    }

    @Test
    void aDraftStillJudgesTheAnswersItDoesHave() {
        givenFields(text("nickname", true, minLength(3, "Muy corto")));
        givenNoDependencies();

        ValidationReport report = service.validate(FORM_ID, Map.of("nickname", "ab"), ValidationMode.DRAFT);

        assertThat(report.errors()).singleElement()
            .extracting(FieldValidationError::message).isEqualTo("Muy corto");
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
        givenFields(text("nickname", false, minLength(5, "Muy corto"), pattern("^[A-Z]+$")));
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
        FieldDefinitionDTO field = text("nickname", true, minLength(3, "Muy corto"));
        when(fieldDefinitionDao.findById(10L)).thenReturn(Optional.of(field));
        givenNoDependencies();

        assertThat(service.validateField(10L, "abcd", Map.of()).valid()).isTrue();
        assertThat(service.validateField(10L, "ab", Map.of()).errors())
            .extracting(FieldValidationError::message).containsExactly("Muy corto");
    }

    @Test
    void judgingASingleAnswerDemandsItWhenTheFieldIsRequired() {
        when(fieldDefinitionDao.findById(10L)).thenReturn(Optional.of(text("nickname", true)));
        givenNoDependencies();

        assertThat(service.validateField(10L, null, Map.of()).errors())
            .extracting(FieldValidationError::restriction).containsExactly(RestrictionType.NOT_NULL);
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

    private void givenFields(FieldDefinitionDTO... fields) {
        when(fieldDefinitionDao.findByFormDefinitionId(FORM_ID)).thenReturn(List.of(fields));
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
