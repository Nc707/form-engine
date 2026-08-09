package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.SpecificationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NotEmptySpecificationTest {

    private static final FieldContext TEXT = FieldContext.builder()
        .fieldType(FieldType.TEXT).fieldName("comment").build();

    @Test
    void acceptsAValueWithContent() {
        assertThat(new NotEmptySpecification().isSatisfiedBy("hola", TEXT).isSatisfied()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "   \n  "})
    void rejectsAValueThatIsOnlyWhitespace(String value) {
        SpecificationResult result = new NotEmptySpecification().isSatisfiedBy(value, TEXT);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).containsExactly("El campo no puede estar vacío");
    }

    @Test
    void reportsTheCustomMessageWhenOneIsGiven() {
        SpecificationResult result = new NotEmptySpecification("Escribí algo").isSatisfiedBy("", TEXT);

        assertThat(result.getReasons()).containsExactly("Escribí algo");
    }

    @Test
    void passesOnNullBecausePresenceIsNotItsJob() {
        assertThat(new NotEmptySpecification().isSatisfiedBy(null, TEXT).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueThatIsNotText() {
        SpecificationResult result = new NotEmptySpecification().isSatisfiedBy(42, TEXT);

        assertThat(result.getReasons()).containsExactly("El valor no es una cadena de texto");
    }

    @Test
    void passesOnAFieldTypeItDoesNotApplyTo() {
        FieldContext multiSelect = FieldContext.builder().fieldType(FieldType.MULTI_SELECT).build();

        assertThat(new NotEmptySpecification().isSatisfiedBy("", multiSelect).isSatisfied()).isTrue();
    }

    @Test
    void appliesToTextFieldsOnly() {
        assertThat(NotEmptySpecification.getApplicableTypes()).containsExactly(FieldType.TEXT);
    }
}
