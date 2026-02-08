package com.nc.formengine.dataimpl.entity;

import com.nc.formengine.model.enums.ComponentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@Table(name = "field_layouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_layout_id", nullable = false)
    private FormLayout formLayout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_definition_id", nullable = false)
    private FieldDefinition fieldDefinition;

    @Column(name = "row_position")
    private Integer row;

    @Column(name = "column_position")
    private Integer column;

    private Integer colspan;

    private Integer rowspan;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type")
    private ComponentType componentType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "field_layout_properties", joinColumns = @JoinColumn(name = "field_layout_id"))
    @MapKeyColumn(name = "property_key")
    @Column(name = "property_value", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, String> customProperties = new HashMap<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean visible = true;
}
