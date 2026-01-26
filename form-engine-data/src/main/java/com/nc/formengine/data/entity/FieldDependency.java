package com.nc.formengine.data.entity;

import com.nc.formengine.data.enums.DependencyAction;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "field_dependencies")
@Data
public class FieldDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El campo que aparece/desaparece (El Hijo)
    @ManyToOne
    @JoinColumn(name = "dependent_field_id", nullable = false)
    private FieldDefinition dependentField;

    // El campo que controla la acción (El Padre)
    @ManyToOne
    @JoinColumn(name = "trigger_field_id", nullable = false)
    private FieldDefinition triggerField;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DependencyAction action;

    // El valor contra el que comparamos.
    // Se guarda como String, el Service hará el casting según el tipo del TriggerField.
    @Column(name = "trigger_value")
    private String triggerValue;
}