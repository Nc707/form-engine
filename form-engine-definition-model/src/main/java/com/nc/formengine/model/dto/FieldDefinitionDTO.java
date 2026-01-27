package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private Integer minLength;
    private Integer maxLength;
    private Double minValue;
    private Double maxValue;
    private String regexPattern;
    private List<FieldDependencyDTO> triggeredDependencies;
    private List<FieldDependencyDTO> myDependencies;
    private List<FieldOptionDTO> options;
}
