package com.nc.formengine.ui.builder;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.specification.RestrictionTypeRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Everything the builder refuses to save, checked before any service is called.
 *
 * <p>The rules here are not a copy of the engine's. Most of them guard against forms the engine
 * would accept quite happily and that would then misbehave in ways nobody could diagnose from the
 * outside: two fields sharing a name, a select with no options, a restriction whose parameter is
 * missing and therefore validates nothing, a dependency whose trigger value can never occur. The
 * few that do mirror an engine rule are here so the user hears about it while they can still fix it,
 * rather than as an exception after pressing save.
 *
 * <p>Pure and static on purpose: this is the part of the builder worth testing exhaustively, and it
 * costs nothing to run.
 */
final class BuilderValidation {

    /**
     * A code identifies a form across every one of its versions and can never be changed afterwards
     * — {@code FormDefinitionService.update} overwrites whatever a caller sends with the stored one.
     * Keeping it to lowercase identifiers keeps it usable as a key everywhere else.
     */
    private static final Pattern CODE = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    /** Field names are keys, not prose: the validator and the dependency engine look fields up by name. */
    private static final Pattern FIELD_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static final Set<FieldType> OPTION_TYPES =
            EnumSet.of(FieldType.SELECT, FieldType.MULTI_SELECT);

    private static final Set<FieldType> ORDERED_TYPES =
            EnumSet.of(FieldType.NUMBER, FieldType.DATE);

    private static final Set<FieldType> CONTAINS_TYPES =
            EnumSet.of(FieldType.TEXT, FieldType.MULTI_SELECT);

    private BuilderValidation() {
    }

    /**
     * Checks the details of a form about to be created.
     *
     * <p>The code is only checked for shape here; whether it is free is a question for the service,
     * and the caller asks {@code existsByCode} separately.
     *
     * @return the problems found, empty when there are none
     */
    static List<String> validateNewForm(String code, String title) {
        List<String> problems = new ArrayList<>();
        if (isBlank(code)) {
            problems.add("The form needs a code.");
        } else if (!CODE.matcher(code).matches()) {
            problems.add("The code must start with a lowercase letter and contain only lowercase "
                    + "letters, digits and underscores.");
        }
        if (isBlank(title)) {
            problems.add("The form needs a title.");
        }
        return problems;
    }

    /**
     * Checks one field, together with the restrictions and options it carries.
     *
     * @param field    the field being saved
     * @param siblings the form's other fields, not including this one
     * @return the problems found, empty when there are none
     */
    static List<String> validateField(FieldDefinitionDTO field, List<FieldDefinitionDTO> siblings) {
        List<String> problems = new ArrayList<>();
        if (field == null) {
            problems.add("There is no field to save.");
            return problems;
        }

        validateFieldName(field, siblings, problems);

        if (isBlank(field.getLabel())) {
            problems.add("The field needs a label.");
        }
        if (field.getType() == null) {
            problems.add("The field needs a type.");
        }

        validateOptions(field, problems);
        validateRestrictions(field, problems);
        return problems;
    }

    private static void validateFieldName(FieldDefinitionDTO field,
                                          List<FieldDefinitionDTO> siblings,
                                          List<String> problems) {
        String name = field.getName();
        if (isBlank(name)) {
            problems.add("The field needs a name.");
            return;
        }
        if (!FIELD_NAME.matcher(name).matches()) {
            problems.add("The field name must start with a letter or underscore and contain only "
                    + "letters, digits and underscores.");
            return;
        }
        // Nothing in the schema enforces this, but a form with two fields of one name renders one
        // input for both and validates only one of them, with nothing anywhere to say why.
        boolean taken = siblings != null && siblings.stream()
                .filter(Objects::nonNull)
                .map(FieldDefinitionDTO::getName)
                .filter(Objects::nonNull)
                .anyMatch(other -> other.equalsIgnoreCase(name));
        if (taken) {
            problems.add("Another field is already called '" + name + "'.");
        }
    }

    private static void validateOptions(FieldDefinitionDTO field, List<String> problems) {
        List<FieldOptionDTO> options = field.getOptions() == null ? List.of() : field.getOptions();
        boolean takesOptions = OPTION_TYPES.contains(field.getType());

        if (!takesOptions) {
            if (!options.isEmpty()) {
                problems.add("Only a select field can have options.");
            }
            return;
        }

        if (options.isEmpty()) {
            problems.add("A select field needs at least one option.");
            return;
        }

        Set<String> values = new HashSet<>();
        for (FieldOptionDTO option : options) {
            if (option == null) {
                continue;
            }
            if (isBlank(option.getLabel())) {
                problems.add("Every option needs a label.");
            }
            if (isBlank(option.getValue())) {
                problems.add("Every option needs a value.");
            } else if (!values.add(option.getValue())) {
                // The renderer looks an option up by value and takes the first match, so a repeated
                // value makes one of the two options unreachable.
                problems.add("Two options share the value '" + option.getValue() + "'.");
            }
        }
    }

