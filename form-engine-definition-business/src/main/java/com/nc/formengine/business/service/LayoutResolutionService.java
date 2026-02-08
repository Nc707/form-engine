package com.nc.formengine.business.service;

import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;

import java.util.Optional;

public interface LayoutResolutionService {
    
    Optional<FormLayoutDTO> resolveLayout(Long formDefinitionId, DeviceType deviceType);
    
    FormLayoutDTO resolveLayoutOrDefault(Long formDefinitionId, DeviceType deviceType);
}
