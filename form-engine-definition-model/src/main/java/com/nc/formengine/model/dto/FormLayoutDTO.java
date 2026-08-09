package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.DeviceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "formDefinitionId is required")
    @Positive(message = "formDefinitionId must be greater than 0")
    private Long formDefinitionId;

    // No @NotNull: a null deviceType is the generic layout every device falls back to.
    private DeviceType deviceType;

    @Size(max = 100, message = "customDeviceName must be at most 100 characters")
    private String customDeviceName;
    
    /** Cascaded: unlike a form's fields, these are always supplied by the caller. */
    @Valid
    @Builder.Default
    private List<FieldLayoutDTO> fieldLayouts = new ArrayList<>();
}
