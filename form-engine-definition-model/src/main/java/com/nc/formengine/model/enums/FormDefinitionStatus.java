package com.nc.formengine.model.enums;

/**
 * Where a form definition sits in its lifecycle.
 *
 * <p>The lifecycle exists to protect submissions. A submission records the answers given to a
 * particular set of fields under a particular set of restrictions; if that definition could still be
 * edited afterwards, the stored answers would silently start being judged against rules they were
 * never shown. So a definition is freely editable while it is a {@link #DRAFT}, and frozen from the
 * moment it is published. Changing a published form means creating a new version, not editing the
 * old one.
 *
 * <p>At most one version per {@code code} is {@link #PUBLISHED} at a time: publishing a newer
 * version moves the previous one to {@link #ARCHIVED}. That is what lets "the current form for this
 * code" be answered without ambiguity, while archived versions stay readable so old submissions can
 * still be rendered against the definition they were actually filled under.
 */
public enum FormDefinitionStatus {

    /** Editable and not accepting submissions. Every definition starts here. */
    DRAFT,

    /** Frozen and accepting submissions. Only one version per code holds this at a time. */
    PUBLISHED,

    /** Frozen and no longer accepting submissions, but kept so past submissions stay interpretable. */
    ARCHIVED
}
