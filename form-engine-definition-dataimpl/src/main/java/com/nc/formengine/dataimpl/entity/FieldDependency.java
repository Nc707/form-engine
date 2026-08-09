package com.nc.formengine.dataimpl.entity;

import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;
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

    /**
     * The comparison applied to the trigger field's value.
     * <p>
     * Mapped to {@code condition_type} because {@code condition} is a reserved word in SQL:2016 and
     * in MySQL; H2 accepts it today, but the default physical naming would emit a bare
     * {@code condition} column and make the schema unportable.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    private DependencyCondition condition;

    /** What is applied to the dependent field while the condition holds. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DependencyEffect effect;

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