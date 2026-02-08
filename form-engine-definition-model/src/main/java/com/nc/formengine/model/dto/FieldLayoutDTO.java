package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.ComponentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldLayoutDTO {

    private Long id;
    private Long formLayoutId;
    private Long fieldDefinitionId;
    private Integer row;
    private Integer column;
    private Integer colspan;
    private Integer rowspan;
    private ComponentType componentType;
    private Map<String, Object> customProperties;
    private Boolean visible;
}
