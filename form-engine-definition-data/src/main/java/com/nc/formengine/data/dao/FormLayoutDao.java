package com.nc.formengine.data.dao;

import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;

import java.util.List;
import java.util.Optional;

public interface FormLayoutDao {

    FormLayoutDTO save(FormLayoutDTO layoutDTO);

    Optional<FormLayoutDTO> findById(Long id);

    List<FormLayoutDTO> findByFormDefinitionId(Long formDefinitionId);

    /** A null {@code deviceType} names the generic layout every device falls back to. */
    Optional<FormLayoutDTO> findByFormDefinitionIdAndDeviceType(Long formDefinitionId, DeviceType deviceType);

    void deleteById(Long id);
}
