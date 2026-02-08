package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormLayoutDTO {

    private Long id;
    private Long formDefinitionId;
    private DeviceType deviceType;
    private String customDeviceName;
    
    @Builder.Default
    private List<FieldLayoutDTO> fieldLayouts = new ArrayList<>();
}
