package com.nc.formengine.model.specification;

import com.nc.formengine.model.enums.FieldType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.Map;

/**
 * Context information for field validation.
 * Provides additional data needed during specification evaluation.
 */
@Data
@Getter
@Builder
public class FieldContext {
    
    /**
     * The type of the field being validated.
     */
    private FieldType fieldType;
    
    /**
     * The name of the field being validated.
     */
    private String fieldName;
    
    /**
     * All form data for cross-field validation.
     * Key: field name, Value: field value
     */
    private Map<String, Object> formData;
    
    /**
     * Additional metadata that might be needed for validation.
     */
    private Map<String, Object> metadata;
}
