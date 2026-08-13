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
 * Specification that validates maximum numeric value.
 * Only applicable to NUMBER fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaxValueSpecification implements FieldSpecification {
    
    private Double maxValue;
    private String errorMessage;
    
    private static final Set<FieldType> APPLICABLE_TYPES = Set.of(FieldType.NUMBER);
    
    public MaxValueSpecification(Double maxValue) {
        this.maxValue = maxValue;
        this.errorMessage = "This answer cannot be greater than " + maxValue;
    }
    
    @Override
    public SpecificationResult isSatisfiedBy(Object value, FieldContext context) {
        // Check if this specification applies to the field type
        if (context.getFieldType() != null && !APPLICABLE_TYPES.contains(context.getFieldType())) {
            return SpecificationResult.satisfied(); // Not applicable, so pass
        }
        
        if (value == null) {
            return SpecificationResult.satisfied(); // Presence is the field's required flag, not a rule
        }
        
        Double numValue;
        try {
            if (value instanceof Number number) {
                numValue = number.doubleValue();
            } else if (value instanceof String str) {
                numValue = Double.parseDouble(str);
            } else {
                return SpecificationResult.notSatisfied("This answer is not a number");
            }
        } catch (NumberFormatException e) {
            return SpecificationResult.notSatisfied("This answer is not a number");
        }
        
        if (numValue > maxValue) {
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
