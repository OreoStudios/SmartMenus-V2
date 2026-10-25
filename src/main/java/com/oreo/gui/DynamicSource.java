package com.oreo.gui;

public enum DynamicSource {

    NONE,

    ONLINE_PLAYERS,

    WORLDS,

    PLAYER_INVENTORY,

    PLAYER_ENDERCHEST,

    PERMISSION_GROUPS;

    public static DynamicSource fromString(String s) {
        if (s == null || s.isEmpty()) return NONE;
        try {
            return valueOf(s.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
