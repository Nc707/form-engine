package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.LayoutResolutionService;
import com.nc.formengine.dataimpl.entity.FormLayout;
import com.nc.formengine.dataimpl.mapper.FormLayoutMapper;
import com.nc.formengine.dataimpl.repository.FormLayoutRepository;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class LayoutResolutionServiceImpl implements LayoutResolutionService {

    private final FormLayoutRepository formLayoutRepository;
    private final FormLayoutMapper formLayoutMapper;

    public LayoutResolutionServiceImpl(FormLayoutRepository formLayoutRepository,
                                       FormLayoutMapper formLayoutMapper) {
        this.formLayoutRepository = formLayoutRepository;
        this.formLayoutMapper = formLayoutMapper;
    }

    @Override
    public Optional<FormLayoutDTO> resolveLayout(Long formDefinitionId, DeviceType deviceType) {
        Optional<FormLayout> exactMatch = formLayoutRepository
                .findByFormDefinitionIdAndDeviceType(formDefinitionId, deviceType);

        if (exactMatch.isPresent()) {
            log.debug("Found exact layout match for form {} and device {}", formDefinitionId, deviceType);
            return exactMatch.map(formLayoutMapper::toDTO);
        }

        Optional<FormLayout> fallback = formLayoutRepository
                .findByFormDefinitionIdAndDeviceTypeIsNull(formDefinitionId);

        if (fallback.isPresent()) {
            log.debug("Using fallback layout for form {} (no device type)", formDefinitionId);
            return fallback.map(formLayoutMapper::toDTO);
        }

        if (deviceType != null) {
            Optional<FormLayout> closestMatch = findClosestLayout(formDefinitionId, deviceType);
            if (closestMatch.isPresent()) {
                log.debug("Using closest layout match for form {} and device {}", formDefinitionId, deviceType);
                return closestMatch.map(formLayoutMapper::toDTO);
            }
        }

        log.warn("No layout found for form {} and device {}", formDefinitionId, deviceType);
        return Optional.empty();
    }

    /**
     * The layout of the other device, when the requested one has none of its own.
     *
     * <p>Laying a form out for the wrong screen still beats not laying it out at all: the caller's
     * alternative is no layout, and a field the layout does not place is one the user cannot answer.
     * With two device types the relation is simply the other one, in both directions.
     */
    private Optional<FormLayout> findClosestLayout(Long formDefinitionId, DeviceType requestedType) {
        DeviceType other = requestedType == DeviceType.MOBILE ? DeviceType.DESKTOP : DeviceType.MOBILE;
        return formLayoutRepository.findByFormDefinitionIdAndDeviceType(formDefinitionId, other);
    }
}
