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
    void textFieldsCarryTheTextRestrictions() {
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.TEXT))
            .containsExactlyInAnyOrder(RestrictionType.MIN_LENGTH, RestrictionType.MAX_LENGTH,
                RestrictionType.PATTERN, RestrictionType.EMAIL);
    }

    @Test
    void numberFieldsCarryTheNumericRestrictions() {
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.NUMBER))
            .containsExactlyInAnyOrder(RestrictionType.MIN_VALUE, RestrictionType.MAX_VALUE);
    }

    /**
     * There is nothing to configure on these. A date is a date, a boolean is one of two things, and a
     * choice is one of the field's own options — the type, the Required flag and the option list
     * already say everything a rule could.
     */
    @Test
    void fieldsWithNothingToMeasureCarryNoRestrictionsAtAll() {
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.BOOLEAN)).isEmpty();
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.DATE)).isEmpty();
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.SELECT)).isEmpty();
        assertThat(RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.MULTI_SELECT)).isEmpty();
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
