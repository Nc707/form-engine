package com.nc.formengine.model.validation;

import java.util.Collection;

/**
 * The engine's one answer to "did the user answer this?".
 *
 * <p>There used to be two, disagreeing: a {@code NOT_NULL} restriction rejected only a null, while the
 * {@code required} flag also rejected whitespace and an empty selection. A field carrying both was
 * judged by whichever ran first. This is the surviving definition, and it is the broad one — a field
 * holding only spaces has not been filled in, whatever the storage layer thinks.
 */
public final class Answers {

    private Answers() {
    }

    /**
     * Whether there is nothing in this value a rule could judge.
     *
     * @param value the answer, in whatever shape it reached the engine; may be null
     * @return true when the field should count as unanswered
     */
    public static boolean isMissing(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }
}
