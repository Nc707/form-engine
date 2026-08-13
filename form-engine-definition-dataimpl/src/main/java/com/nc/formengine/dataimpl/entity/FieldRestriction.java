package com.nc.formengine.dataimpl.entity;

import com.nc.formengine.model.enums.RestrictionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One validation rule applied to a field.
 *
 * <p>Restrictions used to be five nullable columns on {@link FieldDefinition}, one per rule the
 * engine happened to support. That shape could not store a rule taking no parameter, such as
 * {@code EMAIL}, lost the author's custom message and the evaluation order, and needed a schema change
 * for every new kind of rule. Here the kind of rule is data, so every {@link RestrictionType} value is
 * already storable and the next one will be too.
 */
@Entity
@Table(name = "field_restrictions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_definition_id", nullable = false)
    @ToString.Exclude
    private FieldDefinition fieldDefinition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestrictionType type;

    /** The message the form author wrote, or null to use the default of the specification. */
    @Column(name = "error_message")
    private String errorMessage;

    /** Position among the restrictions of the field, which is the order they are evaluated in. */
    @Column(name = "order_index")
    private Integer orderIndex;

    /**
     * Configuration of the rule, such as {@code minLength -> 5}, empty for the rules that take none.
     *
     * <p>Values are stored as JSON text rather than as their {@code toString}, so a number survives
     * the round trip as a number. {@code FieldRestrictionMapper} is where that happens.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "field_restriction_parameters",
        joinColumns = @JoinColumn(name = "field_restriction_id"))
    @MapKeyColumn(name = "parameter_key")
    @Column(name = "parameter_value", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, String> parameters = new HashMap<>();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FieldRestriction that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
