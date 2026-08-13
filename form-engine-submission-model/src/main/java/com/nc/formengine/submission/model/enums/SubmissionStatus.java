package com.nc.formengine.submission.model.enums;

/**
 * Where a submission is in its life.
 *
 * <pre>
 *   DRAFT ──submit──&gt; SUBMITTED ──void──&gt; VOIDED
 *     │
 *     └──discard──&gt; DISCARDED
 * </pre>
 *
 * <p>The two endings are different things done by different people, which is why they are two states
 * and not one. A draft is abandoned by whoever was filling it in — nothing was ever sent, so there is
 * nothing to answer for. A submitted response is voided by whoever owns the form, after it arrived: the
 * answers stay readable, they just no longer count.
 *
 * <p>They used to be a single {@code CANCELED} reachable from both, which read as the respondent
 * un-sending something already sent. Nobody cancels what they just submitted.
 */
public enum SubmissionStatus {

    /** Being filled in. Incomplete is expected, and it can be saved as often as you like. */
    DRAFT,

    /** Sent, and valid against its definition at the moment it was written. */
    SUBMITTED,

    /** A draft the person filling it in gave up on. Terminal. */
    DISCARDED,

    /** A received response the form's owner annulled without deleting it. Terminal. */
    VOIDED
}
