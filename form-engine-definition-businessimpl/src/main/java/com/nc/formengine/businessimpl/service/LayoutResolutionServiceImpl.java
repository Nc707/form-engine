package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.LayoutResolutionService;
import com.nc.formengine.data.dao.FormLayoutDao;
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

    private final FormLayoutDao formLayoutDao;

    public LayoutResolutionServiceImpl(FormLayoutDao formLayoutDao) {
        this.formLayoutDao = formLayoutDao;
    }

    @Override
    public Optional<FormLayoutDTO> resolveLayout(Long formDefinitionId, DeviceType deviceType) {
        Optional<FormLayoutDTO> exactMatch = formLayoutDao
                .findByFormDefinitionIdAndDeviceType(formDefinitionId, deviceType);

        if (exactMatch.isPresent()) {
            log.debug("Found exact layout match for form {} and device {}", formDefinitionId, deviceType);
            return exactMatch;
        }

        Optional<FormLayoutDTO> fallback = formLayoutDao
                .findByFormDefinitionIdAndDeviceType(formDefinitionId, null);

        if (fallback.isPresent()) {
            log.debug("Using fallback layout for form {} (no device type)", formDefinitionId);
            return fallback;
        }

        if (deviceType != null) {
            Optional<FormLayoutDTO> closestMatch = findClosestLayout(formDefinitionId, deviceType);
            if (closestMatch.isPresent()) {
                log.debug("Using closest layout match for form {} and device {}", formDefinitionId, deviceType);
                return closestMatch;
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
    private Optional<FormLayoutDTO> findClosestLayout(Long formDefinitionId, DeviceType requestedType) {
        DeviceType other = requestedType == DeviceType.MOBILE ? DeviceType.DESKTOP : DeviceType.MOBILE;
        return formLayoutDao.findByFormDefinitionIdAndDeviceType(formDefinitionId, other);
    }
}
