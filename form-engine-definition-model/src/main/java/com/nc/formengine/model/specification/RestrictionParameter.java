package com.nc.formengine.model.specification;

import com.nc.formengine.model.enums.RestrictionType;

import java.util.EnumMap;
import java.util.Map;

/**
 * What a restriction needs configuring with, and the name it is stored under.
 *
 * <p>These keys used to be string literals in {@link FieldSpecificationFactory} and, independently,
 * in a table inside the Vaadin builder. Both had to spell them the same way, and nothing but a test
 * made them: the factory treats a key it does not recognise as a reason to accept every value, so a
 * mismatch produced a rule that the editor showed, the database stored, and validation ignored.
 * One table, read by everyone who writes or reads a parameter, is what makes that impossible.
 */
public record RestrictionParameter(RestrictionType type, String key, Kind kind) {

    /** What the parameter is, and therefore how it has to be stored to survive a round trip. */
    public enum Kind {
        /** The restriction takes no parameter. */
        NONE,
        /** A whole number, stored as an {@link Integer}. */
        INTEGER,
        /** A decimal number, stored as a {@link Double}. */
        DECIMAL,
        /** A regular expression, stored as a {@link String}. */
        REGEX
    }

    private static final Map<RestrictionType, RestrictionParameter> BY_TYPE =
        new EnumMap<>(RestrictionType.class);

    static {
        define(RestrictionType.EMAIL, null, Kind.NONE);
        define(RestrictionType.MIN_LENGTH, "minLength", Kind.INTEGER);
        define(RestrictionType.MAX_LENGTH, "maxLength", Kind.INTEGER);
        define(RestrictionType.MIN_VALUE, "minValue", Kind.DECIMAL);
        define(RestrictionType.MAX_VALUE, "maxValue", Kind.DECIMAL);
        define(RestrictionType.PATTERN, "pattern", Kind.REGEX);
    }

    private static void define(RestrictionType type, String key, Kind kind) {
        BY_TYPE.put(type, new RestrictionParameter(type, key, kind));
    }

    /**
     * The parameter a restriction type takes.
     *
     * @param type the restriction type; may be null
     * @return its parameter, or null when the type is null or unknown
     */
    public static RestrictionParameter of(RestrictionType type) {
        return type == null ? null : BY_TYPE.get(type);
    }

    /** Whether this restriction needs a parameter at all. */
    public boolean required() {
        return kind != Kind.NONE;
    }
}
