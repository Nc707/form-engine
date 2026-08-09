package com.nc.formengine.submission.model.exception;

/**
 * Base type for every domain error raised by the submission slice.
 *
 * <p>This mirrors {@code com.nc.formengine.model.exception.FormEngineException} rather than
 * extending it: the submission slice deliberately does not depend on the definition slice, and
 * an exception hierarchy is not a good enough reason to couple them. The REST layer handles both
 * roots side by side.
 */
public abstract class SubmissionException extends RuntimeException {

    protected SubmissionException(String message) {
        super(message);
    }
}
