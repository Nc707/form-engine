package com.nc.formengine.model.exception;

/**
 * Base type for every domain error raised by the definition slice.
 * The REST layer maps subtypes to HTTP status codes, so subclasses should stay
 * semantic: they describe what went wrong, never how it is reported.
 */
public abstract class FormEngineException extends RuntimeException {

    protected FormEngineException(String message) {
        super(message);
    }
}
