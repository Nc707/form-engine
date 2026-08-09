package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.FieldSpecification;
import com.nc.formengine.model.specification.SpecificationResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Specification that validates a string value is not blank.
 * Only applicable to TEXT fields.
 *
 * <p>Whitespace counts as empty: a value made only of spaces is an answer the user did not give.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotEmptySpecification implements FieldSpecification {

    private String errorMessage = "El campo no puede estar vacío";

    private static final Set<FieldType> APPLICABLE_TYPES = Set.of(FieldType.TEXT);

    @Override
    public SpecificationResult isSatisfiedBy(Object value, FieldContext context) {
        // Check if this specification applies to the field type
        if (context.getFieldType() != null && !APPLICABLE_TYPES.contains(context.getFieldType())) {
            return SpecificationResult.satisfied(); // Not applicable, so pass
        }

        if (value == null) {
            return SpecificationResult.satisfied(); // Use NotNullSpecification for null checks
        }

        if (!(value instanceof String str)) {
            return SpecificationResult.notSatisfied("El valor no es una cadena de texto");
        }

        if (str.trim().isEmpty()) {
            return SpecificationResult.notSatisfied(errorMessage);
        }

        return SpecificationResult.satisfied();
    }

    /**
     * Gets the field types this specification applies to.
     *
     * @return set of applicable field types
     */
    public static Set<FieldType> getApplicableTypes() {
        return APPLICABLE_TYPES;
    }
}
