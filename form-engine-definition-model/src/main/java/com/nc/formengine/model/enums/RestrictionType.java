package com.nc.formengine.model.enums;

/**
 * Enumeration of available restriction types for field validation.
 */
public enum RestrictionType {
    
    /**
     * Validates that the value is not null.
     */
    NOT_NULL,
    
    /**
     * Validates that the string value is not empty.
     * Applicable to: TEXT
     */
    NOT_EMPTY,
    
    /**
     * Validates minimum string length.
     * Applicable to: TEXT
     */
    MIN_LENGTH,
    
    /**
     * Validates maximum string length.
     * Applicable to: TEXT
     */
    MAX_LENGTH,
    
    /**
     * Validates minimum numeric value.
     * Applicable to: NUMBER
     */
    MIN_VALUE,
    
    /**
     * Validates maximum numeric value.
     * Applicable to: NUMBER
     */
    MAX_VALUE,
    
    /**
     * Validates string matches a regex pattern.
     * Applicable to: TEXT
     */
    PATTERN,
    
    /**
     * Validates email format.
     * Applicable to: TEXT
     */
    EMAIL
}
