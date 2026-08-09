package com.nc.formengine.ui.responses;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.enums.FieldType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads a stored answer back out.
 *
 * <p>This is the inverse of {@link com.nc.formengine.ui.shared.FieldComponentFactory}, which decides
 * how each {@link FieldType} is written down: a checkbox becomes {@code "true"}, a date becomes an
 * ISO string, a multi-select becomes one comma-joined string, and a select stores an option's value
 * rather than the label the user actually picked. Those two classes have to agree, so change them
 * together.
 *
 * <p>Anything that does not parse is shown as it was stored. A viewer's job is to report what is in
 * the database, and a value the current definition cannot explain is exactly what someone reading
 * old submissions needs to see.
 */
final class AnswerFormatter {

    /** The separator {@code FieldComponentFactory} joins multi-select answers with. */
    private static final String MULTI_VALUE_SEPARATOR = ",";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AnswerFormatter() {
    }

    static String format(FieldDefinitionDTO field, String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (field == null || field.getType() == null) {
            return value;
        }

        return switch (field.getType()) {
            case BOOLEAN -> formatBoolean(value);
            case DATE -> formatDate(value);
            case SELECT -> labelOf(field, value);
            case MULTI_SELECT -> formatMultiSelect(field, value);
            case TEXT, NUMBER -> value;
        };
    }

    private static String formatBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return "Sí";
        }
        if ("false".equalsIgnoreCase(value)) {
            return "No";
        }
        return value;
    }

    private static String formatDate(String value) {
        try {
            return LocalDate.parse(value).format(DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            return value;
        }
    }

    private static String formatMultiSelect(FieldDefinitionDTO field, String value) {
        return Arrays.stream(value.split(MULTI_VALUE_SEPARATOR))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(part -> labelOf(field, part))
                .collect(Collectors.joining(", "));
    }

    /**
     * Options carry a label for people and a value for the engine, and the submission stores the
     * value. An option that has since been removed leaves its value with nothing to name it, so the
     * value itself is shown.
     */
    private static String labelOf(FieldDefinitionDTO field, String value) {
        List<FieldOptionDTO> options = field.getOptions();
        if (options == null) {
            return value;
        }
        return options.stream()
                .filter(option -> value.equals(option.getValue()))
                .map(FieldOptionDTO::getLabel)
                .findFirst()
                .orElse(value);
    }
}
