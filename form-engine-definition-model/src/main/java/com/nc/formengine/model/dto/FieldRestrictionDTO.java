package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

/**
 * DTO for field restriction/validation rules.
 * Represents a single validation rule that can be applied to a field.
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
     * Type of restriction (NOT_NULL, MIN_LENGTH, MAX_VALUE, etc.).
     */
    private RestrictionType restrictionType;
    
    /**
     * Configuration parameters for the restriction.
     * Examples:
     * - For MIN_LENGTH: {"minLength": 5}
     * - For RANGE: {"min": 0, "max": 100}
     * - For PATTERN: {"pattern": "^[A-Z]+$"}
     */
    private Map<String, Object> parameters;
    
    /**
     * Custom error message to display when validation fails.
     * If null, a default message will be used.
     */
    private String errorMessage;
    
    /**
     * Field types this restriction applies to.
     * If empty/null, applies to all field types.
     * Used for filtering restrictions by field type.
     */
    private Set<FieldType> applicableFieldTypes;
    
    /**
     * Order in which this restriction should be evaluated.
     * Lower values are evaluated first.
     */
    private Integer orderIndex;
}