    private static void validateRestrictions(FieldDefinitionDTO field, List<String> problems) {
        List<FieldRestrictionDTO> restrictions =
                field.getRestrictions() == null ? List.of() : field.getRestrictions();

        Set<RestrictionType> seen = EnumSet.noneOf(RestrictionType.class);
        for (FieldRestrictionDTO restriction : restrictions) {
            if (restriction == null) {
                continue;
            }
            RestrictionType type = restriction.getRestrictionType();
            if (type == null) {
                problems.add("Every rule needs a type.");
                continue;
            }
            if (!seen.add(type)) {
                problems.add("The rule " + type + " is set twice on this field.");
            }
            if (field.getType() != null && !RestrictionTypeRegistry.isApplicable(type, field.getType())) {
                problems.add("The rule " + type + " does not apply to a " + field.getType() + " field.");
                continue;
            }
            validateRestrictionParameter(type, restriction.getParameters(), problems);
        }

        validateBounds(restrictions, RestrictionType.MIN_LENGTH, RestrictionType.MAX_LENGTH,
                "The minimum length is greater than the maximum length.", problems);
        validateBounds(restrictions, RestrictionType.MIN_VALUE, RestrictionType.MAX_VALUE,
                "The minimum value is greater than the maximum value.", problems);
    }

    private static void validateRestrictionParameter(RestrictionType type,
                                                     Map<String, Object> parameters,
                                                     List<String> problems) {
        RestrictionParameterSpec spec = RestrictionParameterSpec.of(type);
        if (spec == null || !spec.hasParameter()) {
            return;
        }

        Object value = RestrictionParameterSpec.read(type, parameters);
        if (value == null) {
            // This is the one that would otherwise pass silently: the engine turns a rule with no
            // parameter into one that accepts everything, so it would look configured and do nothing.
            problems.add("The rule " + type + " needs a " + spec.label().toLowerCase(Locale.ROOT)
                    + ". Without one it would accept every answer.");
            return;
        }

        if (spec.kind() == RestrictionParameterSpec.Kind.INTEGER
                && ((Integer) value) < 0) {
            problems.add("The rule " + type + " cannot use a negative length.");
        }

        if (spec.kind() == RestrictionParameterSpec.Kind.REGEX) {
            try {
                Pattern.compile(value.toString());
            } catch (PatternSyntaxException ex) {
                problems.add("The pattern is not a valid regular expression: " + ex.getDescription());
            }
        }
    }

    private static void validateBounds(List<FieldRestrictionDTO> restrictions,
                                       RestrictionType lowerType,
                                       RestrictionType upperType,
                                       String message,
                                       List<String> problems) {
        Double lower = numberOf(restrictions, lowerType);
        Double upper = numberOf(restrictions, upperType);
        if (lower != null && upper != null && lower > upper) {
            problems.add(message);
        }
    }

    private static Double numberOf(List<FieldRestrictionDTO> restrictions, RestrictionType type) {
        return restrictions.stream()
                .filter(Objects::nonNull)
                .filter(restriction -> restriction.getRestrictionType() == type)
                .map(restriction -> RestrictionParameterSpec.read(type, restriction.getParameters()))
                .filter(Number.class::isInstance)
                .map(value -> ((Number) value).doubleValue())
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks a conditional dependency against the fields it names.
     *
     * @param dependency the dependency being saved
     * @param fields     the form's fields as currently stored
     * @return the problems found, empty when there are none
     */
    static List<String> validateDependency(FieldDependencyDTO dependency,
                                           List<FieldDefinitionDTO> fields) {
        List<String> problems = new ArrayList<>();
        if (dependency == null) {
            problems.add("There is no dependency to save.");
            return problems;
        }

        FieldDefinitionDTO trigger = findField(fields, dependency.getTriggerFieldId());
        FieldDefinitionDTO dependent = findField(fields, dependency.getDependentFieldId());

        if (trigger == null) {
            problems.add("Choose the field the rule watches.");
        }
        if (dependent == null) {
            problems.add("Choose the field the rule affects.");
        }
        if (trigger != null && dependent != null
                && Objects.equals(trigger.getId(), dependent.getId())) {
            problems.add("A field cannot depend on itself.");
        }
        if (dependency.getEffect() == null) {
            problems.add("Choose what the rule does.");
        }

        if (dependency.getCondition() == null) {
            problems.add("Choose the condition to check.");
        } else if (trigger != null
                && !conditionsFor(trigger.getType()).contains(dependency.getCondition())) {
            problems.add("The condition " + dependency.getCondition() + " does not apply to a "
                    + trigger.getType() + " field.");
        }

        if (isBlank(dependency.getTriggerValue())) {
            // A condition is never satisfied when there is nothing to compare against, so a blank
            // trigger value is a rule that can never fire.
            problems.add("The rule needs a value to compare against.");
        }

        return problems;
    }

    /**
     * The conditions worth offering for a trigger field of this type.
     *
     * <p>The evaluator compares numerically when both sides parse as numbers and lexicographically
     * otherwise. That is right for numbers and for ISO dates, and meaningless for anything else, so
     * ordering comparisons are only offered where they mean something. {@code CONTAINS} is likewise
     * only useful where the answer is a longer string: free text, or the comma-joined value of a
     * multi-select.
     *
     * @param triggerType the trigger field's type; may be null
     * @return the conditions to offer, never null
     */
    static List<DependencyCondition> conditionsFor(FieldType triggerType) {
        if (triggerType == null) {
            return List.of();
        }
        return Arrays.stream(DependencyCondition.values())
                .filter(condition -> switch (condition) {
                    case EQUALS, NOT_EQUALS -> true;
                    case GREATER_THAN, LESS_THAN -> ORDERED_TYPES.contains(triggerType);
                    case CONTAINS -> CONTAINS_TYPES.contains(triggerType);
                })
                .toList();
    }

    private static FieldDefinitionDTO findField(List<FieldDefinitionDTO> fields, Long id) {
        if (fields == null || id == null) {
            return null;
        }
        return fields.stream()
                .filter(Objects::nonNull)
                .filter(field -> id.equals(field.getId()))
                .findFirst()
                .orElse(null);
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
