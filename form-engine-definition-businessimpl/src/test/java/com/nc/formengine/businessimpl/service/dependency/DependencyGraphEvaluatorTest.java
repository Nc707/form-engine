package com.nc.formengine.businessimpl.service.dependency;

import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.nc.formengine.model.enums.DependencyCondition.EQUALS;
import static com.nc.formengine.model.enums.DependencyCondition.NOT_EQUALS;
import static com.nc.formengine.model.enums.DependencyEffect.HIDE;
import static com.nc.formengine.model.enums.DependencyEffect.OPTIONAL;
import static com.nc.formengine.model.enums.DependencyEffect.REQUIRE;
import static com.nc.formengine.model.enums.DependencyEffect.SHOW;
import static org.assertj.core.api.Assertions.assertThat;

class DependencyGraphEvaluatorTest {

    @Test
    void shouldReturnBaseStateWhenTheFormHasNoDependencies() {
        Map<String, FieldState> states = statesOf(
                List.of(field(1, "name", true), field(2, "nickname", false)),
                List.of(),
                Map.of());

        assertThat(states).containsOnlyKeys("name", "nickname");
        assertThat(states.get("name")).isEqualTo(new FieldState(true, true));
        assertThat(states.get("nickname")).isEqualTo(new FieldState(true, false));
    }

    @Test
    void shouldTreatAnUndeclaredRequiredFlagAsNotRequired() {
        // required is nullable in the database whenever a field was saved through the mapper.
        Map<String, FieldState> states = statesOf(
                List.of(field(1, "name", null)), List.of(), Map.of());

        assertThat(states.get("name")).isEqualTo(new FieldState(true, false));
    }

    @Test
    void shouldHideAFieldOnlyWhileItsHideConditionHolds() {
        List<FieldDefinitionDTO> fields = List.of(field(1, "country", false), field(2, "state", false));
        List<FieldDependencyDTO> dependencies = List.of(dependency(2, 1, EQUALS, "AR", HIDE));

        assertThat(statesOf(fields, dependencies, Map.of("country", "AR")).get("state").visible())
                .isFalse();
        assertThat(statesOf(fields, dependencies, Map.of("country", "UY")).get("state").visible())
                .isTrue();
        assertThat(statesOf(fields, dependencies, Map.of()).get("state").visible())
                .isTrue();
    }

    @Test
    void shouldHideAGatedFieldUntilOneOfItsShowConditionsHolds() {
        // A field targeted by SHOW starts hidden, otherwise SHOW could never change anything.
        List<FieldDefinitionDTO> fields = List.of(field(1, "status", false), field(2, "reason", false));
        List<FieldDependencyDTO> dependencies = List.of(dependency(2, 1, EQUALS, "REJECTED", SHOW));

        assertThat(statesOf(fields, dependencies, Map.of()).get("reason").visible()).isFalse();
        assertThat(statesOf(fields, dependencies, Map.of("status", "APPROVED")).get("reason").visible())
                .isFalse();
        assertThat(statesOf(fields, dependencies, Map.of("status", "REJECTED")).get("reason").visible())
                .isTrue();
        // A field with no SHOW dependency is not gated.
        assertThat(statesOf(fields, dependencies, Map.of()).get("status").visible()).isTrue();
    }

    @Test
    void shouldOpenAGateWhenAnyOfSeveralShowConditionsHolds() {
        List<FieldDefinitionDTO> fields =
                List.of(field(1, "a", false), field(2, "b", false), field(3, "target", false));
        List<FieldDependencyDTO> dependencies = List.of(
                dependency(3, 1, EQUALS, "yes", SHOW),
                dependency(3, 2, EQUALS, "yes", SHOW));

        assertThat(statesOf(fields, dependencies, Map.of("b", "yes")).get("target").visible()).isTrue();
        assertThat(statesOf(fields, dependencies, Map.of("b", "no")).get("target").visible()).isFalse();
    }

