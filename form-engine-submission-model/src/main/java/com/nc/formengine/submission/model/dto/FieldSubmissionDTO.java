package com.nc.formengine.submission.model.dto;

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
    private Long formSubmissionId;
    private Long fieldDefinitionId;
    private String fieldName;
    private String value;
}
