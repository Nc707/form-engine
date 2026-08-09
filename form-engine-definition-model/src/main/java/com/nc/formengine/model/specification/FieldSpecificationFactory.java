package com.nc.formengine.model.specification;

import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.specification.impl.EmailSpecification;
import com.nc.formengine.model.specification.impl.MaxLengthSpecification;
import com.nc.formengine.model.specification.impl.MaxValueSpecification;
import com.nc.formengine.model.specification.impl.MinLengthSpecification;
import com.nc.formengine.model.specification.impl.MinValueSpecification;
import com.nc.formengine.model.specification.impl.NotEmptySpecification;
import com.nc.formengine.model.specification.impl.NotNullSpecification;
import com.nc.formengine.model.specification.impl.PatternSpecification;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns the stored description of a restriction into the specification that enforces it.
 *
 * <p>This is the bridge between the two halves of the validation engine: {@link FieldRestrictionDTO}
 * is data, chosen by whoever designed the form and persisted with it, while {@link FieldSpecification}
 * is behaviour. Everything here is static and free of any framework, so the whole mapping can be
 * exercised without a container.
 *
 * <p><b>Malformed restrictions are permissive.</b> A restriction whose type is unknown, or whose
 * required parameter is missing or unreadable, yields a specification that accepts every value. A
 * mistake made while designing a form must not lock users out of submitting it, and the alternative —
 * rejecting every answer — is both harder to diagnose and impossible for the user to work around.
 */
public final class FieldSpecificationFactory {

    /** Accepts any value. Shared because it holds no state. */
    private static final FieldSpecification ALWAYS_SATISFIED =
        (value, context) -> SpecificationResult.satisfied();

    private FieldSpecificationFactory() {
    }

    /**
     * Builds the specification for a single restriction.
     *
     * <p>Parameters are read under the keys documented on {@link FieldRestrictionDTO}: {@code
     * minLength}, {@code maxLength}, {@code minValue}, {@code maxValue} and {@code pattern}. The
     * remaining three restriction types take no parameter.
     *
     * @param restriction the restriction to enforce; may be null
     * @return the specification enforcing it, never null
     */
    public static FieldSpecification from(FieldRestrictionDTO restriction) {
        if (restriction == null || restriction.getRestrictionType() == null) {
            return ALWAYS_SATISFIED;
        }

        Map<String, Object> parameters = restriction.getParameters();
        String message = restriction.getErrorMessage();

        return switch (restriction.getRestrictionType()) {
            case NOT_NULL -> hasText(message)
                ? new NotNullSpecification(message)
                : new NotNullSpecification();
            case NOT_EMPTY -> hasText(message)
                ? new NotEmptySpecification(message)
                : new NotEmptySpecification();
            case EMAIL -> hasText(message)
                ? new EmailSpecification(message)
                : new EmailSpecification();
            case MIN_LENGTH -> {
                Integer minLength = intParameter(parameters, "minLength");
                if (minLength == null) {
                    yield ALWAYS_SATISFIED;
                }
                yield hasText(message)
                    ? new MinLengthSpecification(minLength, message)
                    : new MinLengthSpecification(minLength);
            }
            case MAX_LENGTH -> {
                Integer maxLength = intParameter(parameters, "maxLength");
                if (maxLength == null) {
                    yield ALWAYS_SATISFIED;
                }
                yield hasText(message)
                    ? new MaxLengthSpecification(maxLength, message)
                    : new MaxLengthSpecification(maxLength);
            }
            case MIN_VALUE -> {
                Double minValue = doubleParameter(parameters, "minValue");
                if (minValue == null) {
                    yield ALWAYS_SATISFIED;
                }
                yield hasText(message)
                    ? new MinValueSpecification(minValue, message)
                    : new MinValueSpecification(minValue);
            }
            case MAX_VALUE -> {
                Double maxValue = doubleParameter(parameters, "maxValue");
                if (maxValue == null) {
                    yield ALWAYS_SATISFIED;
                }
                yield hasText(message)
                    ? new MaxValueSpecification(maxValue, message)
                    : new MaxValueSpecification(maxValue);
            }
            case PATTERN -> {
                String pattern = stringParameter(parameters, "pattern");
                if (!hasText(pattern)) {
                    yield ALWAYS_SATISFIED;
                }
                yield hasText(message)
                    ? new PatternSpecification(pattern, message)
                    : new PatternSpecification(pattern);
            }
        };
    }

    /**
     * Builds the single specification enforcing every restriction of a field.
     *
     * <p>Restrictions are combined with {@code and} in {@code orderIndex} order, so the first failure
     * in the order the form author chose is the one reported. Restrictions without an order come
     * last, keeping their relative order.
     *
     * <p>Because {@code and} stops at the first failure, the result says whether the value is
     * acceptable, not everything that is wrong with it. Callers that need one error per broken rule
     * evaluate the restrictions individually through {@link #from(FieldRestrictionDTO)}.
     *
     * @param restrictions the restrictions to enforce; may be null or empty
     * @return the combined specification, never null
     */
    public static FieldSpecification composite(List<FieldRestrictionDTO> restrictions) {
        if (restrictions == null || restrictions.isEmpty()) {
            return ALWAYS_SATISFIED;
        }

        return restrictions.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(FieldRestrictionDTO::getOrderIndex,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .map(FieldSpecificationFactory::from)
            .reduce(FieldSpecification::and)
            .orElse(ALWAYS_SATISFIED);
    }

    /**
     * Reads a numeric parameter, accepting both a number and its string form.
     *
     * <p>Restriction parameters are typed as {@code Object} and cross JSON on the way in and on the
     * way out of the database, so the same {@code 5} arrives as an {@code Integer}, a {@code Double}
     * or a {@code String} depending on the path it took.
     */
    private static Double doubleParameter(Map<String, Object> parameters, String name) {
        Object value = parameters != null ? parameters.get(name) : null;

        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Integer intParameter(Map<String, Object> parameters, String name) {
        Double value = doubleParameter(parameters, name);
        return value != null ? value.intValue() : null;
    }

    private static String stringParameter(Map<String, Object> parameters, String name) {
        Object value = parameters != null ? parameters.get(name) : null;
        return value != null ? value.toString() : null;
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
