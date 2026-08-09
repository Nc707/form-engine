package com.nc.formengine.submission.model.exception;

/**
 * A submission resource was addressed by an identifier that does not exist.
 * Reported as HTTP 404.
 */
public abstract class SubmissionNotFoundException extends SubmissionException {

    protected SubmissionNotFoundException(String message) {
        super(message);
    }
}
