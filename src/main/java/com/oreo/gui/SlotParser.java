package com.oreo.gui;

import java.util.ArrayList;
import java.util.List;

public final class SlotParser {

    private SlotParser() {
    }

    public static List<Integer> parse(Object value) {
        List<Integer> slots = new ArrayList<>();
        if (value == null) return slots;

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return slots;

        for (String part : text.split(",")) {
            addToken(slots, part.trim());
        }
        return slots;
    }

    private static void addToken(List<Integer> slots, String token) {
        if (token.isEmpty()) return;

        if (token.matches("\\d+\\s*-\\s*\\d+")) {
            String[] bounds = token.split("-");
            int start = Integer.parseInt(bounds[0].trim());
            int end = Integer.parseInt(bounds[1].trim());
            if (start <= end) {
                for (int slot = start; slot <= end; slot++) {
                    slots.add(slot);
                }
            } else {
                for (int slot = start; slot >= end; slot--) {
                    slots.add(slot);
                }
            }
            return;
        }

        try {
            slots.add(Integer.parseInt(token));
        } catch (NumberFormatException ignored) {
        }
    }
}
