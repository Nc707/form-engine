package com.nc.formengine.model.dto;

import jakarta.validation.constraints.NotBlank;
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
public class FormDefinitionDTO {

    private Long id;

    @NotBlank(message = "code is required")
    @Size(max = 100, message = "code must be at most 100 characters")
    private String code;

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    @NotNull(message = "version is required")
    @Positive(message = "version must be greater than 0")
    private Integer version;

    // Nested collections are intentionally not cascaded with @Valid: they are populated
    // server-side on read, and a create body legitimately omits the back-references.

    @Builder.Default
    private List<FieldDefinitionDTO> fields = new ArrayList<>();
    
    @Builder.Default
    private List<FormLayoutDTO> layouts = new ArrayList<>();
}
