package com.nc.formengine.model.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The outcome of validating a set of answers.
 *
 * @param valid  whether every checked answer satisfied every restriction that applied to it
 * @param errors every broken rule, in field order and, within a field, in restriction order
 */
public record ValidationReport(boolean valid, List<FieldValidationError> errors) {

    /**
     * Keeps the two components from ever disagreeing: {@code valid} is exactly "no errors were
     * found", so a caller cannot build a report claiming to be valid while carrying errors. The list
     * is copied and made unmodifiable, since a report is a result and not a work area.
     */
    public ValidationReport {
        errors = errors == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(errors));
        valid = errors.isEmpty();
    }

    /**
     * A report with no errors. Named around the errors rather than the verdict because a record
     * component already owns the name {@code valid}.
     */
    public static ValidationReport noErrors() {
        return new ValidationReport(true, List.of());
    }

    /**
     * A report over the given errors, valid when there are none.
     */
    public static ValidationReport of(List<FieldValidationError> errors) {
        return new ValidationReport(false, errors);
    }

    /**
     * The errors of one field, in the order they were found.
     *
     * @param fieldName the field to filter by
     * @return its errors, empty if the field has none
     */
    public List<FieldValidationError> errorsFor(String fieldName) {
        return errors.stream()
            .filter(error -> Objects.equals(error.fieldName(), fieldName))
            .toList();
    }
}
