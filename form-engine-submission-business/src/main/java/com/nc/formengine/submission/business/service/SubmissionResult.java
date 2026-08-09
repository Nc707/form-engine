package com.nc.formengine.submission.business.service;

import com.nc.formengine.model.validation.ValidationReport;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;

/**
 * What came back from a workflow operation: the submission, and how it was judged.
 *
 * <p>Both parts matter to a caller. A rejected submit needs the report to show the user which fields
 * to fix, and a saved draft needs it to show the same errors without treating them as fatal. Bundling
 * them means neither a thrown exception nor a second call is needed to find out why something did not
 * go through.
 *
 * @param submission the submission as it now stands. Persisted and carrying an id when the operation
 *                   changed anything; the unsaved input, unchanged, when a submit was rejected
 * @param report     the validation outcome. Never null: an operation that validated nothing reports
 *                   {@link ValidationReport#noErrors()}
 * @param persisted  whether the submission was written. False only for a rejected submit, which is
 *                   also the only case where {@code report} is invalid
 */
public record SubmissionResult(FormSubmissionDTO submission, ValidationReport report, boolean persisted) {

    /** The operation went through and the submission was written. */
    public static SubmissionResult persisted(FormSubmissionDTO submission, ValidationReport report) {
        return new SubmissionResult(submission, report, true);
    }

    /** The submission was refused and nothing was written. */
    public static SubmissionResult rejected(FormSubmissionDTO submission, ValidationReport report) {
        return new SubmissionResult(submission, report, false);
    }

    /** Shorthand for {@code report().valid()}. */
    public boolean valid() {
        return report.valid();
    }
}
