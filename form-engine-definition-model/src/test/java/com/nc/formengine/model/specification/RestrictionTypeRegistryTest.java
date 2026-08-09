package com.nc.formengine.model.specification;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class RestrictionTypeRegistryTest {

    @ParameterizedTest
    @EnumSource(RestrictionType.class)
    void everyRestrictionTypeDeclaresWhereItApplies(RestrictionType type) {
        assertThat(RestrictionTypeRegistry.getApplicableFieldTypes(type))
            .as("%s applies nowhere, so it could never run", type)
            .isNotEmpty();
    }

    @Test
    void presenceIsCheckedOnEveryKindOfField() {
        assertThat(RestrictionTypeRegistry.getApplicableFieldTypes(RestrictionType.NOT_NULL))
            .containsExactlyInAnyOrder(FieldType.values());
    }

    @Test
    void textFieldsCarryTheTextRestrictions() {
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.TEXT))
            .containsExactlyInAnyOrder(RestrictionType.NOT_NULL, RestrictionType.NOT_EMPTY,
                RestrictionType.MIN_LENGTH, RestrictionType.MAX_LENGTH,
                RestrictionType.PATTERN, RestrictionType.EMAIL);
    }

    @Test
    void numberFieldsCarryTheNumericRestrictions() {
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.NUMBER))
            .containsExactlyInAnyOrder(RestrictionType.NOT_NULL,
                RestrictionType.MIN_VALUE, RestrictionType.MAX_VALUE);
    }

    @Test
    void fieldsWithNothingToMeasureOnlyCarryPresence() {
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.BOOLEAN))
            .containsExactly(RestrictionType.NOT_NULL);
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.DATE))
            .containsExactly(RestrictionType.NOT_NULL);
    }

    @Test
    void answersWhetherARestrictionAppliesToAFieldType() {
        assertThat(RestrictionTypeRegistry.isApplicable(RestrictionType.MIN_LENGTH, FieldType.TEXT)).isTrue();
        assertThat(RestrictionTypeRegistry.isApplicable(RestrictionType.MIN_LENGTH, FieldType.NUMBER)).isFalse();
        assertThat(RestrictionTypeRegistry.isApplicable(RestrictionType.MIN_VALUE, FieldType.NUMBER)).isTrue();
    }

    /**
     * The registry says a restriction applies to a field type; the specification decides whether the
     * value satisfies it. If the two disagreed, a restriction offered by the form editor would be
     * silently ignored at validation time.
     */
    @ParameterizedTest
    @EnumSource(RestrictionType.class)
    void agreesWithTheSpecificationsAboutWhereEachRestrictionApplies(RestrictionType type) {
        for (FieldType fieldType : FieldType.values()) {
            boolean registrySaysApplies = RestrictionTypeRegistry.isApplicable(type, fieldType);

            assertThat(applicableTypesOf(type).contains(fieldType))
                .as("%s on a %s field", type, fieldType)
                .isEqualTo(registrySaysApplies);
        }
    }

    @SuppressWarnings("unchecked")
    private static java.util.Set<FieldType> applicableTypesOf(RestrictionType type) {
        String className = switch (type) {
            case NOT_NULL -> "NotNull";
            case NOT_EMPTY -> "NotEmpty";
            case MIN_LENGTH -> "MinLength";
            case MAX_LENGTH -> "MaxLength";
            case MIN_VALUE -> "MinValue";
            case MAX_VALUE -> "MaxValue";
            case PATTERN -> "Pattern";
            case EMAIL -> "Email";
        };

        try {
            Class<?> specification = Class.forName(
                "com.nc.formengine.model.specification.impl." + className + "Specification");
            return (java.util.Set<FieldType>) specification.getMethod("getApplicableTypes").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                "No specification implements " + type + "; the registry declares it as supported", e);
        }
    }
}
