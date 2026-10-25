package com.oreo.gui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PageManager {

    private static final Map<UUID, Map<String, Integer>> pages = new ConcurrentHashMap<>();

    private PageManager() {}

    public static int getPage(UUID playerId, String guiId) {
        Map<String, Integer> guiPages = pages.get(playerId);
        if (guiPages == null) return 0;
        return guiPages.getOrDefault(guiId, 0);
    }

    public static void setPage(UUID playerId, String guiId, int page) {
        pages.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
             .put(guiId, Math.max(0, page));
    }

    public static void resetPage(UUID playerId, String guiId) {
        Map<String, Integer> guiPages = pages.get(playerId);
        if (guiPages != null) guiPages.remove(guiId);
    }

    public static void clearPlayer(UUID playerId) {
        pages.remove(playerId);
    }
}
