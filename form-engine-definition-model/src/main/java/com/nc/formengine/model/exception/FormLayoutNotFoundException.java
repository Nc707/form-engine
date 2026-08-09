package com.nc.formengine.model.exception;

import com.nc.formengine.model.enums.DeviceType;

public class FormLayoutNotFoundException extends ResourceNotFoundException {

    public FormLayoutNotFoundException(Long id) {
        super("Layout not found with id: " + id);
    }

    public FormLayoutNotFoundException(Long formDefinitionId, DeviceType deviceType) {
        super("No layout found for form " + formDefinitionId + " and device type "
                + (deviceType != null ? deviceType.name() : "GENERIC"));
    }
}
