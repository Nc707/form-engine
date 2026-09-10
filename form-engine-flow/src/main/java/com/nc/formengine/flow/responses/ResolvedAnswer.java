package com.nc.formengine.flow.responses;

/**
 * One answer, ready to show to a person.
 *
 * @param label        what the field was called, from the definition the submission was filled in
 *                     against — or the stored field name when the field is gone
 * @param displayValue the answer as text, already translated out of the storage encoding; empty when
 *                     the field was not answered
 * @param answered     whether the submission has a value for this field at all. A field the
 *                     definition declares but the submission never answered still gets a row, so the
 *                     reader sees what was left blank
 * @param retired      whether the field is gone from the definition. Its value is shown raw: with no
 *                     field definition there is no type to format it by and no options to name it
 */
public record ResolvedAnswer(String label, String displayValue, boolean answered, boolean retired) {
}
