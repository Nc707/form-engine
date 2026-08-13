package com.nc.formengine.model.validation;

import com.nc.formengine.model.enums.RestrictionType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationReportTest {

    private static final FieldValidationError TOO_SHORT = FieldValidationError.brokenRestriction(
        "nickname", 1L, RestrictionType.MIN_LENGTH, "This answer must be at least 3 characters long");
    private static final FieldValidationError BAD_EMAIL = FieldValidationError.brokenRestriction(
        "email", 2L, RestrictionType.EMAIL, "This is not a valid email address");

    @Test
    void aReportWithoutErrorsIsValid() {
        assertThat(ValidationReport.noErrors().valid()).isTrue();
        assertThat(ValidationReport.noErrors().errors()).isEmpty();
        assertThat(ValidationReport.of(List.of()).valid()).isTrue();
    }

    @Test
    void aReportWithErrorsIsNotValid() {
        ValidationReport report = ValidationReport.of(List.of(TOO_SHORT));

        assertThat(report.valid()).isFalse();
        assertThat(report.errors()).containsExactly(TOO_SHORT);
    }

    @Test
    void theVerdictCannotContradictTheErrors() {
        assertThat(new ValidationReport(true, List.of(TOO_SHORT)).valid()).isFalse();
        assertThat(new ValidationReport(false, List.of()).valid()).isTrue();
    }

    @Test
    void treatsAMissingErrorListAsNoErrors() {
        ValidationReport report = new ValidationReport(false, null);

        assertThat(report.valid()).isTrue();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void keepsItsOwnCopyOfTheErrors() {
        List<FieldValidationError> source = new ArrayList<>(List.of(TOO_SHORT));
        ValidationReport report = ValidationReport.of(source);

        source.add(BAD_EMAIL);

        assertThat(report.errors()).containsExactly(TOO_SHORT);
        assertThatThrownBy(() -> report.errors().add(BAD_EMAIL))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void picksOutTheErrorsOfOneField() {
        ValidationReport report = ValidationReport.of(List.of(TOO_SHORT, BAD_EMAIL));

        assertThat(report.errorsFor("email")).containsExactly(BAD_EMAIL);
        assertThat(report.errorsFor("nickname")).containsExactly(TOO_SHORT);
        assertThat(report.errorsFor("unknown")).isEmpty();
        assertThat(report.errorsFor(null)).isEmpty();
    }
}
