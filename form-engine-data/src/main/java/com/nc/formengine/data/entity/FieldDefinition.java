package com.nc.formengine.data.entity;

import com.nc.formengine.data.enums.FieldType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "field_definitions")
@Data
public class FieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id", nullable = false)
    @ToString.Exclude // Evita ciclos infinitos en logs
    private FormDefinition formDefinition;

    // Identificador del campo para el JSON de respuesta (ej: "user_age")
    @Column(nullable = false)
    private String name; 

    // Texto visible para el humano (ej: "¿Cuál es tu edad?")
    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType type;

    @Column(name = "order_index")
    private Integer orderIndex;

    // --- Validaciones Básicas (Metadatos Estructurados) ---
    private Boolean required = false;
    private Integer minLength;
    private Integer maxLength;
    private Double minValue; // Double para soportar decimales
    private Double maxValue;
    private String regexPattern;
    
    // --- Relaciones de Dependencia ---
    // Campos que dependen de ESTE campo (Yo soy el trigger)
    @OneToMany(mappedBy = "triggerField", cascade = CascadeType.ALL)
    private List<FieldDependency> triggeredDependencies;

    // Campos de los que YO dependo (Yo soy el dependiente)
    @OneToMany(mappedBy = "dependentField", cascade = CascadeType.ALL)
    private List<FieldDependency> myDependencies;
}