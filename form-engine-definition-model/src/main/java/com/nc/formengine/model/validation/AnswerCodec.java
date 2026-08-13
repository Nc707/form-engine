package com.nc.formengine.model.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * How an answer is written down, for the parts of it the engine itself has to read.
 *
 * <p>This used to be decided in the Vaadin module, by the class that builds the widgets — and the
 * engine depended on it anyway: the dependency evaluator compared against {@code "true"}, the seeder
 * needed a comment to say what a checkbox stores, and nothing told a REST client any of it. A rule the
 * engine enforces cannot live in one of its consumers, so the shared part lives here.
 *
 * <p>What stays in the UI is the widget plumbing — parsing a date picker, formatting a number for
 * display. What is here is only what two independent readers must agree on.
 */
public final class AnswerCodec {

    /**
     * Separates the chosen values of a MULTI_SELECT answer, which is stored as one string.
     *
     * <p>It is not escaped, so an option value containing it would be indistinguishable from two
     * values. That is a rule about option values rather than about answers, and it is enforced where
     * options are written.
     */
    public static final String SELECTION_SEPARATOR = ",";

    /** What a BOOLEAN answer stores when it is checked. */
    public static final String TRUE = "true";

    /** What a BOOLEAN answer stores when it is not checked. */
    public static final String FALSE = "false";

    private AnswerCodec() {
    }

    /**
     * The values selected by an answer, whichever shape it arrived in.
     *
     * <p>Both shapes are real. Coming through a submission an answer is the joined string; coming from
     * a form being filled in, before anything is stored, it is still the widget's own collection.
     *
     * @param value the answer; may be null
     * @return the selected values in order, without blanks, never null
     */
    public static List<String> decodeSelections(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return nonBlank(collection.stream().map(String::valueOf).toList());
        }
        if (value instanceof Object[] array) {
            return nonBlank(Arrays.stream(array).map(String::valueOf).toList());
        }
        return nonBlank(Arrays.asList(String.valueOf(value).split(SELECTION_SEPARATOR)));
    }

    /**
     * Writes selected values down as one answer.
     *
     * @param values the chosen values; may be null
     * @return the stored form, never null
     */
    public static String encodeSelections(Collection<String> values) {
        return values == null ? "" : String.join(SELECTION_SEPARATOR, values);
    }

    /** Writes a boolean answer down. */
    public static String encodeBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }

    private static List<String> nonBlank(List<String> parts) {
        List<String> kept = new ArrayList<>(parts.size());
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();
            if (!trimmed.isEmpty()) {
                kept.add(trimmed);
            }
        }
        return kept;
    }
}
