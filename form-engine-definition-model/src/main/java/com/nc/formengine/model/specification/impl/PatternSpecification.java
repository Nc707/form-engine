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
import java.util.regex.PatternSyntaxException;

/**
 * Specification that validates a string value matches a regex pattern.
 * Only applicable to TEXT fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatternSpecification implements FieldSpecification {
    
    private String pattern;
    private String errorMessage;
    
    private static final Set<FieldType> APPLICABLE_TYPES = Set.of(FieldType.TEXT);
    
    public PatternSpecification(String pattern) {
        this.pattern = pattern;
        this.errorMessage = "El valor no cumple con el formato requerido";
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
            return SpecificationResult.notSatisfied("El valor no es una cadena de texto");
        }
        
        try {
            if (!Pattern.matches(pattern, str)) {
                return SpecificationResult.notSatisfied(errorMessage);
            }
        } catch (PatternSyntaxException e) {
            return SpecificationResult.notSatisfied("Patrón de expresión regular inválido: " + e.getMessage());
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
