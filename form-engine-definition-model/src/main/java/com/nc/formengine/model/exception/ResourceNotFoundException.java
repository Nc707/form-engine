package com.nc.formengine.model.exception;

/**
 * A definition resource was addressed by an identifier that does not exist.
 * Reported as HTTP 404.
 */
public abstract class ResourceNotFoundException extends FormEngineException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
