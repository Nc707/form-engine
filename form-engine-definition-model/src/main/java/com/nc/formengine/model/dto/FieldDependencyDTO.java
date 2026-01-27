package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.DependencyAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDependencyDTO {

    private Long id;
    private Long dependentFieldId;
    private Long triggerFieldId;
    private DependencyAction action;
    private String triggerValue;
}
