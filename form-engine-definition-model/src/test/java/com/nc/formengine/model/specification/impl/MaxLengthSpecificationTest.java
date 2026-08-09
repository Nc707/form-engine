package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.SpecificationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxLengthSpecificationTest {

    private static final FieldContext TEXT = FieldContext.builder()
        .fieldType(FieldType.TEXT).fieldName("nickname").build();

    @Test
    void acceptsAValueShorterThanTheMaximum() {
        assertThat(new MaxLengthSpecification(5).isSatisfiedBy("abc", TEXT).isSatisfied()).isTrue();
    }

    @Test
    void acceptsAValueExactlyAtTheMaximum() {
        assertThat(new MaxLengthSpecification(5).isSatisfiedBy("abcde", TEXT).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueLongerThanTheMaximum() {
        SpecificationResult result = new MaxLengthSpecification(5).isSatisfiedBy("abcdef", TEXT);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).containsExactly("El campo no puede tener más de 5 caracteres");
    }

    @Test
    void reportsTheCustomMessageWhenOneIsGiven() {
        SpecificationResult result = new MaxLengthSpecification(5, "Muy largo").isSatisfiedBy("abcdef", TEXT);

        assertThat(result.getReasons()).containsExactly("Muy largo");
    }

    @Test
    void passesOnNullBecausePresenceIsNotItsJob() {
        assertThat(new MaxLengthSpecification(5).isSatisfiedBy(null, TEXT).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueThatIsNotText() {
        SpecificationResult result = new MaxLengthSpecification(5).isSatisfiedBy(123456, TEXT);

        assertThat(result.getReasons()).containsExactly("El valor no es una cadena de texto");
    }

    @Test
    void passesOnAFieldTypeItDoesNotApplyTo() {
        FieldContext date = FieldContext.builder().fieldType(FieldType.DATE).build();

        assertThat(new MaxLengthSpecification(2).isSatisfiedBy("abcdef", date).isSatisfied()).isTrue();
    }

    @Test
    void appliesToTextFieldsOnly() {
        assertThat(MaxLengthSpecification.getApplicableTypes()).containsExactly(FieldType.TEXT);
    }
}
