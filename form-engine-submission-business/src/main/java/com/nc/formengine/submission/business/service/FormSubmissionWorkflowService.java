package com.nc.formengine.submission.business.service;

import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;

/**
 * Drives a submission through its lifecycle.
 *
 * <p>{@link FormSubmissionService} is the plain CRUD view of submissions and stays that way. This is
 * the view that enforces the rules: which state changes are allowed, and what has to be true before
 * one is.
 *
 * <pre>
 *   DRAFT ──submit──&gt; SUBMITTED ──void──&gt; VOIDED
 *     │
 *     └──discard──&gt; DISCARDED
 * </pre>
 *
 * <p>Both endings are terminal, and there is no way back to {@link SubmissionStatus#DRAFT} from
 * {@link SubmissionStatus#SUBMITTED}: a submitted form is a record of what someone actually sent, so it
 * is not editable afterwards. Which of the two endings applies follows from where you are — a draft can
 * only be discarded, a sent response can only be voided — so neither operation needs to be told who is
 * asking.
 *
 * @see SubmissionStatus
 */
public interface FormSubmissionWorkflowService {

    /**
     * Saves work in progress.
     *
     * <p>Validated leniently: missing answers are expected while the user is still typing, so
     * required fields are not demanded and the returned report is advice rather than a verdict. The
     * draft is saved whatever the report says.
     *
     * @param submission the draft, with an id to update one or without to start one
     * @return the saved draft and the lenient report, always persisted
     * @throws com.nc.formengine.submission.model.exception.IllegalSubmissionTransitionException
     *         if the stored submission is no longer a draft
     */
    SubmissionResult saveDraft(FormSubmissionDTO submission);

    /**
     * Submits the form, if it passes.
     *
     * <p>Validated strictly, against the definition's restrictions and with every visible required
     * field demanded. Validation and the write are one transaction, so a submission is never stored
     * in a state its own definition rejects.
     *
     * <p>An invalid submission is not an exception: being told which fields to fix is the normal way
     * this ends. It comes back as a {@link SubmissionResult} that is not
     * {@link SubmissionResult#persisted()}, carrying the errors, with nothing written.
     *
     * @param submission the answers to submit, with an id to submit a stored draft or without to
     *                   submit in one step
     * @return the submission and its report; persisted only if the report is valid
     * @throws com.nc.formengine.submission.model.exception.IllegalSubmissionTransitionException
     *         if the stored submission is not a draft
     * @throws com.nc.formengine.submission.model.exception.FormNotAcceptingSubmissionsException
     *         if the definition is not published
     */
    SubmissionResult submit(FormSubmissionDTO submission);

    /**
     * Gives up on a draft, without sending it.
     *
     * <p>The answers are kept: a discarded draft is a record that someone started and stopped, which is
     * worth more than a deleted row. Nothing was ever submitted, so nothing is being retracted.
     *
     * @param id the draft to discard
     * @return the discarded submission
     * @throws com.nc.formengine.submission.model.exception.IllegalSubmissionTransitionException
     *         if it is not a draft
     * @throws com.nc.formengine.submission.model.exception.FormSubmissionNotFoundException
     *         if the id is unknown
     */
    FormSubmissionDTO discard(Long id);

    /**
     * Annuls a response that was already submitted, leaving it readable.
     *
     * <p>This is the form owner's decision about a response that arrived — a duplicate, a test, an
     * answer given in error — and not the respondent taking something back. The answers are never
     * deleted, so the record of what was sent survives being annulled.
     *
     * <p>Named {@code voidSubmission} only because {@code void} is a Java keyword; the state, and what
     * every reader of it sees, is {@link SubmissionStatus#VOIDED}.
     *
     * @param id the submitted response to void
     * @return the voided submission
     * @throws com.nc.formengine.submission.model.exception.IllegalSubmissionTransitionException
     *         if it was never submitted, or was already voided
     * @throws com.nc.formengine.submission.model.exception.FormSubmissionNotFoundException
     *         if the id is unknown
     */
    FormSubmissionDTO voidSubmission(Long id);
}
