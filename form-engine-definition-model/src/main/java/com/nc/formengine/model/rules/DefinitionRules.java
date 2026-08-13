package com.nc.formengine.model.rules;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.specification.RestrictionParameter;
import com.nc.formengine.model.specification.RestrictionTypeRegistry;
import com.nc.formengine.model.validation.AnswerCodec;

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
 * What makes a form definition coherent, as opposed to merely well formed.
 *
 * <p>These rules used to live in the Vaadin builder, and only there. Every one of them was therefore
 * bypassable through the REST API — which is how a form could be stored with two fields of one name,
 * a select with nothing to select, or a restriction whose parameter was missing and which consequently
 * validated nothing at all. The builder was the real boundary of the model, and the other consumer was
 * an unguarded way into the same data.
 *
 * <p>They live here, in the model, so that the business layer can enforce them and the builder can
 * still show them <em>before</em> a save. Pure and static: no container, no database, no I/O — a rule
 * that needs to read the rest of the form takes it as an argument.
 *
 * <p>Each check returns the reasons a thing is not acceptable, in the wording a person should read.
 * An empty list means acceptable.
 */
public final class DefinitionRules {

    /**
     * A code identifies a form across every one of its versions and can never be changed afterwards.
     * Keeping it to lowercase identifiers keeps it usable as a key everywhere else.
     */
    public static final Pattern FORM_CODE = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    /** Field names are keys, not prose: the validator and the dependency engine look fields up by name. */
    public static final Pattern FIELD_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static final Set<FieldType> OPTION_TYPES =
        EnumSet.of(FieldType.SELECT, FieldType.MULTI_SELECT);

    private static final Set<FieldType> ORDERED_TYPES =
        EnumSet.of(FieldType.NUMBER, FieldType.DATE);

    private static final Set<FieldType> CONTAINS_TYPES =
        EnumSet.of(FieldType.TEXT, FieldType.MULTI_SELECT);

    private DefinitionRules() {
    }

    // --- forms ------------------------------------------------------------------------------------

    /**
     * Checks a form's own details.
     *
     * <p>Only shape. Whether the code is already taken is a question for whoever can see the other
     * forms.
     *
     * @param code  the form's code
     * @param title the form's title
     * @return the reasons it is not acceptable, empty when it is
     */
    public static List<String> checkForm(String code, String title) {
        List<String> problems = new ArrayList<>();
        if (isBlank(code)) {
            problems.add("The form needs a code.");
        } else if (!FORM_CODE.matcher(code).matches()) {
            problems.add("The code must start with a lowercase letter and contain only lowercase "
                + "letters, digits and underscores.");
        }
        if (isBlank(title)) {
            problems.add("The form needs a title.");
        }
        return problems;
    }

    // --- fields -----------------------------------------------------------------------------------

    /**
     * Checks one field, together with the restrictions and options it carries.
     *
     * @param field    the field being saved
     * @param siblings the form's other fields, not including this one; may be null
     * @return the reasons it is not acceptable, empty when it is
     */
    public static List<String> checkField(FieldDefinitionDTO field, List<FieldDefinitionDTO> siblings) {
        List<String> problems = new ArrayList<>();
        if (field == null) {
            problems.add("There is no field to save.");
            return problems;
        }

        checkFieldName(field, siblings, problems);

        if (isBlank(field.getLabel())) {
            problems.add("The field needs a label.");
        }
        if (field.getType() == null) {
            problems.add("The field needs a type.");
        }

        checkOptions(field, problems);
        checkRestrictions(field, problems);
        return problems;
    }

    private static void checkFieldName(FieldDefinitionDTO field,
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
        // A form with two fields of one name renders one input for both and validates only one of
        // them, with nothing anywhere to say why: the dependency evaluator keys fields by name and the
        // last one of the form wins.
        boolean taken = siblings != null && siblings.stream()
            .filter(Objects::nonNull)
            .filter(other -> !Objects.equals(other.getId(), field.getId()) || field.getId() == null)
            .map(FieldDefinitionDTO::getName)
            .filter(Objects::nonNull)
            .anyMatch(other -> other.equalsIgnoreCase(name));
        if (taken) {
            problems.add("Another field is already called '" + name + "'.");
        }
    }

