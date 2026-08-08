package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for field definition.
 * Represents a field in a form with its configuration and validation rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinitionDTO {

    private Long id;
    private Long formDefinitionId;
    private String name;
    private String label;
    private FieldType type;
    private Integer orderIndex;
    private Boolean required;
    
    /**
     * List of validation restrictions for this field.
     * Uses the Specification pattern for flexible, composable validation rules.
     */
    private List<FieldRestrictionDTO> restrictions;
    
    /**
     * Options for SELECT and MULTI_SELECT fields.
     */
    private List<FieldOptionDTO> options;
}
