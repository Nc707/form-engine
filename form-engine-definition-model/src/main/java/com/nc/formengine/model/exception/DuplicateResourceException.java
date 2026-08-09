package com.nc.formengine.model.exception;

/**
 * A resource was created (or renamed) with a value that must be unique and is
 * already taken. Reported as HTTP 409.
 */
public class DuplicateResourceException extends FormEngineException {

    private final String resource;
    private final String field;
    private final String value;

    public DuplicateResourceException(String resource, String field, String value) {
        super(resource + " already exists with " + field + ": " + value);
        this.resource = resource;
        this.field = field;
        this.value = value;
    }

    public String getResource() {
        return resource;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
