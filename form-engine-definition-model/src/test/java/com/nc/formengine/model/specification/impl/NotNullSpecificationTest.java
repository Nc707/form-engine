package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.SpecificationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class NotNullSpecificationTest {

    private static final FieldContext TEXT = FieldContext.builder()
        .fieldType(FieldType.TEXT).fieldName("nickname").build();

    @Test
    void rejectsNull() {
        SpecificationResult result = new NotNullSpecification().isSatisfiedBy(null, TEXT);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).containsExactly("El campo es obligatorio");
    }

    @Test
    void reportsTheCustomMessageWhenOneIsGiven() {
        SpecificationResult result = new NotNullSpecification("Falta el apodo").isSatisfiedBy(null, TEXT);

        assertThat(result.getReasons()).containsExactly("Falta el apodo");
    }

    @Test
    void acceptsAnyValueThatIsThere() {
        assertThat(new NotNullSpecification().isSatisfiedBy("", TEXT).isSatisfied()).isTrue();
        assertThat(new NotNullSpecification().isSatisfiedBy(0, TEXT).isSatisfied()).isTrue();
        assertThat(new NotNullSpecification().isSatisfiedBy(false, TEXT).isSatisfied()).isTrue();
    }

    @Test
    void leavesEmptinessToNotEmpty() {
        assertThat(new NotNullSpecification().isSatisfiedBy("   ", TEXT).isSatisfied()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(FieldType.class)
    void guardsEveryFieldType(FieldType fieldType) {
        FieldContext context = FieldContext.builder().fieldType(fieldType).build();

        assertThat(new NotNullSpecification().isSatisfiedBy(null, context).isSatisfied()).isFalse();
    }

    @Test
    void appliesToEveryFieldType() {
        assertThat(NotNullSpecification.getApplicableTypes())
            .containsExactlyInAnyOrder(FieldType.values());
    }
}
