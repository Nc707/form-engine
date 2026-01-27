package com.nc.formengine.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormDefinitionDTO {

    private Long id;
    private String code;
    private String title;
    private String description;
    private Integer version;
    
    @Builder.Default
    private List<FieldDefinitionDTO> fields = new ArrayList<>();
}
