package com.nc.formengine.ui.builder;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BuilderValidationTest {

    // --- forms -----------------------------------------------------------------------------

    @Test
    void aFormNeedsACodeAndATitle() {
        assertThat(BuilderValidation.validateNewForm("  ", "Title")).hasSize(1);
        assertThat(BuilderValidation.validateNewForm("job_application", " ")).hasSize(1);
        assertThat(BuilderValidation.validateNewForm("job_application", "Job application")).isEmpty();
    }

    @Test
    void aCodeHasToBeAnIdentifier() {
        assertThat(BuilderValidation.validateNewForm("Job Application", "t")).isNotEmpty();
        assertThat(BuilderValidation.validateNewForm("9lives", "t")).isNotEmpty();
        assertThat(BuilderValidation.validateNewForm("job-application", "t")).isNotEmpty();
        assertThat(BuilderValidation.validateNewForm("job_application_2", "t")).isEmpty();
    }

    // --- fields ----------------------------------------------------------------------------

    @Test
    void aFieldNeedsANameALabelAndAType() {
        var field = FieldDefinitionDTO.builder().build();

        assertThat(BuilderValidation.validateField(field, List.of())).hasSize(3);
    }

    @Test
    void twoFieldsCannotShareANameEvenInADifferentCase() {
        var existing = text("email");
        var added = text("EMAIL");

        assertThat(BuilderValidation.validateField(added, List.of(existing)))
                .anySatisfy(problem -> assertThat(problem).contains("already called"));
    }

    @Test
    void aFieldNameHasToBeAnIdentifier() {
        assertThat(BuilderValidation.validateField(text("home address"), List.of())).isNotEmpty();
        assertThat(BuilderValidation.validateField(text("2nd_line"), List.of())).isNotEmpty();
        assertThat(BuilderValidation.validateField(text("_home_address2"), List.of())).isEmpty();
    }

    // --- options ---------------------------------------------------------------------------

    @Test
    void aSelectNeedsAtLeastOneOption() {
        var field = text("country");
        field.setType(FieldType.SELECT);

        assertThat(BuilderValidation.validateField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("at least one option"));
    }

    @Test
    void optionValuesHaveToBeUniqueAndFilledIn() {
        var field = text("country");
        field.setType(FieldType.MULTI_SELECT);
        field.setOptions(new ArrayList<>(List.of(
                option("Argentina", "ar"),
                option("Australia", "ar"),
                option("Austria", " "))));

        List<String> problems = BuilderValidation.validateField(field, List.of());

        assertThat(problems).anySatisfy(p -> assertThat(p).contains("share the value"));
        assertThat(problems).anySatisfy(p -> assertThat(p).contains("needs a value"));
    }

    @Test
    void onlyASelectMayCarryOptions() {
        var field = text("nickname");
        field.setOptions(new ArrayList<>(List.of(option("A", "a"))));

        assertThat(BuilderValidation.validateField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("Only a select field"));
    }

    // --- restrictions ----------------------------------------------------------------------

    @Test
    void aRuleWithoutItsParameterIsRefusedRatherThanSilentlyIgnored() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_LENGTH)
                .parameters(Map.of())
                .build())));

        assertThat(BuilderValidation.validateField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("accept every answer"));
    }

    @Test
    void aRuleThatDoesNotApplyToTheTypeIsRefused() {
        var field = text("age");
        field.setType(FieldType.NUMBER);
        field.setRestrictions(new ArrayList<>(List.of(restriction(RestrictionType.EMAIL, null))));

        assertThat(BuilderValidation.validateField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("does not apply"));
    }

    @Test
    void theSameRuleCannotBeSetTwice() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(
                restriction(RestrictionType.MIN_LENGTH, 2),
                restriction(RestrictionType.MIN_LENGTH, 4))));

        assertThat(BuilderValidation.validateField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("set twice"));
    }

    @Test
    void aMinimumAboveItsMaximumIsRefused() {
        var text = text("nickname");
        text.setRestrictions(new ArrayList<>(List.of(
                restriction(RestrictionType.MIN_LENGTH, 10),
                restriction(RestrictionType.MAX_LENGTH, 4))));
        assertThat(BuilderValidation.validateField(text, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("minimum length"));

        var number = text("age");
        number.setType(FieldType.NUMBER);
        number.setRestrictions(new ArrayList<>(List.of(
                restriction(RestrictionType.MIN_VALUE, 10),
                restriction(RestrictionType.MAX_VALUE, 4))));
        assertThat(BuilderValidation.validateField(number, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("minimum value"));
    }

    @Test
    void aNegativeLengthIsRefused() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(restriction(RestrictionType.MIN_LENGTH, -1))));

        assertThat(BuilderValidation.validateField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("negative"));
    }

    @Test
    void aPatternThatDoesNotCompileIsRefused() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(restriction(RestrictionType.PATTERN, "([a-z"))));

        assertThat(BuilderValidation.validateField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("valid regular expression"));
    }

    @Test
    void aWellFormedFieldPasses() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(
                restriction(RestrictionType.MIN_LENGTH, 2),
                restriction(RestrictionType.MAX_LENGTH, 40))));

        assertThat(BuilderValidation.validateField(field, List.of(text("email")))).isEmpty();
    }

    // --- dependencies ----------------------------------------------------------------------

    @Test
    void aFieldCannotDependOnItself() {
        var field = text("visa");
        field.setId(1L);

        var dependency = FieldDependencyDTO.builder()
                .triggerFieldId(1L).dependentFieldId(1L)
                .condition(DependencyCondition.EQUALS).effect(DependencyEffect.SHOW)
                .triggerValue("yes")
                .build();

        assertThat(BuilderValidation.validateDependency(dependency, List.of(field)))
                .anySatisfy(problem -> assertThat(problem).contains("depend on itself"));
    }

    @Test
    void aDependencyOnAFieldThatIsGoneIsRefused() {
        var trigger = text("visa");
        trigger.setId(1L);

        var dependency = FieldDependencyDTO.builder()
                .triggerFieldId(1L).dependentFieldId(99L)
                .condition(DependencyCondition.EQUALS).effect(DependencyEffect.SHOW)
                .triggerValue("yes")
                .build();

        assertThat(BuilderValidation.validateDependency(dependency, List.of(trigger)))
                .anySatisfy(problem -> assertThat(problem).contains("field the rule affects"));
    }

    @Test
    void aDependencyWithoutATriggerValueCanNeverFire() {
        var dependency = wellFormedDependency();
        dependency.setTriggerValue("  ");

        assertThat(BuilderValidation.validateDependency(dependency, twoFields()))
                .anySatisfy(problem -> assertThat(problem).contains("value to compare"));
    }

    @Test
    void anOrderingConditionOnTextIsRefused() {
        var dependency = wellFormedDependency();
        dependency.setCondition(DependencyCondition.GREATER_THAN);

        assertThat(BuilderValidation.validateDependency(dependency, twoFields()))
                .anySatisfy(problem -> assertThat(problem).contains("does not apply"));
    }

    @Test
    void aWellFormedDependencyPasses() {
        assertThat(BuilderValidation.validateDependency(wellFormedDependency(), twoFields())).isEmpty();
    }

    @Test
    void conditionsAreOfferedOnlyWhereTheyMeanSomething() {
        assertThat(BuilderValidation.conditionsFor(FieldType.TEXT)).containsExactly(
                DependencyCondition.EQUALS, DependencyCondition.NOT_EQUALS, DependencyCondition.CONTAINS);
        assertThat(BuilderValidation.conditionsFor(FieldType.NUMBER)).containsExactly(
                DependencyCondition.EQUALS, DependencyCondition.NOT_EQUALS,
                DependencyCondition.GREATER_THAN, DependencyCondition.LESS_THAN);
        assertThat(BuilderValidation.conditionsFor(FieldType.BOOLEAN)).containsExactly(
                DependencyCondition.EQUALS, DependencyCondition.NOT_EQUALS);
        assertThat(BuilderValidation.conditionsFor(null)).isEmpty();
    }

    // --- fixtures --------------------------------------------------------------------------

    private static List<FieldDefinitionDTO> twoFields() {
        var trigger = text("visa");
        trigger.setId(1L);
        var dependent = text("passport");
        dependent.setId(2L);
        return List.of(trigger, dependent);
    }

    private static FieldDependencyDTO wellFormedDependency() {
        return FieldDependencyDTO.builder()
                .triggerFieldId(1L).dependentFieldId(2L)
                .condition(DependencyCondition.EQUALS).effect(DependencyEffect.SHOW)
                .triggerValue("yes")
                .build();
    }

    private static FieldDefinitionDTO text(String name) {
        return FieldDefinitionDTO.builder()
                .name(name).label(name).type(FieldType.TEXT).required(false).orderIndex(0)
                .build();
    }

    private static FieldOptionDTO option(String label, String value) {
        return FieldOptionDTO.builder().label(label).value(value).orderIndex(0).build();
    }

    private static FieldRestrictionDTO restriction(RestrictionType type, Object value) {
        return FieldRestrictionDTO.builder()
                .restrictionType(type)
                .parameters(RestrictionParameterSpec.parameters(type, value))
                .build();
    }
}
