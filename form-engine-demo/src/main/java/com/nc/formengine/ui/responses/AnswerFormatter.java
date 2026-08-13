package com.nc.formengine.ui.responses;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.validation.AnswerCodec;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads a stored answer back out.
 *
 * <p>How each {@link FieldType} is written down comes from {@link AnswerCodec}, so this reads the same
 * format the engine validates and the renderer writes, rather than a third guess at it. What is left
 * here is presentation: a boolean reads as Yes or No, a date in the local format, and a choice as the
 * label the user picked rather than the value stored for it.
 *
 * <p>Anything that does not parse is shown as it was stored. A viewer's job is to report what is in
 * the database, and a value the current definition cannot explain is exactly what someone reading
 * old submissions needs to see.
 */
final class AnswerFormatter {

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
        if (AnswerCodec.TRUE.equalsIgnoreCase(value)) {
            return "Yes";
        }
        if (AnswerCodec.FALSE.equalsIgnoreCase(value)) {
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
        return AnswerCodec.decodeSelections(value).stream()
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
