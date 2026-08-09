package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.ComponentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

    @NotNull(message = "fieldDefinitionId is required")
    @Positive(message = "fieldDefinitionId must be greater than 0")
    private Long fieldDefinitionId;

    @PositiveOrZero(message = "row must not be negative")
    private Integer row;

    @PositiveOrZero(message = "column must not be negative")
    private Integer column;

    @Positive(message = "colspan must be greater than 0")
    private Integer colspan;

    @Positive(message = "rowspan must be greater than 0")
    private Integer rowspan;

    private ComponentType componentType;
    private Map<String, Object> customProperties;
    private Boolean visible;
}