    @Test
    void shouldPreferHideOverShowWhenBothHold() {
        // The most restrictive effect wins, so the outcome does not depend on the order of the rows.
        List<FieldDefinitionDTO> fields =
                List.of(field(1, "a", false), field(2, "b", false), field(3, "target", false));
        List<FieldDependencyDTO> shownFirst = List.of(
                dependency(3, 1, EQUALS, "yes", SHOW),
                dependency(3, 2, EQUALS, "yes", HIDE));
        List<FieldDependencyDTO> hiddenFirst = List.of(
                dependency(3, 2, EQUALS, "yes", HIDE),
                dependency(3, 1, EQUALS, "yes", SHOW));
        Map<String, Object> values = Map.of("a", "yes", "b", "yes");

        assertThat(statesOf(fields, shownFirst, values).get("target").visible()).isFalse();
        assertThat(statesOf(fields, hiddenFirst, values).get("target").visible()).isFalse();
    }

    @Test
    void shouldRequireAFieldWhileItsRequireConditionHolds() {
        List<FieldDefinitionDTO> fields = List.of(field(1, "status", false), field(2, "reason", false));
        List<FieldDependencyDTO> dependencies = List.of(dependency(2, 1, EQUALS, "REJECTED", REQUIRE));

        assertThat(statesOf(fields, dependencies, Map.of("status", "REJECTED")).get("reason"))
                .isEqualTo(new FieldState(true, true));
        assertThat(statesOf(fields, dependencies, Map.of("status", "APPROVED")).get("reason"))
                .isEqualTo(new FieldState(true, false));
    }

    @Test
    void shouldReleaseARequiredFieldWhileItsOptionalConditionHolds() {
        List<FieldDefinitionDTO> fields = List.of(field(1, "kind", false), field(2, "taxId", true));
        List<FieldDependencyDTO> dependencies = List.of(dependency(2, 1, EQUALS, "PERSON", OPTIONAL));

        assertThat(statesOf(fields, dependencies, Map.of("kind", "PERSON")).get("taxId").required())
                .isFalse();
        assertThat(statesOf(fields, dependencies, Map.of("kind", "COMPANY")).get("taxId").required())
                .isTrue();
    }

    @Test
    void shouldPreferRequireOverOptionalWhenBothHold() {
        List<FieldDefinitionDTO> fields =
                List.of(field(1, "a", false), field(2, "b", false), field(3, "target", false));
        List<FieldDependencyDTO> optionalFirst = List.of(
                dependency(3, 1, EQUALS, "yes", OPTIONAL),
                dependency(3, 2, EQUALS, "yes", REQUIRE));
        List<FieldDependencyDTO> requiredFirst = List.of(
                dependency(3, 2, EQUALS, "yes", REQUIRE),
                dependency(3, 1, EQUALS, "yes", OPTIONAL));
        Map<String, Object> values = Map.of("a", "yes", "b", "yes");

        assertThat(statesOf(fields, optionalFirst, values).get("target").required()).isTrue();
        assertThat(statesOf(fields, requiredFirst, values).get("target").required()).isTrue();
    }

    @Test
    void shouldNeverReportAHiddenFieldAsRequired() {
        // What the parallel validation work relies on: a field the user cannot see cannot be demanded.
        List<FieldDefinitionDTO> fields = List.of(field(1, "kind", false), field(2, "taxId", true));
        List<FieldDependencyDTO> dependencies = List.of(dependency(2, 1, EQUALS, "PERSON", HIDE));

        assertThat(statesOf(fields, dependencies, Map.of("kind", "PERSON")).get("taxId"))
                .isEqualTo(new FieldState(false, false));
    }

    @Test
    void shouldResolveAChainOfDependencies() {
        // a controls b, b controls c.
        List<FieldDefinitionDTO> fields =
                List.of(field(1, "a", false), field(2, "b", false), field(3, "c", false));
        List<FieldDependencyDTO> dependencies = List.of(
                dependency(2, 1, EQUALS, "hide", HIDE),
                dependency(3, 2, EQUALS, "x", HIDE));

        Map<String, FieldState> visibleChain = statesOf(fields, dependencies, Map.of("a", "keep", "b", "x"));
        assertThat(visibleChain.get("b").visible()).isTrue();
        assertThat(visibleChain.get("c").visible()).isFalse();

        // Hiding b neutralises the rule b drives, even though an answer for b was submitted.
        Map<String, FieldState> brokenChain = statesOf(fields, dependencies, Map.of("a", "hide", "b", "x"));
        assertThat(brokenChain.get("b").visible()).isFalse();
        assertThat(brokenChain.get("c").visible()).isTrue();
    }

