package com.nc.formengine.model.specification.impl;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.specification.FieldContext;
import com.nc.formengine.model.specification.FieldSpecification;
import com.nc.formengine.model.specification.SpecificationResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Specification that validates an email address format.
 * Only applicable to TEXT fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailSpecification implements FieldSpecification {
    
    private String errorMessage = "El formato del email es inválido";
    
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern PATTERN = Pattern.compile(EMAIL_PATTERN);
    
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
        
        if (!PATTERN.matcher(str).matches()) {
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
