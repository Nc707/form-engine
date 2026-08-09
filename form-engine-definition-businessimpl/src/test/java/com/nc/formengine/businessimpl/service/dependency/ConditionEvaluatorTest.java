package com.nc.formengine.businessimpl.service.dependency;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.nc.formengine.model.enums.DependencyCondition.CONTAINS;
import static com.nc.formengine.model.enums.DependencyCondition.EQUALS;
import static com.nc.formengine.model.enums.DependencyCondition.GREATER_THAN;
import static com.nc.formengine.model.enums.DependencyCondition.LESS_THAN;
import static com.nc.formengine.model.enums.DependencyCondition.NOT_EQUALS;
import static org.assertj.core.api.Assertions.assertThat;

class ConditionEvaluatorTest {

    @Test
    void shouldMatchEqualStrings() {
        assertThat(ConditionEvaluator.matches(EQUALS, "yes", "yes")).isTrue();
        assertThat(ConditionEvaluator.matches(EQUALS, "no", "yes")).isFalse();
    }

    @Test
    void shouldCompareNumbersByValueRatherThanByText() {
        // A JSON answer arrives as a Double while the dependency stores text: 1.0 must match "1".
        assertThat(ConditionEvaluator.matches(EQUALS, 1.0d, "1")).isTrue();
        assertThat(ConditionEvaluator.matches(EQUALS, 1, "1.00")).isTrue();
        assertThat(ConditionEvaluator.matches(EQUALS, new BigDecimal("1.0"), "1")).isTrue();
        assertThat(ConditionEvaluator.matches(EQUALS, "007", "7")).isTrue();
    }

    @Test
    void shouldNegateEqualsWhenTheValueIsPresent() {
        assertThat(ConditionEvaluator.matches(NOT_EQUALS, "no", "yes")).isTrue();
        assertThat(ConditionEvaluator.matches(NOT_EQUALS, "yes", "yes")).isFalse();
    }

    @Test
    void shouldCompareOrderNumerically() {
        assertThat(ConditionEvaluator.matches(GREATER_THAN, 18, "17")).isTrue();
        // "9" > "10" as text, but not as numbers.
        assertThat(ConditionEvaluator.matches(GREATER_THAN, 9, "10")).isFalse();
        assertThat(ConditionEvaluator.matches(LESS_THAN, 9, "10")).isTrue();
        assertThat(ConditionEvaluator.matches(LESS_THAN, 18, "17")).isFalse();
    }

    @Test
    void shouldFallBackToLexicographicOrderForNonNumericValues() {
        assertThat(ConditionEvaluator.matches(GREATER_THAN, "2024-03-01", "2024-02-28")).isTrue();
        assertThat(ConditionEvaluator.matches(LESS_THAN, "2024-01-05", "2024-02-28")).isTrue();
    }

    @Test
    void shouldTreatContainsAsMembershipForMultiValuedAnswers() {
        assertThat(ConditionEvaluator.matches(CONTAINS, List.of("red", "blue"), "blue")).isTrue();
        assertThat(ConditionEvaluator.matches(CONTAINS, List.of("red", "blue"), "green")).isFalse();
        assertThat(ConditionEvaluator.matches(CONTAINS, new Object[]{"red", "blue"}, "red")).isTrue();
        // A single element must match in full, not as a substring.
        assertThat(ConditionEvaluator.matches(CONTAINS, List.of("blueish"), "blue")).isFalse();
    }

    @Test
    void shouldTreatContainsAsSubstringForSingleValues() {
        assertThat(ConditionEvaluator.matches(CONTAINS, "hello world", "lo wo")).isTrue();
        assertThat(ConditionEvaluator.matches(CONTAINS, "hello", "bye")).isFalse();
    }

    @Test
    void shouldNeverMatchWhenTheAnswerIsAbsent() {
        // Absence neutralises a trigger. NOT_EQUALS in particular must not fire, or hiding a trigger
        // field would switch on every rule it feeds with no way to switch them back off.
        for (Object absent : new Object[]{null, "", "   ", List.of(), new Object[0]}) {
            assertThat(ConditionEvaluator.matches(EQUALS, absent, "yes")).isFalse();
            assertThat(ConditionEvaluator.matches(NOT_EQUALS, absent, "yes")).isFalse();
            assertThat(ConditionEvaluator.matches(GREATER_THAN, absent, "1")).isFalse();
            assertThat(ConditionEvaluator.matches(LESS_THAN, absent, "1")).isFalse();
            assertThat(ConditionEvaluator.matches(CONTAINS, absent, "yes")).isFalse();
        }
    }

    @Test
    void shouldNeverMatchWhenTheDependencyDeclaresNoValue() {
        assertThat(ConditionEvaluator.matches(EQUALS, "yes", null)).isFalse();
        assertThat(ConditionEvaluator.matches(NOT_EQUALS, "yes", "  ")).isFalse();
        assertThat(ConditionEvaluator.matches(CONTAINS, "yes", null)).isFalse();
    }

    @Test
    void shouldMatchBooleanAnswersAsText() {
        assertThat(ConditionEvaluator.matches(EQUALS, true, "true")).isTrue();
        assertThat(ConditionEvaluator.matches(EQUALS, false, "true")).isFalse();
    }
}
