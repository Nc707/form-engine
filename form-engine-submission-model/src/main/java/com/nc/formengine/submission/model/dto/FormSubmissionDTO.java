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

    // NOT NULL in the schema, so demanded here too: omitting it used to fail as a 500 on the way in.
    @NotBlank(message = "formCode is required")
    @Size(max = 100, message = "formCode must be at most 100 characters")
    private String formCode;

    @NotBlank(message = "author is required")
    @Size(max = 255, message = "author must be at most 255 characters")
    private String author;

    /** When it was started. Set on insert; a value sent by a caller is kept. */
    private LocalDateTime createdAt;

    /** When it was sent, or null while it never was. Only submitting sets this. */
    private LocalDateTime submittedAt;

    /**
     * Where the submission is in its lifecycle.
     *
     * <p>Deliberately not {@code @NotNull}: the status is owned by the lifecycle, not by the caller.
     * A new submission starts as a {@code DRAFT} and moves on only through
     * {@code FormSubmissionWorkflowService}.
     */
    private SubmissionStatus status;

    @Builder.Default
    private List<FieldSubmissionDTO> fieldSubmissions = new ArrayList<>();
}