    private static void checkOptions(FieldDefinitionDTO field, List<String> problems) {
        List<FieldOptionDTO> options = field.getOptions() == null ? List.of() : field.getOptions();
        boolean takesOptions = OPTION_TYPES.contains(field.getType());

        if (!takesOptions) {
            if (!options.isEmpty()) {
                problems.add("Only a select field can have options.");
            }
            return;
        }

        if (options.isEmpty()) {
            // The engine checks an answer against the field's options, so a select with none offers
            // nothing that could ever be a valid answer.
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
                continue;
            }
            if (option.getValue().contains(AnswerCodec.SELECTION_SEPARATOR)) {
                // A multi-select answer joins its values with that separator and does not escape it,
                // so such an option could never be told apart from two others.
                problems.add("An option value cannot contain '" + AnswerCodec.SELECTION_SEPARATOR
                    + "': it is what separates the answers of a multi-select.");
            }
            if (!values.add(option.getValue())) {
                // The renderer looks an option up by value and takes the first match, so a repeated
                // value makes one of the two options unreachable.
                problems.add("Two options share the value '" + option.getValue() + "'.");
            }
        }
    }

    private static void checkRestrictions(FieldDefinitionDTO field, List<String> problems) {
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
            checkRestrictionParameter(type, restriction.getParameters(), problems);
        }

        checkBounds(restrictions, RestrictionType.MIN_LENGTH, RestrictionType.MAX_LENGTH,
            "The minimum length is greater than the maximum length.", problems);
        checkBounds(restrictions, RestrictionType.MIN_VALUE, RestrictionType.MAX_VALUE,
            "The minimum value is greater than the maximum value.", problems);
    }

    private static void checkRestrictionParameter(RestrictionType type,
                                                  Map<String, Object> parameters,
                                                  List<String> problems) {
        RestrictionParameter parameter = RestrictionParameter.of(type);
        if (parameter == null || !parameter.required()) {
            return;
        }

        Object value = readParameter(parameter, parameters);
        if (value == null) {
            // The one that would otherwise pass silently. Evaluation is permissive by design — a rule
            // nobody can satisfy would leave a form unsubmittable — so a rule with no parameter accepts
            // everything. Refusing to store it is what keeps that from meaning "looks configured, does
            // nothing".
            problems.add("The rule " + type + " needs a " + describe(parameter)
                + ". Without one it would accept every answer.");
            return;
        }

        if (parameter.kind() == RestrictionParameter.Kind.INTEGER && ((Number) value).intValue() < 0) {
            problems.add("The rule " + type + " cannot use a negative length.");
        }

        if (parameter.kind() == RestrictionParameter.Kind.REGEX) {
            try {
                Pattern.compile(value.toString());
            } catch (PatternSyntaxException ex) {
                problems.add("The pattern is not a valid regular expression: " + ex.getDescription());
            }
        }
    }

    /**
     * Reads a restriction's configured value, accepting every shape it can arrive in.
     *
     * <p>Parameters cross JSON on the way into the database and back out, so the same 5 turns up as an
     * {@code Integer}, a {@code Double} or a {@code String} depending on the route it took.
     */
    public static Object readParameter(RestrictionParameter parameter, Map<String, Object> parameters) {
        if (parameter == null || !parameter.required() || parameters == null) {
            return null;
        }
        Object stored = parameters.get(parameter.key());
        if (stored == null) {
            return null;
        }
        return switch (parameter.kind()) {
            case INTEGER -> asNumber(stored) == null ? null : asNumber(stored).intValue();
            case DECIMAL -> asNumber(stored) == null ? null : asNumber(stored).doubleValue();
            case REGEX -> stored.toString().isBlank() ? null : stored.toString();
            case NONE -> null;
        };
    }

    private static void checkBounds(List<FieldRestrictionDTO> restrictions,
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
        RestrictionParameter parameter = RestrictionParameter.of(type);
        return restrictions.stream()
            .filter(Objects::nonNull)
            .filter(restriction -> restriction.getRestrictionType() == type)
            .map(restriction -> readParameter(parameter, restriction.getParameters()))
            .filter(Number.class::isInstance)
            .map(value -> ((Number) value).doubleValue())
            .findFirst()
            .orElse(null);
    }

    // --- dependencies -----------------------------------------------------------------------------

    /**
     * Checks a conditional dependency against the fields it names.
     *
     * @param dependency the dependency being saved
     * @param fields     the form's fields as currently stored; may be null
     * @return the reasons it is not acceptable, empty when it is
     */
    public static List<String> checkDependency(FieldDependencyDTO dependency,
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
     * The conditions that mean something for a trigger field of this type.
     *
     * <p>The evaluator compares numerically when both sides parse as numbers and lexicographically
     * otherwise. That is right for numbers and for ISO dates, and meaningless for anything else, so
     * ordering comparisons only apply where they mean something. {@code CONTAINS} is likewise only
     * useful where the answer is a longer string: free text, or the joined value of a multi-select.
     *
     * @param triggerType the trigger field's type; may be null
     * @return the conditions that apply, never null
     */
    public static List<DependencyCondition> conditionsFor(FieldType triggerType) {
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

    // --- shared -----------------------------------------------------------------------------------

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

    private static String describe(RestrictionParameter parameter) {
        return switch (parameter.kind()) {
            case INTEGER, DECIMAL -> parameter.key().toLowerCase(Locale.ROOT);
            case REGEX -> "regular expression";
            case NONE -> "parameter";
        };
    }

    private static Number asNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String text) {
            try {
                return Double.valueOf(text.trim());
            } catch (NumberFormatException notANumber) {
                return null;
            }
        }
        return null;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
