package com.nc.formengine.model.exception;

public class FieldOptionNotFoundException extends ResourceNotFoundException {

    public FieldOptionNotFoundException(Long id) {
        super("Field option not found with id: " + id);
    }
}
