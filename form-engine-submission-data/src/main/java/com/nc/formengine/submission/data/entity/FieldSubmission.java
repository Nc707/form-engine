package com.nc.formengine.submission.data.entity;

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

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(columnDefinition = "TEXT")
    private String value;
}
