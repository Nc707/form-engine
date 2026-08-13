package com.nc.formengine.business.service;

import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;

import java.util.Optional;

/**
 * Which stored layout a given device should be rendered on.
 *
 * <p>Answering "none" is a real answer: a form with no layout is stacked in field order, which is what
 * the renderer does anyway, so there is nothing to synthesise here.
 */
public interface LayoutResolutionService {

    Optional<FormLayoutDTO> resolveLayout(Long formDefinitionId, DeviceType deviceType);
}
