package com.nc.formengine.submission.dataimpl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "field_submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_submission_id", nullable = false)
    @ToString.Exclude
    private FormSubmission formSubmission;

    @Column(name = "field_definition_id", nullable = false)
    private Long fieldDefinitionId;

    /**
     * Denormalized snapshot of the field name at submission time.
     * Optional: the submission modules are decoupled from the definition modules,
     * so it can only be supplied by the caller.
     */
    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "`value`", columnDefinition = "TEXT")
    private String value;
}
