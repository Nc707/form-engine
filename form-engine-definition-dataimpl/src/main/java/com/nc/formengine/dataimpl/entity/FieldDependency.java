package com.nc.formengine.dataimpl.entity;

import com.nc.formengine.model.enums.DependencyAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Entity
@Table(name = "field_dependencies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dependent_field_id", nullable = false)
    @ToString.Exclude
    private FieldDefinition dependentField;

    @ManyToOne
    @JoinColumn(name = "trigger_field_id", nullable = false)
    @ToString.Exclude
    private FieldDefinition triggerField;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DependencyAction action;

    @Column(name = "trigger_value")
    private String triggerValue;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FieldDependency that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}