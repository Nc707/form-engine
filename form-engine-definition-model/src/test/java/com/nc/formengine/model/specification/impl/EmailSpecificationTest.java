package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.SpecificationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSpecificationTest {

    private static final FieldContext TEXT = FieldContext.builder()
        .fieldType(FieldType.TEXT).fieldName("email").build();

    @ParameterizedTest
    @ValueSource(strings = {"a@b.co", "first.last+tag@sub.example.com", "USER_1@example.org"})
    void acceptsAWellFormedAddress(String address) {
        assertThat(new EmailSpecification().isSatisfiedBy(address, TEXT).isSatisfied()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-at-sign", "@example.com", "user@", "user@example", "user@example.c",
        "user name@example.com"})
    void rejectsAMalformedAddress(String address) {
        SpecificationResult result = new EmailSpecification().isSatisfiedBy(address, TEXT);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).containsExactly("El formato del email es inválido");
    }

    @Test
    void reportsTheCustomMessageWhenOneIsGiven() {
        SpecificationResult result = new EmailSpecification("Revisá el correo").isSatisfiedBy("nope", TEXT);

        assertThat(result.getReasons()).containsExactly("Revisá el correo");
    }

    @Test
    void passesOnNullBecausePresenceIsNotItsJob() {
        assertThat(new EmailSpecification().isSatisfiedBy(null, TEXT).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueThatIsNotText() {
        SpecificationResult result = new EmailSpecification().isSatisfiedBy(42, TEXT);

        assertThat(result.getReasons()).containsExactly("El valor no es una cadena de texto");
    }

    @Test
    void passesOnAFieldTypeItDoesNotApplyTo() {
        FieldContext number = FieldContext.builder().fieldType(FieldType.NUMBER).build();

        assertThat(new EmailSpecification().isSatisfiedBy("nope", number).isSatisfied()).isTrue();
    }

    @Test
    void appliesToTextFieldsOnly() {
        assertThat(EmailSpecification.getApplicableTypes()).containsExactly(FieldType.TEXT);
    }
}
