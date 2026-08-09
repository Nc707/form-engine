package com.nc.formengine.submission.model.dto;

import com.nc.formengine.submission.model.enums.SubmissionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmissionDTO {

    private Long id;

    @NotNull(message = "formDefinitionId is required")
    @Positive(message = "formDefinitionId must be greater than 0")
    private Long formDefinitionId;

    @Size(max = 100, message = "formCode must be at most 100 characters")
    private String formCode;

    @NotBlank(message = "submittedBy is required")
    @Size(max = 255, message = "submittedBy must be at most 255 characters")
    private String submittedBy;

    private LocalDateTime submittedAt;

    @NotNull(message = "status is required")
    private SubmissionStatus status;
    
    @Builder.Default
    private List<FieldSubmissionDTO> fieldSubmissions = new ArrayList<>();
}
