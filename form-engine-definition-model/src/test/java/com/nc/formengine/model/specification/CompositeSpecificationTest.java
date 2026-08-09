package com.nc.formengine.model.specification;

import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The combinators of {@link FieldSpecification} and the composite
 * {@link FieldSpecificationFactory#composite(List)} builds out of them.
 */
class CompositeSpecificationTest {

    private static final FieldContext TEXT = FieldContext.builder()
        .fieldType(FieldType.TEXT).fieldName("nickname").build();

    private static final FieldSpecification ACCEPTS = (value, context) -> SpecificationResult.satisfied();
    private static final FieldSpecification REJECTS = (value, context) -> SpecificationResult.notSatisfied("no");

    @Test
    void andHoldsOnlyWhenBothHold() {
        assertThat(ACCEPTS.and(ACCEPTS).isSatisfiedBy("x", TEXT).isSatisfied()).isTrue();
        assertThat(ACCEPTS.and(REJECTS).isSatisfiedBy("x", TEXT).isSatisfied()).isFalse();
        assertThat(REJECTS.and(ACCEPTS).isSatisfiedBy("x", TEXT).isSatisfied()).isFalse();
    }

    @Test
    void andStopsAtTheFirstFailure() {
        List<String> evaluated = new ArrayList<>();
        FieldSpecification first = record(evaluated, "first", false);
        FieldSpecification second = record(evaluated, "second", true);

        first.and(second).isSatisfiedBy("x", TEXT);

        assertThat(evaluated).containsExactly("first");
    }

    @Test
    void orHoldsWhenEitherHolds() {
        assertThat(REJECTS.or(ACCEPTS).isSatisfiedBy("x", TEXT).isSatisfied()).isTrue();
        assertThat(ACCEPTS.or(REJECTS).isSatisfiedBy("x", TEXT).isSatisfied()).isTrue();
        assertThat(REJECTS.or(REJECTS).isSatisfiedBy("x", TEXT).isSatisfied()).isFalse();
    }

    @Test
    void notInvertsTheOutcome() {
        assertThat(ACCEPTS.not().isSatisfiedBy("x", TEXT).isSatisfied()).isFalse();
        assertThat(REJECTS.not().isSatisfiedBy("x", TEXT).isSatisfied()).isTrue();
    }

    @Test
    void compositeEnforcesEveryRestriction() {
        FieldSpecification specification = FieldSpecificationFactory.composite(List.of(
            restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), 0),
            restriction(RestrictionType.MAX_LENGTH, Map.of("maxLength", 5), 1)));

        assertThat(specification.isSatisfiedBy("abcd", TEXT).isSatisfied()).isTrue();
        assertThat(specification.isSatisfiedBy("ab", TEXT).isSatisfied()).isFalse();
        assertThat(specification.isSatisfiedBy("abcdef", TEXT).isSatisfied()).isFalse();
    }

    @Test
    void compositeReportsTheFirstFailureInOrderIndexOrder() {
        FieldRestrictionDTO tooShort = restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), 2);
        FieldRestrictionDTO wrongShape = restriction(RestrictionType.PATTERN, Map.of("pattern", "^[A-Z]+$"), 1);

        // Declared out of order on purpose: the composite has to sort them, not trust the list.
        FieldSpecification specification = FieldSpecificationFactory.composite(List.of(tooShort, wrongShape));

        assertThat(specification.isSatisfiedBy("ab", TEXT).getReasons())
            .containsExactly("El valor no cumple con el formato requerido");
    }

    @Test
    void compositeEvaluatesRestrictionsWithoutAnOrderLast() {
        FieldRestrictionDTO unordered = restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), null);
        FieldRestrictionDTO ordered = restriction(RestrictionType.PATTERN, Map.of("pattern", "^[A-Z]+$"), 7);

        FieldSpecification specification = FieldSpecificationFactory.composite(List.of(unordered, ordered));

        assertThat(specification.isSatisfiedBy("ab", TEXT).getReasons())
            .containsExactly("El valor no cumple con el formato requerido");
    }

    @Test
    void compositeAcceptsEverythingWhenThereIsNothingToEnforce() {
        assertThat(FieldSpecificationFactory.composite(null).isSatisfiedBy("x", TEXT).isSatisfied()).isTrue();
        assertThat(FieldSpecificationFactory.composite(List.of()).isSatisfiedBy("x", TEXT).isSatisfied()).isTrue();
        assertThat(FieldSpecificationFactory.composite(Arrays.asList((FieldRestrictionDTO) null))
            .isSatisfiedBy("x", TEXT).isSatisfied()).isTrue();
    }

    private static FieldSpecification record(List<String> evaluated, String name, boolean satisfied) {
        return (value, context) -> {
            evaluated.add(name);
            return satisfied ? SpecificationResult.satisfied() : SpecificationResult.notSatisfied(name);
        };
    }

    private static FieldRestrictionDTO restriction(RestrictionType type, Map<String, Object> parameters,
                                                   Integer orderIndex) {
        return FieldRestrictionDTO.builder()
            .restrictionType(type)
            .parameters(parameters)
            .orderIndex(orderIndex)
            .build();
    }
}
