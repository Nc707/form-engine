package com.nc.formengine.model.exception;

public class FieldDependencyNotFoundException extends ResourceNotFoundException {

    public FieldDependencyNotFoundException(Long id) {
        super("Field dependency not found with id: " + id);
    }
}
