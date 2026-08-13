package com.nc.formengine.model.enums;

/**
 * The screen a layout was designed for.
 *
 * <p>Only the two the renderer can actually report. A third value nothing could ever ask for would be
 * a layout no form could be rendered on, and a fallback target that never exists.
 */
public enum DeviceType {
    MOBILE,
    DESKTOP
}
