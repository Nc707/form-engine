package com.nc.formengine.business.service;

import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;

import java.util.List;
import java.util.Optional;

public interface FormLayoutService {
    
    FormLayoutDTO createLayout(FormLayoutDTO layoutDTO);
    
    FormLayoutDTO updateLayout(Long layoutId, FormLayoutDTO layoutDTO);
    
    void deleteLayout(Long layoutId);
    
    Optional<FormLayoutDTO> getLayoutById(Long layoutId);
    
    List<FormLayoutDTO> getLayoutsByFormDefinition(Long formDefinitionId);
    
    Optional<FormLayoutDTO> getLayoutByFormAndDevice(Long formDefinitionId, DeviceType deviceType);
    
    void validateLayout(FormLayoutDTO layoutDTO);
}
