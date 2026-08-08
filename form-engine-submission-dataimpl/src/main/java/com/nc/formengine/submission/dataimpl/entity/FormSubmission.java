package com.nc.formengine.submission.dataimpl.entity;

import com.nc.formengine.submission.model.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A filled-in form.
 *
 * <p>Intentionally decoupled from the definition side: {@code formDefinitionId} is a plain id with
 * no foreign key, and {@code formCode} is a snapshot. This keeps the submission modules free of any
 * compile-time dependency on the definition entities.
 */
@Entity
@Table(name = "form_submissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_definition_id", nullable = false)
    private Long formDefinitionId;

    @Column(name = "form_code", nullable = false)
    private String formCode;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @OneToMany(mappedBy = "formSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<FieldSubmission> fieldSubmissions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    /**
     * Identity is the persistent id only. Lombok's {@code @Data} would derive equals/hashCode from
     * every field, including {@code fieldSubmissions}, which recurses through the back-reference and
     * forces lazy collections to load.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FormSubmission that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
