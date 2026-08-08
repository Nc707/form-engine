package com.nc.formengine.model.specification;

/**
 * Core interface for field validation using the Specification pattern.
 * Allows composable, reusable validation rules that can be combined using logical operators.
 */
@FunctionalInterface
public interface FieldSpecification {
    
    /**
     * Evaluates if the given value satisfies this specification.
     *
     * @param value   the value to validate
     * @param context additional context for validation (field type, form data, etc.)
     * @return the result of the specification evaluation
     */
    SpecificationResult isSatisfiedBy(Object value, FieldContext context);
    
    /**
     * Combines this specification with another using logical AND.
     * Both specifications must be satisfied.
     *
     * @param other the specification to combine with
     * @return a new specification representing the AND operation
     */
    default FieldSpecification and(FieldSpecification other) {
        return (value, context) -> {
            SpecificationResult result = this.isSatisfiedBy(value, context);
            if (!result.isSatisfied()) {
                return result;
            }
            return other.isSatisfiedBy(value, context);
        };
    }
    
    /**
     * Combines this specification with another using logical OR.
     * At least one specification must be satisfied.
     *
     * @param other the specification to combine with
     * @return a new specification representing the OR operation
     */
    default FieldSpecification or(FieldSpecification other) {
        return (value, context) -> {
            SpecificationResult result = this.isSatisfiedBy(value, context);
            if (result.isSatisfied()) {
                return result;
            }
            return other.isSatisfiedBy(value, context);
        };
    }
    
    /**
     * Negates this specification.
     *
     * @return a new specification representing the NOT operation
     */
    default FieldSpecification not() {
        return (value, context) -> {
            SpecificationResult result = this.isSatisfiedBy(value, context);
            return result.isSatisfied() 
                ? SpecificationResult.notSatisfied("Negated condition failed")
                : SpecificationResult.satisfied();
        };
    }
}
