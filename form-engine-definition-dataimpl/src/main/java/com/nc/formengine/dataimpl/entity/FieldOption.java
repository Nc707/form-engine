package com.nc.formengine.dataimpl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Entity
// An answer names an option by its value, and the renderer resolves it by taking the first match, so a
// repeated value would make one of the two options unreachable and unreadable.
@Table(name = "field_options",
    uniqueConstraints = @UniqueConstraint(name = "uq_option_value_per_field",
        columnNames = {"field_definition_id", "`value`"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "field_definition_id")
    @ToString.Exclude
    private FieldDefinition fieldDefinition;

    private String label;

    @Column(name = "`value`")
    private String value;

    private Integer orderIndex;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FieldOption that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}