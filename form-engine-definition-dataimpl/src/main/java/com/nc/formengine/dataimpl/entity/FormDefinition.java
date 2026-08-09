package com.nc.formengine.dataimpl.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.nc.formengine.model.enums.FormDefinitionStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A form, at one version.
 *
 * <p>A {@code code} identifies a form across its whole history, not a single row: every version of
 * the same form shares it. Uniqueness therefore has to be on {@code (code, version)}, which is what
 * makes a second version possible at all.
 */
@Entity
@Table(
        name = "form_definitions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_form_definitions_code_version",
                columnNames = {"code", "version"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FormDefinitionStatus status = FormDefinitionStatus.DRAFT;

    @OneToMany(mappedBy = "formDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    @ToString.Exclude
    private List<FieldDefinition> fields = new ArrayList<>();

    @OneToMany(mappedBy = "formDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<FormLayout> layouts = new ArrayList<>();

    /**
     * Identity is the persistent id only. Lombok's {@code @Data} would derive equals/hashCode from
     * every field, including the two bidirectional collections, which recurses through the
     * back-references and forces lazy collections to load.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FormDefinition that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}