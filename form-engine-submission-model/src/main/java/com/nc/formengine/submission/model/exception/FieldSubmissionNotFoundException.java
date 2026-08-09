package com.nc.formengine.submission.model.exception;

public class FieldSubmissionNotFoundException extends SubmissionNotFoundException {

    public FieldSubmissionNotFoundException(Long id) {
        super("Field submission not found with id: " + id);
    }
}
