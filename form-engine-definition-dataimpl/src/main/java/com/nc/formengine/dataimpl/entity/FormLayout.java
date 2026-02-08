package com.nc.formengine.dataimpl.entity;

import com.nc.formengine.model.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_layouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id", nullable = false)
    private FormDefinition formDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType;

    @Column(name = "custom_device_name")
    private String customDeviceName;

    @OneToMany(mappedBy = "formLayout", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FieldLayout> fieldLayouts = new ArrayList<>();
}
