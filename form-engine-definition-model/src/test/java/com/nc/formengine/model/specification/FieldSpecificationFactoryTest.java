package com.nc.formengine.model.specification;

import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.specification.impl.EmailSpecification;
import com.nc.formengine.model.specification.impl.MaxLengthSpecification;
import com.nc.formengine.model.specification.impl.MaxValueSpecification;
import com.nc.formengine.model.specification.impl.MinLengthSpecification;
import com.nc.formengine.model.specification.impl.MinValueSpecification;
import com.nc.formengine.model.specification.impl.PatternSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FieldSpecificationFactoryTest {

    private static final FieldContext TEXT = FieldContext.builder()
        .fieldType(FieldType.TEXT).fieldName("nickname").build();
    private static final FieldContext NUMBER = FieldContext.builder()
        .fieldType(FieldType.NUMBER).fieldName("age").build();

    @ParameterizedTest
    @EnumSource(RestrictionType.class)
    void buildsAnImplementationForEveryDeclaredRestrictionType(RestrictionType type) {
        FieldSpecification specification = FieldSpecificationFactory.from(wellFormed(type));

        assertThat(specification.getClass().getPackageName())
            .as("%s must map to a real specification and not to the permissive fallback", type)
            .isEqualTo(EmailSpecification.class.getPackageName());
    }

    @Test
    void buildsTheSpecificationMatchingTheRestrictionType() {
        assertThat(FieldSpecificationFactory.from(wellFormed(RestrictionType.EMAIL)))
            .isInstanceOf(EmailSpecification.class);
        assertThat(FieldSpecificationFactory.from(wellFormed(RestrictionType.MIN_LENGTH)))
            .isInstanceOf(MinLengthSpecification.class);
        assertThat(FieldSpecificationFactory.from(wellFormed(RestrictionType.MAX_LENGTH)))
            .isInstanceOf(MaxLengthSpecification.class);
        assertThat(FieldSpecificationFactory.from(wellFormed(RestrictionType.MIN_VALUE)))
            .isInstanceOf(MinValueSpecification.class);
        assertThat(FieldSpecificationFactory.from(wellFormed(RestrictionType.MAX_VALUE)))
            .isInstanceOf(MaxValueSpecification.class);
        assertThat(FieldSpecificationFactory.from(wellFormed(RestrictionType.PATTERN)))
            .isInstanceOf(PatternSpecification.class);
    }

    @Test
    void wiresTheParameterIntoTheSpecification() {
        FieldSpecification specification = FieldSpecificationFactory.from(wellFormed(RestrictionType.MIN_LENGTH));

        assertThat(specification.isSatisfiedBy("abcde", TEXT).isSatisfied()).isTrue();
        assertThat(specification.isSatisfiedBy("abcd", TEXT).isSatisfied()).isFalse();
    }

    @Test
    void prefersTheMessageTheAuthorWrote() {
        FieldRestrictionDTO restriction = FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MIN_LENGTH)
            .parameters(Map.of("minLength", 5))
            .errorMessage("At least five letters")
            .build();

        assertThat(FieldSpecificationFactory.from(restriction).isSatisfiedBy("ab", TEXT).getReasons())
            .containsExactly("At least five letters");
    }

    @Test
    void fallsBackToTheDefaultMessageWhenTheAuthorWroteOnlyBlanks() {
        FieldRestrictionDTO restriction = FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MIN_LENGTH)
            .parameters(Map.of("minLength", 5))
            .errorMessage("   ")
            .build();

        assertThat(FieldSpecificationFactory.from(restriction).isSatisfiedBy("ab", TEXT).getReasons())
            .containsExactly("This answer must be at least 5 characters long");
    }

    /**
     * The same {@code 5} arrives as an Integer, a Double or a String depending on whether it came
     * straight from a client or back out of the parameter table.
     */
    @Test
    void readsANumericParameterInAnyOfTheFormsItCanArriveIn() {
        for (Object parameter : new Object[] {5, 5.0, "5", " 5 "}) {
            FieldRestrictionDTO restriction = FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_LENGTH)
                .parameters(Map.of("minLength", parameter))
                .build();

            assertThat(FieldSpecificationFactory.from(restriction).isSatisfiedBy("abcd", TEXT).isSatisfied())
                .as("minLength given as %s", parameter)
                .isFalse();
        }
    }

    @Test
    void readsADecimalBoundOnANumericRestriction() {
        FieldRestrictionDTO restriction = FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MAX_VALUE)
            .parameters(Map.of("maxValue", "99.5"))
            .build();

        FieldSpecification specification = FieldSpecificationFactory.from(restriction);

        assertThat(specification.isSatisfiedBy(99.5, NUMBER).isSatisfied()).isTrue();
        assertThat(specification.isSatisfiedBy(99.6, NUMBER).isSatisfied()).isFalse();
    }

    @Test
    void acceptsEverythingWhenTheRestrictionIsMissing() {
        assertThat(FieldSpecificationFactory.from(null).isSatisfiedBy("anything", TEXT).isSatisfied())
            .isTrue();
    }

    @Test
    void acceptsEverythingWhenTheRestrictionHasNoType() {
        FieldRestrictionDTO restriction = FieldRestrictionDTO.builder()
            .parameters(Map.of("minLength", 5))
            .build();

        assertThat(FieldSpecificationFactory.from(restriction).isSatisfiedBy("a", TEXT).isSatisfied())
            .isTrue();
    }

    /**
     * A rule nobody can satisfy is worse than no rule: the form would be unsubmittable and the user
     * would have no way to work out why.
     */
    @Test
    void acceptsEverythingWhenTheParameterIsMissingOrUnreadable() {
        FieldRestrictionDTO noParameters = FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MIN_LENGTH)
            .build();
        FieldRestrictionDTO wrongKey = FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MIN_LENGTH)
            .parameters(Map.of("length", 5))
            .build();
        FieldRestrictionDTO notANumber = FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MIN_LENGTH)
            .parameters(Map.of("minLength", "cinco"))
            .build();
        FieldRestrictionDTO blankPattern = FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.PATTERN)
            .parameters(Map.of("pattern", "  "))
            .build();

        assertThat(FieldSpecificationFactory.from(noParameters).isSatisfiedBy("a", TEXT).isSatisfied()).isTrue();
        assertThat(FieldSpecificationFactory.from(wrongKey).isSatisfiedBy("a", TEXT).isSatisfied()).isTrue();
        assertThat(FieldSpecificationFactory.from(notANumber).isSatisfiedBy("a", TEXT).isSatisfied()).isTrue();
        assertThat(FieldSpecificationFactory.from(blankPattern).isSatisfiedBy("a", TEXT).isSatisfied()).isTrue();
    }

    private static FieldRestrictionDTO wellFormed(RestrictionType type) {
        Map<String, Object> parameters = switch (type) {
            case MIN_LENGTH -> Map.of("minLength", 5);
            case MAX_LENGTH -> Map.of("maxLength", 5);
            case MIN_VALUE -> Map.of("minValue", 5);
            case MAX_VALUE -> Map.of("maxValue", 5);
            case PATTERN -> Map.of("pattern", "^[A-Z]+$");
            case EMAIL -> Map.of();
        };

        return FieldRestrictionDTO.builder()
            .restrictionType(type)
            .parameters(parameters)
            .build();
    }
}
