package com.nc.formengine.submission.model.exception;

/**
 * The form being submitted to is not live: it is still a draft, or it has been archived by a newer
 * version. Reported as HTTP 409.
 *
 * <p>The status is carried as a plain string rather than the definition slice's enum, so that
 * {@code form-engine-submission-model} keeps depending on nothing outside itself. Only the layers
 * above it know about form definitions.
 */
public class FormNotAcceptingSubmissionsException extends SubmissionException {

    private final Long formDefinitionId;
    private final String status;

    public FormNotAcceptingSubmissionsException(Long formDefinitionId, String status) {
        super("Form definition " + formDefinitionId + " is " + status
                + " and is not accepting submissions.");
        this.formDefinitionId = formDefinitionId;
        this.status = status;
    }

    public Long getFormDefinitionId() {
        return formDefinitionId;
    }

    public String getStatus() {
        return status;
    }
}
