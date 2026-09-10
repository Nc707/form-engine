package com.nc.formengine.model.rules;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.specification.RestrictionParameter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The invariants a definition has to satisfy, checked where they are defined.
 *
 * <p>These used to be asserted through a wrapper in the Vaadin builder, from the days when the
 * builder was the only thing enforcing them. It no longer is, and testing them through one consumer
 * left the rules themselves — the thing the REST path and the services also rely on — covered only
 * by accident.
 */
class DefinitionRulesTest {

    // --- forms -----------------------------------------------------------------------------

    @Test
    void aFormNeedsACodeAndATitle() {
        assertThat(DefinitionRules.checkForm("  ", "Title")).hasSize(1);
        assertThat(DefinitionRules.checkForm("job_application", " ")).hasSize(1);
        assertThat(DefinitionRules.checkForm("job_application", "Job application")).isEmpty();
    }

    @Test
    void aCodeHasToBeAnIdentifier() {
        assertThat(DefinitionRules.checkForm("Job Application", "t")).isNotEmpty();
        assertThat(DefinitionRules.checkForm("9lives", "t")).isNotEmpty();
        assertThat(DefinitionRules.checkForm("job-application", "t")).isNotEmpty();
        assertThat(DefinitionRules.checkForm("job_application_2", "t")).isEmpty();
    }

    // --- fields ----------------------------------------------------------------------------

    @Test
    void aFieldNeedsANameALabelAndAType() {
        var field = FieldDefinitionDTO.builder().build();

        assertThat(DefinitionRules.checkField(field, List.of())).hasSize(3);
    }

    @Test
    void twoFieldsCannotShareANameEvenInADifferentCase() {
        var existing = text("email");
        var added = text("EMAIL");

        assertThat(DefinitionRules.checkField(added, List.of(existing)))
                .anySatisfy(problem -> assertThat(problem).contains("already called"));
    }

    @Test
    void aFieldNameHasToBeAnIdentifier() {
        assertThat(DefinitionRules.checkField(text("home address"), List.of())).isNotEmpty();
        assertThat(DefinitionRules.checkField(text("2nd_line"), List.of())).isNotEmpty();
        assertThat(DefinitionRules.checkField(text("_home_address2"), List.of())).isEmpty();
    }

    // --- options ---------------------------------------------------------------------------

    @Test
    void aSelectNeedsAtLeastOneOption() {
        var field = text("country");
        field.setType(FieldType.SELECT);

        assertThat(DefinitionRules.checkField(field, List.of()))
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

        List<String> problems = DefinitionRules.checkField(field, List.of());

        assertThat(problems).anySatisfy(p -> assertThat(p).contains("share the value"));
        assertThat(problems).anySatisfy(p -> assertThat(p).contains("needs a value"));
    }

    @Test
    void onlyASelectMayCarryOptions() {
        var field = text("nickname");
        field.setOptions(new ArrayList<>(List.of(option("A", "a"))));

        assertThat(DefinitionRules.checkField(field, List.of()))
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

        assertThat(DefinitionRules.checkField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("accept every answer"));
    }

    @Test
    void aRuleThatDoesNotApplyToTheTypeIsRefused() {
        var field = text("age");
        field.setType(FieldType.NUMBER);
        field.setRestrictions(new ArrayList<>(List.of(restriction(RestrictionType.EMAIL, null))));

        assertThat(DefinitionRules.checkField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("does not apply"));
    }

    @Test
    void theSameRuleCannotBeSetTwice() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(
                restriction(RestrictionType.MIN_LENGTH, 2),
                restriction(RestrictionType.MIN_LENGTH, 4))));

        assertThat(DefinitionRules.checkField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("set twice"));
    }

    @Test
    void aMinimumAboveItsMaximumIsRefused() {
        var text = text("nickname");
        text.setRestrictions(new ArrayList<>(List.of(
                restriction(RestrictionType.MIN_LENGTH, 10),
                restriction(RestrictionType.MAX_LENGTH, 4))));
        assertThat(DefinitionRules.checkField(text, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("minimum length"));

        var number = text("age");
        number.setType(FieldType.NUMBER);
        number.setRestrictions(new ArrayList<>(List.of(
                restriction(RestrictionType.MIN_VALUE, 10),
                restriction(RestrictionType.MAX_VALUE, 4))));
        assertThat(DefinitionRules.checkField(number, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("minimum value"));
    }

    @Test
    void aNegativeLengthIsRefused() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(restriction(RestrictionType.MIN_LENGTH, -1))));

        assertThat(DefinitionRules.checkField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("negative"));
    }

    @Test
    void aPatternThatDoesNotCompileIsRefused() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(restriction(RestrictionType.PATTERN, "([a-z"))));

        assertThat(DefinitionRules.checkField(field, List.of()))
                .anySatisfy(problem -> assertThat(problem).contains("valid regular expression"));
    }

    @Test
    void aWellFormedFieldPasses() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(
                restriction(RestrictionType.MIN_LENGTH, 2),
                restriction(RestrictionType.MAX_LENGTH, 40))));

        assertThat(DefinitionRules.checkField(field, List.of(text("email")))).isEmpty();
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

        assertThat(DefinitionRules.checkDependency(dependency, List.of(field)))
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

        assertThat(DefinitionRules.checkDependency(dependency, List.of(trigger)))
                .anySatisfy(problem -> assertThat(problem).contains("field the rule affects"));
    }

    @Test
    void aDependencyWithoutATriggerValueCanNeverFire() {
        var dependency = wellFormedDependency();
        dependency.setTriggerValue("  ");

        assertThat(DefinitionRules.checkDependency(dependency, twoFields()))
                .anySatisfy(problem -> assertThat(problem).contains("value to compare"));
    }

    @Test
    void anOrderingConditionOnTextIsRefused() {
        var dependency = wellFormedDependency();
        dependency.setCondition(DependencyCondition.GREATER_THAN);

        assertThat(DefinitionRules.checkDependency(dependency, twoFields()))
                .anySatisfy(problem -> assertThat(problem).contains("does not apply"));
    }

    @Test
    void aWellFormedDependencyPasses() {
        assertThat(DefinitionRules.checkDependency(wellFormedDependency(), twoFields())).isEmpty();
    }

    @Test
    void conditionsAreOfferedOnlyWhereTheyMeanSomething() {
        assertThat(DefinitionRules.conditionsFor(FieldType.TEXT)).containsExactly(
                DependencyCondition.EQUALS, DependencyCondition.NOT_EQUALS, DependencyCondition.CONTAINS);
        assertThat(DefinitionRules.conditionsFor(FieldType.NUMBER)).containsExactly(
                DependencyCondition.EQUALS, DependencyCondition.NOT_EQUALS,
                DependencyCondition.GREATER_THAN, DependencyCondition.LESS_THAN);
        assertThat(DefinitionRules.conditionsFor(FieldType.BOOLEAN)).containsExactly(
                DependencyCondition.EQUALS, DependencyCondition.NOT_EQUALS);
        assertThat(DefinitionRules.conditionsFor(null)).isEmpty();
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

    /** Keyed the way {@link RestrictionParameter} says the type expects, which is what the rules read. */
    private static FieldRestrictionDTO restriction(RestrictionType type, Object value) {
        RestrictionParameter parameter = RestrictionParameter.of(type);
        Map<String, Object> parameters = parameter == null || !parameter.required() || value == null
                ? Map.of()
                : Map.of(parameter.key(), value);
        return FieldRestrictionDTO.builder()
                .restrictionType(type)
                .parameters(parameters)
                .build();
    }
}
