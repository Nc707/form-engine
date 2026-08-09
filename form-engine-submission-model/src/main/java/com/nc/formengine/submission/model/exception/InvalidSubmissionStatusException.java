package com.nc.formengine.submission.model.exception;

import com.nc.formengine.submission.model.enums.SubmissionStatus;

import java.util.Arrays;
import java.util.List;

/**
 * A caller passed a status string that is not a {@link SubmissionStatus} constant.
 * Reported as HTTP 400, listing what would have been accepted.
 */
public class InvalidSubmissionStatusException extends SubmissionException {

    private static final List<String> ALLOWED_VALUES =
            Arrays.stream(SubmissionStatus.values()).map(Enum::name).toList();

    private final String value;

    public InvalidSubmissionStatusException(String value) {
        super("Unknown submission status: '" + value + "'. Allowed values are "
                + String.join(", ", ALLOWED_VALUES) + ".");
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public List<String> getAllowedValues() {
        return ALLOWED_VALUES;
    }
}
