package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.RestrictionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for field restriction/validation rules.
 * Represents a single validation rule that can be applied to a field.
 *
 * <p>Which field types a restriction applies to is not carried here: it follows from the restriction
 * type, and {@code RestrictionTypeRegistry} is the one place that says so.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldRestrictionDTO {
    
    /**
     * Unique identifier for the restriction.
     */
    private Long id;
    
    /**
     * ID of the field definition this restriction belongs to.
     */
    private Long fieldDefinitionId;
    
    /**
     * Type of restriction (MIN_LENGTH, MAX_VALUE, PATTERN, etc.).
     */
    private RestrictionType restrictionType;
    
    /**
     * Configuration parameters for the restriction.
     * Examples:
     * - For MIN_LENGTH: {"minLength": 5}
     * - For MAX_VALUE: {"maxValue": 100}
     * - For PATTERN: {"pattern": "^[A-Z]+$"}
     */
    private Map<String, Object> parameters;

    /**
     * Custom error message to display when validation fails.
     * If null, a default message will be used.
     */
    private String errorMessage;

    /**
     * Order in which this restriction should be evaluated.
     * Lower values are evaluated first.
     */
    private Integer orderIndex;
}