    @Test
    void shouldNotSatisfyNotEqualsWhenTheTriggerFieldIsHidden() {
        // Were absence to satisfy NOT_EQUALS, hiding status would make reason permanently required.
        List<FieldDefinitionDTO> fields =
                List.of(field(1, "toggle", false), field(2, "status", false), field(3, "reason", false));
        List<FieldDependencyDTO> dependencies = List.of(
                dependency(2, 1, EQUALS, "off", HIDE),
                dependency(3, 2, NOT_EQUALS, "APPROVED", REQUIRE));

        Map<String, FieldState> hidden =
                statesOf(fields, dependencies, Map.of("toggle", "off", "status", "REJECTED"));
        assertThat(hidden.get("status").visible()).isFalse();
        assertThat(hidden.get("reason").required()).isFalse();

        Map<String, FieldState> shown =
                statesOf(fields, dependencies, Map.of("toggle", "on", "status", "REJECTED"));
        assertThat(shown.get("status").visible()).isTrue();
        assertThat(shown.get("reason").required()).isTrue();
    }

    @Test
    void shouldIgnoreASelfReferencingDependency() {
        List<FieldDefinitionDTO> fields = List.of(field(1, "a", false));
        List<FieldDependencyDTO> dependencies = List.of(dependency(1, 1, EQUALS, "x", HIDE));

        EvaluationResult result =
                DependencyGraphEvaluator.evaluate(fields, dependencies, Map.of("a", "x"));

        assertThat(result.states().get("a")).isEqualTo(new FieldState(true, false));
        assertThat(result.cycles()).containsExactly(List.of("a"));
    }

