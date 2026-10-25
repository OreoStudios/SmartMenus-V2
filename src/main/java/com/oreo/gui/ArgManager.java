package com.oreo.gui;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArgManager {

    private static final Map<UUID, Map<String, String>> args = new ConcurrentHashMap<>();

    private ArgManager() {}

    public static void store(UUID id, Map<String, String> playerArgs) {
        args.put(id, playerArgs);
    }

    public static String get(UUID id, String key) {
        Map<String, String> playerArgs = args.get(id);
        if (playerArgs == null) return null;
        return playerArgs.get(key);
    }

    public static Map<String, String> getAll(UUID id) {
        Map<String, String> playerArgs = args.get(id);
        return playerArgs != null ? Collections.unmodifiableMap(playerArgs) : Collections.emptyMap();
    }

    public static void clear(UUID id) {
        args.remove(id);
    }
}
