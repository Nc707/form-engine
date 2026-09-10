package com.nc.formengine.flow.utils.builder;

import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;

/**
 * Puts a conditional dependency into words.
 *
 * <p>{@code SHOW / EQUALS / triggerValue} is how the engine stores a rule and a poor way to read
 * one. The list of a form's dependencies is the place a mistake is most likely to be spotted, and it
 * is only spottable if each line reads as the sentence the author meant.
 */
public final class DependencyText {

    private DependencyText() {
    }

    /** A whole rule, for the dependency list: {@code Show 'Passport' when 'Needs visa' is 'yes'}. */
    public static String sentence(FieldDependencyDTO dependency, String triggerLabel, String dependentLabel) {
        return effect(dependency.getEffect()) + " " + quote(dependentLabel)
                + " when " + quote(triggerLabel)
                + " " + condition(dependency.getCondition())
                + " " + quote(dependency.getTriggerValue());
    }

    /** The same rule from the affected field's side, for a note under it in the preview. */
    public static String note(FieldDependencyDTO dependency, String triggerLabel) {
        return outcome(dependency.getEffect()) + " when " + quote(triggerLabel)
                + " " + condition(dependency.getCondition())
                + " " + quote(dependency.getTriggerValue());
    }

    private static String effect(DependencyEffect effect) {
        if (effect == null) {
            return "Do something to";
        }
        return switch (effect) {
            case SHOW -> "Show";
            case HIDE -> "Hide";
            case REQUIRE -> "Require";
            case OPTIONAL -> "Make optional";
        };
    }

    private static String outcome(DependencyEffect effect) {
        if (effect == null) {
            return "Affected";
        }
        return switch (effect) {
            case SHOW -> "Shown";
            case HIDE -> "Hidden";
            case REQUIRE -> "Required";
            case OPTIONAL -> "Optional";
        };
    }

    private static String condition(DependencyCondition condition) {
        if (condition == null) {
            return "matches";
        }
        return switch (condition) {
            case EQUALS -> "is";
            case NOT_EQUALS -> "is not";
            case GREATER_THAN -> "is greater than";
            case LESS_THAN -> "is less than";
            case CONTAINS -> "contains";
        };
    }

    private static String quote(String text) {
        return "'" + (text == null || text.isBlank() ? "?" : text) + "'";
    }
}
