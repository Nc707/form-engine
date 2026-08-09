package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.SpecificationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxValueSpecificationTest {

    private static final FieldContext NUMBER = FieldContext.builder()
        .fieldType(FieldType.NUMBER).fieldName("score").build();

    @Test
    void acceptsAValueBelowTheMaximum() {
        assertThat(new MaxValueSpecification(100.0).isSatisfiedBy(99, NUMBER).isSatisfied()).isTrue();
    }

    @Test
    void acceptsAValueExactlyAtTheMaximum() {
        assertThat(new MaxValueSpecification(100.0).isSatisfiedBy(100, NUMBER).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueAboveTheMaximum() {
        SpecificationResult result = new MaxValueSpecification(100.0).isSatisfiedBy(100.01, NUMBER);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).containsExactly("El valor no puede ser mayor que 100.0");
    }

    @Test
    void readsANumberWrittenAsText() {
        assertThat(new MaxValueSpecification(100.0).isSatisfiedBy("101", NUMBER).isSatisfied()).isFalse();
    }

    @Test
    void rejectsAValueThatIsNeitherNumberNorText() {
        SpecificationResult result = new MaxValueSpecification(100.0).isSatisfiedBy(true, NUMBER);

        assertThat(result.getReasons()).containsExactly("El valor no es un número válido");
    }

    @Test
    void reportsTheCustomMessageWhenOneIsGiven() {
        SpecificationResult result = new MaxValueSpecification(100.0, "Pasado").isSatisfiedBy(200, NUMBER);

        assertThat(result.getReasons()).containsExactly("Pasado");
    }

    @Test
    void passesOnNullBecausePresenceIsNotItsJob() {
        assertThat(new MaxValueSpecification(100.0).isSatisfiedBy(null, NUMBER).isSatisfied()).isTrue();
    }

    @Test
    void passesOnAFieldTypeItDoesNotApplyTo() {
        FieldContext bool = FieldContext.builder().fieldType(FieldType.BOOLEAN).build();

        assertThat(new MaxValueSpecification(1.0).isSatisfiedBy(999, bool).isSatisfied()).isTrue();
    }

    @Test
    void appliesToNumberFieldsOnly() {
        assertThat(MaxValueSpecification.getApplicableTypes()).containsExactly(FieldType.NUMBER);
    }
}
