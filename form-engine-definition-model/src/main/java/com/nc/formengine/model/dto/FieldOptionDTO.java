package com.nc.formengine.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldOptionDTO {

    private Long id;
    private Long fieldDefinitionId;
    private String label;
    private String value;
    private Integer orderIndex;
}
