package com.oreo.gui;

import java.util.Locale;
import java.util.logging.Logger;

public enum BottomInventoryMode {
    DEFAULT,
    PACKET_EVENT;

    public static BottomInventoryMode parse(String value, String guiId, Logger logger) {
        if (value == null || value.isBlank()) return DEFAULT;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            logger.warning("[SmartMenus] Unknown bottom_inventory_mode '" + value
                    + "' in GUI '" + guiId + "', falling back to DEFAULT.");
            return DEFAULT;
        }
    }
}
