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
        this.errorMessage = "This answer is not in the expected format";
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
        
        try {
            if (!Pattern.matches(pattern, str)) {
                return SpecificationResult.notSatisfied(errorMessage);
            }
        } catch (PatternSyntaxException e) {
            // The rule itself is broken, which is the form author's mistake and not the respondent's,
            // so the message does not ask them to fix their answer. The field service refuses to store
            // a pattern that will not compile, so this is only reachable by building one by hand.
            return SpecificationResult.notSatisfied("This field's pattern rule is misconfigured "
                + "and could not be checked");
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
