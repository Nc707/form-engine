package com.nc.formengine.flow.builder;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.rules.DefinitionRules;
import com.nc.formengine.model.specification.RestrictionParameter;
import com.nc.formengine.model.specification.RestrictionTypeRegistry;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * How each restriction is presented to the person configuring it.
 *
 * <p>Only presentation lives here now. The parameter's <em>name</em> and <em>type</em> come from
 * {@link RestrictionParameter} in the model, which is also what {@code FieldSpecificationFactory} reads
 * them from — so the editor cannot spell a key differently from the engine. That used to be two
 * independent tables kept in step by a test, and getting one wrong produced a rule that appeared in the
 * editor, was saved to the database, and quietly validated nothing.
 */
record RestrictionParameterSpec(RestrictionType type, String label, String helper) {

    private static final Map<RestrictionType, RestrictionParameterSpec> SPECS =
            new EnumMap<>(RestrictionType.class);

    static {
        define(RestrictionType.EMAIL, null, "The answer must look like an email address.");
        define(RestrictionType.MIN_LENGTH, "Minimum length", "Shorter answers are rejected.");
        define(RestrictionType.MAX_LENGTH, "Maximum length", "Longer answers are rejected.");
        define(RestrictionType.MIN_VALUE, "Minimum value", "Smaller numbers are rejected.");
        define(RestrictionType.MAX_VALUE, "Maximum value", "Larger numbers are rejected.");
        define(RestrictionType.PATTERN, "Regular expression", "The answer must match this expression.");
    }

    private static void define(RestrictionType type, String label, String helper) {
        SPECS.put(type, new RestrictionParameterSpec(type, label, helper));
    }

    /**
     * The presentation of a restriction type.
     *
     * @param type the type; may be null
     * @return its spec, or null when the type is null or unknown
     */
    static RestrictionParameterSpec of(RestrictionType type) {
        return type == null ? null : SPECS.get(type);
    }

    /** The name the engine stores this restriction's parameter under, or null when it takes none. */
    String key() {
        RestrictionParameter parameter = RestrictionParameter.of(type);
        return parameter == null ? null : parameter.key();
    }

    /** What kind of input the parameter needs. */
    RestrictionParameter.Kind kind() {
        RestrictionParameter parameter = RestrictionParameter.of(type);
        return parameter == null ? RestrictionParameter.Kind.NONE : parameter.kind();
    }

    /** Whether this restriction needs a parameter at all. */
    boolean hasParameter() {
        return kind() != RestrictionParameter.Kind.NONE;
    }

    /**
     * The restrictions that can be applied to a field of this type, in a stable order.
     *
     * <p>{@code RestrictionTypeRegistry.getApplicableRestrictionTypes} returns an unordered {@code
     * Set}, which would let a dropdown reshuffle itself between renders. Filtering the enum's own
     * values instead gives declaration order, every time.
     *
     * <p>Note how little this leaves for most types: <em>nothing</em> applies to dates, booleans and
     * the two select kinds. Everything true of their answers is decided by the type itself, by the
     * Required checkbox, or by the options the field offers — so the editor says so out loud rather
     * than showing an empty dropdown.
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
        RestrictionParameter parameter = RestrictionParameter.of(type);
        if (parameter == null || !parameter.required() || value == null) {
            return Map.of();
        }
        return switch (parameter.kind()) {
            case INTEGER -> asNumber(value) == null
                    ? Map.of() : Map.of(parameter.key(), asNumber(value).intValue());
            case DECIMAL -> asNumber(value) == null
                    ? Map.of() : Map.of(parameter.key(), asNumber(value).doubleValue());
            case REGEX -> value.toString().isBlank()
                    ? Map.of() : Map.of(parameter.key(), value.toString());
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
        return DefinitionRules.readParameter(RestrictionParameter.of(type), parameters);
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
