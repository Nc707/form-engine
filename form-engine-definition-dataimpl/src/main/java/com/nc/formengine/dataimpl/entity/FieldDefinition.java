package com.nc.formengine.dataimpl.entity;

import com.nc.formengine.data.enums.FieldType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "field_definitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id", nullable = false)
    @ToString.Exclude
    private FormDefinition formDefinition;

    @Column(nullable = false)
    private String name; 

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType type;

    @Column(name = "order_index")
    private Integer orderIndex;

    private Boolean required = false;
    private Integer minLength;
    private Integer maxLength;
    private Double minValue;
    private Double maxValue;
    private String regexPattern;
    
    @OneToMany(mappedBy = "triggerField", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FieldDependency> triggeredDependencies;

    @OneToMany(mappedBy = "dependentField", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FieldDependency> myDependencies;
}