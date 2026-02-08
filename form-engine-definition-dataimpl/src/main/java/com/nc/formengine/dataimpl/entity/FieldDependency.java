package com.nc.formengine.dataimpl.entity;

import com.nc.formengine.data.enums.DependencyAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "field_dependencies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dependent_field_id", nullable = false)
    private FieldDefinition dependentField;

    @ManyToOne
    @JoinColumn(name = "trigger_field_id", nullable = false)
    private FieldDefinition triggerField;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DependencyAction action;

    @Column(name = "trigger_value")
    private String triggerValue;
}