package com.nc.formengine.flow.shared;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/** The two things a view ever needs to tell the user, phrased the same way everywhere. */
public final class Notifications {

    private static final int DURATION_MS = 3000;
    private static final int ERROR_DURATION_MS = 5000;

    private Notifications() {
    }

    public static void success(String message) {
        show(message, DURATION_MS, NotificationVariant.LUMO_SUCCESS);
    }

    /** Errors stay up longer: they usually say something the user has to act on. */
    public static void error(String message) {
        show(message, ERROR_DURATION_MS, NotificationVariant.LUMO_ERROR);
    }

    private static void show(String message, int durationMs, NotificationVariant variant) {
        Notification.show(message, durationMs, Notification.Position.BOTTOM_END)
                .addThemeVariants(variant);
    }
}
