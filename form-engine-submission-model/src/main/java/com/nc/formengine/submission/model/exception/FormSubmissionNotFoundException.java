package com.nc.formengine.submission.model.exception;

public class FormSubmissionNotFoundException extends SubmissionNotFoundException {

    public FormSubmissionNotFoundException(Long id) {
        super("Form submission not found with id: " + id);
    }
}
