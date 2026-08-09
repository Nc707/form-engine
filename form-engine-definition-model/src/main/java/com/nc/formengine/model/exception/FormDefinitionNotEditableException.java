package com.nc.formengine.model.exception;

import com.nc.formengine.model.enums.FormDefinitionStatus;

/**
 * Something tried to change a definition that is no longer a draft.
 *
 * <p>Reported as HTTP 409: the request is well formed and the form exists, but the form is frozen.
 * The way forward is a new version, not a retry, so the message says so.
 */
public class FormDefinitionNotEditableException extends FormEngineException {

    private final Long formDefinitionId;
    private final FormDefinitionStatus status;

    public FormDefinitionNotEditableException(Long formDefinitionId, FormDefinitionStatus status) {
        super("Form definition " + formDefinitionId + " is " + status
                + " and can no longer be modified. Create a new version to change it.");
        this.formDefinitionId = formDefinitionId;
        this.status = status;
    }

    public Long getFormDefinitionId() {
        return formDefinitionId;
    }

    public FormDefinitionStatus getStatus() {
        return status;
    }
}
