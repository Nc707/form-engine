package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.SpecificationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MinValueSpecificationTest {

    private static final FieldContext NUMBER = FieldContext.builder()
        .fieldType(FieldType.NUMBER).fieldName("age").build();

    @Test
    void acceptsAValueAboveTheMinimum() {
        assertThat(new MinValueSpecification(18.0).isSatisfiedBy(21, NUMBER).isSatisfied()).isTrue();
    }

    @Test
    void acceptsAValueExactlyAtTheMinimum() {
        assertThat(new MinValueSpecification(18.0).isSatisfiedBy(18, NUMBER).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueBelowTheMinimum() {
        SpecificationResult result = new MinValueSpecification(18.0).isSatisfiedBy(17.99, NUMBER);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).containsExactly("El valor debe ser al menos 18.0");
    }

    @Test
    void readsANumberWrittenAsText() {
        assertThat(new MinValueSpecification(18.0).isSatisfiedBy("21", NUMBER).isSatisfied()).isTrue();
        assertThat(new MinValueSpecification(18.0).isSatisfiedBy("17", NUMBER).isSatisfied()).isFalse();
    }

    @Test
    void rejectsAValueThatIsNotANumber() {
        SpecificationResult result = new MinValueSpecification(18.0).isSatisfiedBy("veintiuno", NUMBER);

        assertThat(result.getReasons()).containsExactly("El valor no es un número válido");
    }

    @Test
    void reportsTheCustomMessageWhenOneIsGiven() {
        SpecificationResult result = new MinValueSpecification(18.0, "Sos menor").isSatisfiedBy(10, NUMBER);

        assertThat(result.getReasons()).containsExactly("Sos menor");
    }

    @Test
    void passesOnNullBecausePresenceIsNotItsJob() {
        assertThat(new MinValueSpecification(18.0).isSatisfiedBy(null, NUMBER).isSatisfied()).isTrue();
    }

    @Test
    void passesOnAFieldTypeItDoesNotApplyTo() {
        FieldContext text = FieldContext.builder().fieldType(FieldType.TEXT).build();

        assertThat(new MinValueSpecification(18.0).isSatisfiedBy(1, text).isSatisfied()).isTrue();
    }

    @Test
    void appliesToNumberFieldsOnly() {
        assertThat(MinValueSpecification.getApplicableTypes()).containsExactly(FieldType.NUMBER);
    }
}
