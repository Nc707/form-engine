package com.nc.formengine.flow;

import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.FieldSpecificationFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the builder's half of the restriction contract.
 *
 * <p>Worth more than its size suggests. {@code FieldSpecificationFactory} treats a parameter it
 * cannot find as a reason to accept every value, so a wrong key here does not throw, does not log
 * and does not fail a save — it produces a rule that the editor shows, the database stores, and the
 * validator ignores. Only a test that names the keys as literals, and one that runs a restriction
 * through the real factory, can catch that.
 */
class RestrictionParameterSpecTest {

    @Test
    void everyParameterKeyIsTheOneTheEngineReads() {
        assertThat(RestrictionParameterSpec.of(RestrictionType.MIN_LENGTH).key()).isEqualTo("minLength");
        assertThat(RestrictionParameterSpec.of(RestrictionType.MAX_LENGTH).key()).isEqualTo("maxLength");
        assertThat(RestrictionParameterSpec.of(RestrictionType.MIN_VALUE).key()).isEqualTo("minValue");
        assertThat(RestrictionParameterSpec.of(RestrictionType.MAX_VALUE).key()).isEqualTo("maxValue");
        assertThat(RestrictionParameterSpec.of(RestrictionType.PATTERN).key()).isEqualTo("pattern");
    }

    @Test
    void theOnlyParameterlessRuleTakesNothing() {
        assertThat(RestrictionParameterSpec.of(RestrictionType.EMAIL).hasParameter()).isFalse();
        assertThat(RestrictionParameterSpec.parameters(RestrictionType.EMAIL, "ignored")).isEmpty();
    }

    @Test
    void everyRestrictionTypeHasASpec() {
        for (RestrictionType type : RestrictionType.values()) {
            assertThat(RestrictionParameterSpec.of(type))
                    .as("no spec for %s", type)
                    .isNotNull();
        }
    }

    /** The whole point of the table: what the builder writes is what the engine enforces. */
    @Test
    void aRestrictionBuiltFromTheSpecActuallyRejects() {
        var restriction = FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_LENGTH)
                .parameters(RestrictionParameterSpec.parameters(RestrictionType.MIN_LENGTH, 3))
                .build();

        var specification = FieldSpecificationFactory.from(restriction);
        var context = FieldContext.builder().fieldType(FieldType.TEXT).fieldName("nickname").build();

        assertThat(specification.isSatisfiedBy("ab", context).isSatisfied()).isFalse();
        assertThat(specification.isSatisfiedBy("abc", context).isSatisfied()).isTrue();
    }

    @Test
    void aRestrictionWithTheWrongKeyWouldAcceptEverything() {
        // Documents the failure mode the rest of this class exists to prevent.
        var restriction = FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_LENGTH)
                .parameters(Map.of("value", 3))
                .build();

        var specification = FieldSpecificationFactory.from(restriction);
        var context = FieldContext.builder().fieldType(FieldType.TEXT).fieldName("nickname").build();

        assertThat(specification.isSatisfiedBy("ab", context).isSatisfied()).isTrue();
    }

    @Test
    void aLengthIsStoredAsAWholeNumber() {
        // A double would come back out of the database reading "5.0", because the mapper serialises
        // each parameter value on its own.
        assertThat(RestrictionParameterSpec.parameters(RestrictionType.MIN_LENGTH, 5.0))
                .containsExactly(Map.entry("minLength", 5));
        assertThat(RestrictionParameterSpec.parameters(RestrictionType.MIN_VALUE, 5))
                .containsExactly(Map.entry("minValue", 5.0));
    }

    @Test
    void aParameterSurvivesTheRoundTripItTakesThroughTheDatabase() {
        // Values come back as text or as a different number type depending on the route they took.
        assertThat(RestrictionParameterSpec.read(RestrictionType.MIN_LENGTH, Map.of("minLength", "7")))
                .isEqualTo(7);
        assertThat(RestrictionParameterSpec.read(RestrictionType.MAX_VALUE, Map.of("maxValue", 7)))
                .isEqualTo(7.0);
        assertThat(RestrictionParameterSpec.read(RestrictionType.PATTERN, Map.of("pattern", "^a$")))
                .isEqualTo("^a$");
    }

    @Test
    void readingAnAbsentParameterGivesNothing() {
        assertThat(RestrictionParameterSpec.read(RestrictionType.MIN_LENGTH, Map.of())).isNull();
        assertThat(RestrictionParameterSpec.read(RestrictionType.MIN_LENGTH, null)).isNull();
        assertThat(RestrictionParameterSpec.read(RestrictionType.EMAIL, Map.of("x", 1))).isNull();
    }

    /** The editor has to say so rather than offer an empty dropdown. */
    @Test
    void noRuleAtAllAppliesToTheTypesWithoutTextOrNumbers() {
        for (FieldType type : new FieldType[]{FieldType.DATE, FieldType.BOOLEAN,
                FieldType.SELECT, FieldType.MULTI_SELECT}) {
            assertThat(RestrictionParameterSpec.applicableTo(type))
                    .as("applicable to %s", type)
                    .isEmpty();
        }
    }

    @Test
    void textAndNumbersGetTheirOwnRulesInAStableOrder() {
        assertThat(RestrictionParameterSpec.applicableTo(FieldType.TEXT)).containsExactly(
                RestrictionType.MIN_LENGTH,
                RestrictionType.MAX_LENGTH,
                RestrictionType.PATTERN,
                RestrictionType.EMAIL);
        assertThat(RestrictionParameterSpec.applicableTo(FieldType.NUMBER)).containsExactly(
                RestrictionType.MIN_VALUE,
                RestrictionType.MAX_VALUE);
        assertThat(RestrictionParameterSpec.applicableTo(null)).isEmpty();
    }
}
