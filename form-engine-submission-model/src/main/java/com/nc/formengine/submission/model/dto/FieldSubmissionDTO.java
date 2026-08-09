package com.nc.formengine.submission.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSubmissionDTO {

    private Long id;

    @NotNull(message = "formSubmissionId is required")
    @Positive(message = "formSubmissionId must be greater than 0")
    private Long formSubmissionId;

    @NotNull(message = "fieldDefinitionId is required")
    @Positive(message = "fieldDefinitionId must be greater than 0")
    private Long fieldDefinitionId;

    // No @NotBlank: fieldName is denormalised from the field definition on save, so callers
    // legitimately omit it.
    private String fieldName;

    private String value;
}
