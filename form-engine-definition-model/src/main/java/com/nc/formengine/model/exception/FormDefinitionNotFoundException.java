package com.nc.formengine.model.exception;

public class FormDefinitionNotFoundException extends ResourceNotFoundException {

    public FormDefinitionNotFoundException(Long id) {
        super("Form not found with id: " + id);
    }

    public FormDefinitionNotFoundException(String code) {
        super("Form not found with code: " + code);
    }
}
