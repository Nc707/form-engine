package com.nc.formengine.submission.model.exception;

import com.nc.formengine.submission.model.enums.SubmissionStatus;

/**
 * A state change the submission lifecycle does not allow, such as editing a submission that has
 * already been sent, or voiding a draft nobody submitted. Reported as HTTP 409.
 *
 * <p>Distinct from {@link InvalidSubmissionStatusException}, which is about a status <em>value</em>
 * that does not exist at all. Here both states are real; the move between them is not.
 *
 * <p>{@code to} is the state the caller asked for, which is not always a state the lifecycle would
 * ever move to from {@code from} — that is the whole point of the message.
 */
public class IllegalSubmissionTransitionException extends SubmissionException {

    private final SubmissionStatus from;
    private final SubmissionStatus to;

    public IllegalSubmissionTransitionException(Long submissionId, SubmissionStatus from, SubmissionStatus to) {
        super("Submission " + submissionId + " cannot go from " + from + " to " + to + ".");
        this.from = from;
        this.to = to;
    }

    public SubmissionStatus getFrom() {
        return from;
    }

    public SubmissionStatus getTo() {
        return to;
    }
}
