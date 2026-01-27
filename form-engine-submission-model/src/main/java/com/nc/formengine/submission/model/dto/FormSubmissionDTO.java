package com.nc.formengine.submission.model.dto;

import com.nc.formengine.submission.model.enums.SubmissionStatus;
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
    private Long formDefinitionId;
    private String formCode;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private SubmissionStatus status;
    
    @Builder.Default
    private List<FieldSubmissionDTO> fieldSubmissions = new ArrayList<>();
}
