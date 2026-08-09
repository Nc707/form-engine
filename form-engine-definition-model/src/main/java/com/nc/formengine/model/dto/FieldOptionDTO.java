package com.nc.formengine.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "fieldDefinitionId is required")
    @Positive(message = "fieldDefinitionId must be greater than 0")
    private Long fieldDefinitionId;

    @NotBlank(message = "label is required")
    @Size(max = 255, message = "label must be at most 255 characters")
    private String label;

    @NotBlank(message = "value is required")
    @Size(max = 255, message = "value must be at most 255 characters")
    private String value;

    @PositiveOrZero(message = "orderIndex must not be negative")
    private Integer orderIndex;
}
