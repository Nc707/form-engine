package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.SpecificationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MinLengthSpecificationTest {

    private static final FieldContext TEXT = FieldContext.builder()
        .fieldType(FieldType.TEXT).fieldName("nickname").build();

    @Test
    void acceptsAValueLongerThanTheMinimum() {
        SpecificationResult result = new MinLengthSpecification(3).isSatisfiedBy("abcd", TEXT);

        assertThat(result.isSatisfied()).isTrue();
        assertThat(result.getReasons()).isEmpty();
    }

    @Test
    void acceptsAValueExactlyAtTheMinimum() {
        assertThat(new MinLengthSpecification(3).isSatisfiedBy("abc", TEXT).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueShorterThanTheMinimum() {
        SpecificationResult result = new MinLengthSpecification(3).isSatisfiedBy("ab", TEXT);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).containsExactly("El campo debe tener al menos 3 caracteres");
    }

    @Test
    void reportsTheCustomMessageWhenOneIsGiven() {
        SpecificationResult result = new MinLengthSpecification(3, "Muy corto").isSatisfiedBy("ab", TEXT);

        assertThat(result.getReasons()).containsExactly("Muy corto");
    }

    @Test
    void passesOnNullBecausePresenceIsNotItsJob() {
        assertThat(new MinLengthSpecification(3).isSatisfiedBy(null, TEXT).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueThatIsNotText() {
        SpecificationResult result = new MinLengthSpecification(3).isSatisfiedBy(42, TEXT);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).containsExactly("El valor no es una cadena de texto");
    }

    @Test
    void passesOnAFieldTypeItDoesNotApplyTo() {
        FieldContext number = FieldContext.builder().fieldType(FieldType.NUMBER).build();

        assertThat(new MinLengthSpecification(3).isSatisfiedBy("ab", number).isSatisfied()).isTrue();
    }

    @Test
    void appliesWhenTheContextDoesNotSayWhichTypeTheFieldIs() {
        FieldContext unknown = FieldContext.builder().build();

        assertThat(new MinLengthSpecification(3).isSatisfiedBy("ab", unknown).isSatisfied()).isFalse();
    }

    @Test
    void appliesToTextFieldsOnly() {
        assertThat(MinLengthSpecification.getApplicableTypes()).containsExactly(FieldType.TEXT);
    }
}
