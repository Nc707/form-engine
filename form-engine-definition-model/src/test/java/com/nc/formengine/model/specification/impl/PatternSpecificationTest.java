package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.SpecificationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatternSpecificationTest {

    private static final FieldContext TEXT = FieldContext.builder()
        .fieldType(FieldType.TEXT).fieldName("code").build();

    @Test
    void acceptsAValueMatchingThePattern() {
        assertThat(new PatternSpecification("^[A-Z]+$").isSatisfiedBy("ABC", TEXT).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueNotMatchingThePattern() {
        SpecificationResult result = new PatternSpecification("^[A-Z]+$").isSatisfiedBy("AbC", TEXT);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).containsExactly("El valor no cumple con el formato requerido");
    }

    @Test
    void matchesTheWholeValueAndNotJustAPartOfIt() {
        assertThat(new PatternSpecification("[A-Z]+").isSatisfiedBy("ABC!", TEXT).isSatisfied()).isFalse();
    }

    @Test
    void reportsTheCustomMessageWhenOneIsGiven() {
        SpecificationResult result = new PatternSpecification("^[A-Z]+$", "Sólo mayúsculas")
            .isSatisfiedBy("abc", TEXT);

        assertThat(result.getReasons()).containsExactly("Sólo mayúsculas");
    }

    @Test
    void reportsAnUnusablePatternInsteadOfBlowingUp() {
        SpecificationResult result = new PatternSpecification("[unclosed").isSatisfiedBy("abc", TEXT);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).singleElement().asString()
            .startsWith("Patrón de expresión regular inválido:");
    }

    @Test
    void passesOnNullBecausePresenceIsNotItsJob() {
        assertThat(new PatternSpecification("^[A-Z]+$").isSatisfiedBy(null, TEXT).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueThatIsNotText() {
        SpecificationResult result = new PatternSpecification("^[A-Z]+$").isSatisfiedBy(1, TEXT);

        assertThat(result.getReasons()).containsExactly("El valor no es una cadena de texto");
    }

    @Test
    void passesOnAFieldTypeItDoesNotApplyTo() {
        FieldContext select = FieldContext.builder().fieldType(FieldType.SELECT).build();

        assertThat(new PatternSpecification("^[A-Z]+$").isSatisfiedBy("abc", select).isSatisfied()).isTrue();
    }

    @Test
    void appliesToTextFieldsOnly() {
        assertThat(PatternSpecification.getApplicableTypes()).containsExactly(FieldType.TEXT);
    }
}
