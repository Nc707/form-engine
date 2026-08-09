package com.nc.formengine.submission.model.dto;

import com.nc.formengine.submission.model.enums.SubmissionStatus;

/**
 * What a caller wants to narrow a list of submissions down to.
 *
 * <p>Every component is optional, and a {@code null} means "this says nothing", not "this must be
 * null" — so a filter with three nulls matches everything. That is what lets a screen with three
 * independent filter inputs build one object without branching on which of them the user filled in.
 *
 * @param formDefinitionId the exact version of a form, not the form's code: a submission belongs to
 *                         the definition it was filled in against, and two versions of the same code
 *                         are different definitions
 * @param status           the lifecycle state to keep
 * @param submittedBy      matched as a case-insensitive substring, because this backs a search box
 *                         rather than an exact-match lookup
 */
public record SubmissionFilter(Long formDefinitionId, SubmissionStatus status, String submittedBy) {
}