    @Test
    void shouldIgnoreDependenciesInsideATwoFieldCycle() {
        List<FieldDefinitionDTO> fields = List.of(field(1, "a", false), field(2, "b", false));
        List<FieldDependencyDTO> dependencies = List.of(
                dependency(2, 1, EQUALS, "x", HIDE),
                dependency(1, 2, EQUALS, "y", HIDE));

        EvaluationResult result =
                DependencyGraphEvaluator.evaluate(fields, dependencies, Map.of("a", "x", "b", "y"));

        assertThat(result.states().get("a")).isEqualTo(new FieldState(true, false));
        assertThat(result.states().get("b")).isEqualTo(new FieldState(true, false));
        assertThat(result.cycles()).hasSize(1);
        assertThat(result.cycles().get(0)).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void shouldKeepFieldsDownstreamOfACycleWorking() {
        // A field outside a cycle must keep its rules even when its trigger sits inside one, and even
        // when its name sorts before every cycle member.
        List<FieldDefinitionDTO> fields = List.of(
                field(1, "a_total", false), field(2, "m_x", false),
                field(3, "m_y", false), field(4, "m_z", false));
        List<FieldDependencyDTO> dependencies = List.of(
                dependency(3, 2, EQUALS, "1", HIDE),
                dependency(4, 3, EQUALS, "1", HIDE),
                dependency(2, 4, EQUALS, "1", HIDE),
                dependency(1, 2, EQUALS, "0", HIDE));

        EvaluationResult result =
                DependencyGraphEvaluator.evaluate(fields, dependencies, Map.of("m_x", "0"));

        assertThat(result.states().get("a_total").visible()).isFalse();
        assertThat(result.states().get("m_x").visible()).isTrue();
        assertThat(result.states().get("m_y").visible()).isTrue();
        assertThat(result.states().get("m_z").visible()).isTrue();
        assertThat(result.cycles()).hasSize(1);
        assertThat(result.cycles().get(0)).containsExactlyInAnyOrder("m_x", "m_y", "m_z");
    }

    @Test
    void shouldKeepDependenciesEnteringACycleFromOutside() {
        List<FieldDefinitionDTO> fields =
                List.of(field(1, "flag", false), field(2, "a", false), field(3, "b", false));
        List<FieldDependencyDTO> dependencies = List.of(
                dependency(3, 2, EQUALS, "x", HIDE),
                dependency(2, 3, EQUALS, "y", HIDE),
                dependency(2, 1, EQUALS, "on", HIDE));

        EvaluationResult result = DependencyGraphEvaluator.evaluate(
                fields, dependencies, Map.of("flag", "on", "a", "x", "b", "y"));

        assertThat(result.states().get("a").visible()).isFalse();
        assertThat(result.states().get("b").visible()).isTrue();
    }

    @Test
    void shouldReportNoCyclesForAnAcyclicGraph() {
        EvaluationResult result = DependencyGraphEvaluator.evaluate(
                List.of(field(1, "a", false), field(2, "b", false)),
                List.of(dependency(2, 1, EQUALS, "x", HIDE)),
                Map.of("a", "x"));

        assertThat(result.cycles()).isEmpty();
    }

    @Test
    void shouldHandleDuplicateDependencyRows() {
        List<FieldDefinitionDTO> fields = List.of(field(1, "a", false), field(2, "b", false));
        List<FieldDependencyDTO> dependencies = List.of(
                dependency(2, 1, EQUALS, "x", HIDE),
                dependency(2, 1, EQUALS, "x", HIDE));

        EvaluationResult result =
                DependencyGraphEvaluator.evaluate(fields, dependencies, Map.of("a", "x"));

        assertThat(result.states().get("b").visible()).isFalse();
        assertThat(result.cycles()).isEmpty();
    }

    @Test
    void shouldIgnoreUnusableDependencies() {
        List<FieldDefinitionDTO> fields = List.of(field(1, "a", false), field(2, "b", false));
        List<FieldDependencyDTO> dependencies = List.of(
                dependency(2, 99, EQUALS, "x", HIDE),      // trigger outside the form
                dependency(99, 1, EQUALS, "x", HIDE),      // dependent outside the form
                dependency(2, 1, EQUALS, null, HIDE),      // nothing to compare against
                dependency(2, 1, null, "x", HIDE),         // no condition
                dependency(2, 1, EQUALS, "x", null));      // no effect

        Map<String, FieldState> states = statesOf(fields, dependencies, Map.of("a", "x"));

        assertThat(states.get("b")).isEqualTo(new FieldState(true, false));
    }

    @Test
    void shouldKeepTheLastFieldWhenTwoFieldsShareAName() {
        Map<String, FieldState> states = statesOf(
                List.of(field(1, "same", false), field(2, "same", true)),
                List.of(),
                Map.of());

        assertThat(states).containsOnlyKeys("same");
        assertThat(states.get("same").required()).isTrue();
    }

    @Test
    void shouldTolerateEmptyInput() {
        assertThat(DependencyGraphEvaluator.evaluate(List.of(), List.of(), Map.of()).states()).isEmpty();
        assertThat(DependencyGraphEvaluator.evaluate(null, null, null).states()).isEmpty();
    }

    private static Map<String, FieldState> statesOf(List<FieldDefinitionDTO> fields,
                                                    List<FieldDependencyDTO> dependencies,
                                                    Map<String, Object> values) {
        return DependencyGraphEvaluator.evaluate(fields, dependencies, values).states();
    }

    private static FieldDefinitionDTO field(long id, String name, Boolean required) {
        return FieldDefinitionDTO.builder().id(id).name(name).required(required).build();
    }

    private static FieldDependencyDTO dependency(long dependentFieldId,
                                                 long triggerFieldId,
                                                 DependencyCondition condition,
                                                 String triggerValue,
                                                 DependencyEffect effect) {
        return FieldDependencyDTO.builder()
                .dependentFieldId(dependentFieldId)
                .triggerFieldId(triggerFieldId)
                .condition(condition)
                .triggerValue(triggerValue)
                .effect(effect)
                .build();
    }
}
