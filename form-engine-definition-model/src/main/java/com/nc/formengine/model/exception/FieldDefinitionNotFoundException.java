package com.nc.formengine.model.exception;

public class FieldDefinitionNotFoundException extends ResourceNotFoundException {

    public FieldDefinitionNotFoundException(Long id) {
        super("Field not found with id: " + id);
    }
}
