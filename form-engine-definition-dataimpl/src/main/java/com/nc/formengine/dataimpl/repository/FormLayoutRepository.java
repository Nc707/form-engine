package com.nc.formengine.dataimpl.repository;

import com.nc.formengine.dataimpl.entity.FormLayout;
import com.nc.formengine.model.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormLayoutRepository extends JpaRepository<FormLayout, Long> {
    
    List<FormLayout> findByFormDefinitionId(Long formDefinitionId);
    
    Optional<FormLayout> findByFormDefinitionIdAndDeviceType(Long formDefinitionId, DeviceType deviceType);
    
    Optional<FormLayout> findByFormDefinitionIdAndDeviceTypeIsNull(Long formDefinitionId);
}
