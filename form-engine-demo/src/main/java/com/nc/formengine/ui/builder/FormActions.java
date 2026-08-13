package com.nc.formengine.ui.builder;

import com.nc.formengine.model.enums.FormDefinitionStatus;

/**
 * What a form in a given state may have done to it.
 *
 * <p>This is the single policy function behind every enabled or disabled control in the builder, in
 * the index grid and in the editor alike. Two views asking the same question have to get the same
 * answer, and the way to guarantee that is to have only one place that answers it.
 *
 * <p>It decides which controls to offer, and nothing more. Every rule it reads is enforced in the
 * business layer as well — a published form refuses writes to its fields, options, dependencies and
 * layouts, an empty form refuses to be published, and a draft refuses to be versioned — so a control
 * this record leaves enabled by mistake cannot produce a state the engine considers impossible. It used
 * to be the only thing standing between a user and exactly that.
 *
 * @param editable    whether the definition may still be changed
 * @param publishable whether the form may go live now
 * @param deletable   whether the form may be removed outright
 * @param versionable whether a new draft may be branched from it
 * @param reason      why editing is closed, or null while it is open
 */
record FormActions(boolean editable,
                   boolean publishable,
                   boolean deletable,
                   boolean versionable,
                   String reason) {

    /**
     * Reads the allowed actions off a form's status.
     *
     * <p>A draft is the only editable, deletable state, and the only one that can be published — but
     * not while it is empty. The engine would accept an empty published form; nobody means to create
     * one, and it would show up in the renderer as a form that cannot be answered.
     *
     * <p>Published and archived are equally frozen. Archived is not a lesser published: it is a
     * version that has already been superseded, and the way forward from both is a new version.
     *
     * @param status     the form's current status; null is treated as frozen
     * @param fieldCount how many fields the form has
     * @return the actions allowed, never null
     */
    static FormActions of(FormDefinitionStatus status, int fieldCount) {
        if (status == FormDefinitionStatus.DRAFT) {
            return new FormActions(true, fieldCount > 0, true, false, null);
        }
        if (status == FormDefinitionStatus.PUBLISHED) {
            return frozen("published");
        }
        if (status == FormDefinitionStatus.ARCHIVED) {
            return frozen("archived");
        }
        // No status at all should not happen, but guessing "editable" would be the dangerous guess.
        return new FormActions(false, false, false, false,
                "This form has no status and cannot be edited.");
    }

    private static FormActions frozen(String statusName) {
        return new FormActions(false, false, false, true,
                "This form is " + statusName + " and can no longer be edited. "
                        + "Create a new version to change it.");
    }

    /**
     * Why the publish action is unavailable, for the button's tooltip.
     *
     * <p>Kept here rather than in the view so that the rule and its explanation cannot drift apart.
     *
     * @return the explanation, or null when publishing is in fact allowed
     */
    String publishBlockedReason() {
        if (publishable) {
            return null;
        }
        return editable ? "Add at least one field before publishing." : reason;
    }
}
