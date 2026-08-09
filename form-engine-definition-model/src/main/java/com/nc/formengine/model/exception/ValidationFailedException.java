package com.nc.formengine.model.exception;

import java.util.List;

/**
 * The request was syntactically fine but broke a domain rule that can only be
 * checked against persisted state. Reported as HTTP 422, with {@link #getReasons()}
 * carrying one entry per rule that failed.
 */
public class ValidationFailedException extends FormEngineException {

    private final List<String> reasons;

    public ValidationFailedException(String message, List<String> reasons) {
        super(message);
        this.reasons = List.copyOf(reasons);
    }

    public ValidationFailedException(String message, String reason) {
        this(message, List.of(reason));
    }

    public List<String> getReasons() {
        return reasons;
    }
}
