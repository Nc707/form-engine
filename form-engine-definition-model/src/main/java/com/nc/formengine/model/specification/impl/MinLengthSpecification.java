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
 * Specification that validates minimum length of a string value.
 * Only applicable to TEXT fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinLengthSpecification implements FieldSpecification {
    
    private Integer minLength;
    private String errorMessage;
    
    private static final Set<FieldType> APPLICABLE_TYPES = Set.of(FieldType.TEXT);
    
    public MinLengthSpecification(Integer minLength) {
        this.minLength = minLength;
        this.errorMessage = "This answer must be at least " + minLength + " characters long";
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
        
        if (!(value instanceof String str)) {
            return SpecificationResult.notSatisfied("This answer is not text");
        }
        
        if (str.length() < minLength) {
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
