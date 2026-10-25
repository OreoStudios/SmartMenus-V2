package com.oreo.gui;

public enum ButtonType {
    NONE,
    PAGINATION,
    DYNAMIC_PAGINATION,
    NEXT,
    PREVIOUS,
    HOME,
    JUMP,
    MAIN_MENU,
    SWITCH,
    INVENTORY,
    BACK,
    INPUT,
    CHAT_INPUT;

    public static ButtonType fromString(String s) {
        if (s == null || s.isEmpty()) return NONE;
        try {
            return valueOf(s.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
