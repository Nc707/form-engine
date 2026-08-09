package com.nc.formengine.ui.builder;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.specification.RestrictionTypeRegistry;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What each restriction type needs from the person configuring it, and under which name the engine
 * expects to find it.
 *
 * <p>This table is the builder's half of a contract whose other half is {@code
 * FieldSpecificationFactory}, and getting a key wrong here fails in the worst possible way. The
 * factory is deliberately permissive: a restriction whose parameter is missing or stored under a
 * name it does not recognise yields a specification that accepts every value. So a typo does not
 * raise anything — it produces a rule that appears in the editor, is saved to the database, and
 * quietly validates nothing. {@code RestrictionParameterSpecTest} pins every key against a literal
 * and round-trips one through the real factory for exactly that reason.
 */
record RestrictionParameterSpec(RestrictionType type,
                                String key,
                                Kind kind,
                                String label,
                                String helper) {

    /** What kind of input the parameter needs, and what type it has to be stored as. */
    enum Kind {
        /** The restriction takes no parameter. */
        NONE,
        /** A whole number, stored as an {@link Integer}. */
        INTEGER,
        /** A decimal number, stored as a {@link Double}. */
        DECIMAL,
        /** A regular expression, stored as a {@link String}. */
        REGEX
    }

    private static final Map<RestrictionType, RestrictionParameterSpec> SPECS =
            new EnumMap<>(RestrictionType.class);

    static {
        define(RestrictionType.NOT_NULL, null, Kind.NONE,
                null, "The field must be answered.");
        define(RestrictionType.NOT_EMPTY, null, Kind.NONE,
                null, "The answer must not be blank.");
        define(RestrictionType.EMAIL, null, Kind.NONE,
                null, "The answer must look like an email address.");
        define(RestrictionType.MIN_LENGTH, "minLength", Kind.INTEGER,
                "Minimum length", "Shorter answers are rejected.");
        define(RestrictionType.MAX_LENGTH, "maxLength", Kind.INTEGER,
                "Maximum length", "Longer answers are rejected.");
        define(RestrictionType.MIN_VALUE, "minValue", Kind.DECIMAL,
                "Minimum value", "Smaller numbers are rejected.");
        define(RestrictionType.MAX_VALUE, "maxValue", Kind.DECIMAL,
                "Maximum value", "Larger numbers are rejected.");
        define(RestrictionType.PATTERN, "pattern", Kind.REGEX,
                "Regular expression", "The answer must match this expression.");
    }

    private static void define(RestrictionType type, String key, Kind kind, String label, String helper) {
        SPECS.put(type, new RestrictionParameterSpec(type, key, kind, label, helper));
    }

    /**
     * The spec for a restriction type.
     *
     * @param type the type; may be null
     * @return its spec, or null when the type is null or unknown
     */
    static RestrictionParameterSpec of(RestrictionType type) {
        return type == null ? null : SPECS.get(type);
    }

    /** Whether this restriction needs a parameter at all. */
    boolean hasParameter() {
        return kind != Kind.NONE;
    }

    /**
     * The restrictions that can be applied to a field of this type, in a stable order.
     *
     * <p>{@code RestrictionTypeRegistry.getApplicableRestrictionTypes} returns an unordered {@code
     * Set}, which would let a dropdown reshuffle itself between renders. Filtering the enum's own
     * values instead gives declaration order, every time.
     *
     * <p>Note how little this leaves for most types: only {@code NOT_NULL} applies to dates,
     * booleans and the two select kinds. The editor says so out loud rather than showing what looks
     * like a broken one-item dropdown.
     *
     * @param fieldType the field's type; may be null
     * @return the applicable restrictions, never null
     */
    static List<RestrictionType> applicableTo(FieldType fieldType) {
        if (fieldType == null) {
            return List.of();
        }
        return Arrays.stream(RestrictionType.values())
                .filter(type -> RestrictionTypeRegistry.isApplicable(type, fieldType))
                .toList();
    }

    /**
     * Builds the parameter map to store for a restriction.
     *
     * <p>The numeric kinds are coerced to {@link Integer} or {@link Double} rather than passed
     * through, because {@code FieldRestrictionMapper} JSON-serialises each value on its own: a
     * length written as a double comes back out of the database reading {@code 5.0}.
     *
     * @param type  the restriction type
     * @param value the configured value; ignored for parameterless types
     * @return the map to put on the DTO, never null and empty when there is nothing to store
     */
    static Map<String, Object> parameters(RestrictionType type, Object value) {
        RestrictionParameterSpec spec = of(type);
        if (spec == null || !spec.hasParameter() || value == null) {
            return Map.of();
        }
        return switch (spec.kind()) {
            case INTEGER -> asNumber(value) == null
                    ? Map.of() : Map.of(spec.key(), asNumber(value).intValue());
            case DECIMAL -> asNumber(value) == null
                    ? Map.of() : Map.of(spec.key(), asNumber(value).doubleValue());
            case REGEX -> value.toString().isBlank()
                    ? Map.of() : Map.of(spec.key(), value.toString());
            case NONE -> Map.of();
        };
    }

    /**
     * Reads back the configured value of a stored restriction, so the editor can show it.
     *
     * @param type       the restriction type
     * @param parameters the stored parameters; may be null
     * @return the value, or null when there is none to show
     */
    static Object read(RestrictionType type, Map<String, Object> parameters) {
        RestrictionParameterSpec spec = of(type);
        if (spec == null || !spec.hasParameter() || parameters == null) {
            return null;
        }
        Object stored = parameters.get(spec.key());
        if (stored == null) {
            return null;
        }
        return switch (spec.kind()) {
            case INTEGER -> asNumber(stored) == null ? null : asNumber(stored).intValue();
            case DECIMAL -> asNumber(stored) == null ? null : asNumber(stored).doubleValue();
            case REGEX -> stored.toString();
            case NONE -> null;
        };
    }

    /**
     * Reads a number that may have arrived as text.
     *
     * <p>Parameters cross JSON on the way into the database and back out of it, so the same 5 turns
     * up as an {@code Integer}, a {@code Double} or a {@code String} depending on the route it took.
     */
    private static Number asNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String text) {
            try {
                return Double.valueOf(text.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }
}
