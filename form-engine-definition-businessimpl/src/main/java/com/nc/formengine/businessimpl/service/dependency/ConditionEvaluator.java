package com.nc.formengine.businessimpl.service.dependency;

import com.nc.formengine.model.enums.DependencyCondition;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

/**
 * Applies a single {@link DependencyCondition} to the current value of a trigger field.
 * <p>
 * Pure and stateless, so it can be unit tested without any container.
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {
    }

    /**
     * Whether a condition holds.
     * <p>
     * A condition never holds when the trigger field has no value, or when the dependency declares no
     * value to compare against. That uniformity matters: if absence satisfied {@code NOT_EQUALS},
     * hiding a trigger field would switch on every {@code NOT_EQUALS} rule it feeds, and the user
     * would have no way to switch them back off.
     *
     * @param condition the comparison to apply
     * @param actual    the current answer for the trigger field, possibly null
     * @param expected  the value declared by the dependency
     */
    public static boolean matches(DependencyCondition condition, Object actual, String expected) {
        if (condition == null || expected == null || expected.isBlank() || isAbsent(actual)) {
            return false;
        }

        return switch (condition) {
            case EQUALS -> equalValues(actual, expected);
            case NOT_EQUALS -> !equalValues(actual, expected);
            case GREATER_THAN -> compare(actual, expected) > 0;
            case LESS_THAN -> compare(actual, expected) < 0;
            case CONTAINS -> contains(actual, expected);
        };
    }

    /** An answer counts as absent when it is null, blank, or an empty collection or array. */
    public static boolean isAbsent(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence text) {
            return text.toString().isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value instanceof Object[] array) {
            return array.length == 0;
        }
        return false;
    }

    /**
     * Numeric comparison when both sides parse as numbers, so a JSON {@code 1.0} matches a stored
     * {@code "1"}; exact string equality otherwise.
     */
    private static boolean equalValues(Object actual, String expected) {
        BigDecimal actualNumber = toNumber(actual);
        BigDecimal expectedNumber = toNumber(expected);
        if (actualNumber != null && expectedNumber != null) {
            // compareTo, never equals: BigDecimal.equals("1.0", "1") is false.
            return actualNumber.compareTo(expectedNumber) == 0;
        }
        return String.valueOf(actual).equals(expected);
    }

    /**
     * Numeric ordering when both sides parse as numbers. The lexicographic fallback is deliberate and
     * makes ISO-8601 dates order correctly; it is case sensitive, so {@code "Z"} sorts before
     * {@code "a"}.
     */
    private static int compare(Object actual, String expected) {
        BigDecimal actualNumber = toNumber(actual);
        BigDecimal expectedNumber = toNumber(expected);
        if (actualNumber != null && expectedNumber != null) {
            return actualNumber.compareTo(expectedNumber);
        }
        return String.valueOf(actual).compareTo(expected);
    }

    /**
     * Membership for a multi-valued answer such as a MULTI_SELECT, substring containment for a single
     * value.
     */
    private static boolean contains(Object actual, String expected) {
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(element -> String.valueOf(element).equals(expected));
        }
        if (actual instanceof Object[] array) {
            return Arrays.stream(array).anyMatch(element -> String.valueOf(element).equals(expected));
        }
        return String.valueOf(actual).contains(expected);
    }

    /** The value as a number, or null when it does not represent one. */
    private static BigDecimal toNumber(Object value) {
        if (value instanceof BigDecimal number) {
            return number;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof Boolean || value instanceof Collection<?> || value instanceof Object[]) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }
}
