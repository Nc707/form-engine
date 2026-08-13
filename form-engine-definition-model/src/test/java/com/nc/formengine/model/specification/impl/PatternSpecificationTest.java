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
        assertThat(result.getReasons()).containsExactly("This answer is not in the expected format");
    }

    @Test
    void matchesTheWholeValueAndNotJustAPartOfIt() {
        assertThat(new PatternSpecification("[A-Z]+").isSatisfiedBy("ABC!", TEXT).isSatisfied()).isFalse();
    }

    @Test
    void reportsTheCustomMessageWhenOneIsGiven() {
        SpecificationResult result = new PatternSpecification("^[A-Z]+$", "Capitals only")
            .isSatisfiedBy("abc", TEXT);

        assertThat(result.getReasons()).containsExactly("Capitals only");
    }

    /**
     * The field service refuses to store a pattern that will not compile, so this is only reachable by
     * building the specification by hand. It still must not blow up — and the message has to blame the
     * rule rather than the person answering, who cannot do anything about it.
     */
    @Test
    void reportsAnUnusablePatternInsteadOfBlowingUp() {
        SpecificationResult result = new PatternSpecification("[unclosed").isSatisfiedBy("abc", TEXT);

        assertThat(result.isSatisfied()).isFalse();
        assertThat(result.getReasons()).singleElement().asString()
            .isEqualTo("This field's pattern rule is misconfigured and could not be checked");
    }

    @Test
    void passesOnNullBecausePresenceIsNotItsJob() {
        assertThat(new PatternSpecification("^[A-Z]+$").isSatisfiedBy(null, TEXT).isSatisfied()).isTrue();
    }

    @Test
    void rejectsAValueThatIsNotText() {
        SpecificationResult result = new PatternSpecification("^[A-Z]+$").isSatisfiedBy(1, TEXT);

        assertThat(result.getReasons()).containsExactly("This answer is not text");
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
