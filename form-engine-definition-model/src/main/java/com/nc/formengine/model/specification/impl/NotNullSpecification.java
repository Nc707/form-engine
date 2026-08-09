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
 * Specification that validates a value is present.
 * Applicable to every field type.
 *
 * <p>This is the one specification that must not pass on a null value: every other implementation
 * treats null as "nothing to check" and delegates the presence check here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotNullSpecification implements FieldSpecification {

    private String errorMessage = "El campo es obligatorio";

    private static final Set<FieldType> APPLICABLE_TYPES = Set.of(
        FieldType.TEXT, FieldType.NUMBER, FieldType.DATE,
        FieldType.BOOLEAN, FieldType.SELECT, FieldType.MULTI_SELECT
    );

    @Override
    public SpecificationResult isSatisfiedBy(Object value, FieldContext context) {
        // Check if this specification applies to the field type
        if (context.getFieldType() != null && !APPLICABLE_TYPES.contains(context.getFieldType())) {
            return SpecificationResult.satisfied(); // Not applicable, so pass
        }

        if (value == null) {
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
