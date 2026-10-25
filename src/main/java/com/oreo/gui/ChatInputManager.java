package com.oreo.gui;

import com.oreo.action.Action;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInputManager {

    public record PendingInput(
            String guiId,
            String promptMessage,
            String inputType,
            String cancelWord,
            List<Action> onComplete,
            Map<String, String> baseVars
    ) {}

    private static final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    private ChatInputManager() {}

    public static void await(UUID id, PendingInput input) {
        pending.put(id, input);
    }

    public static PendingInput get(UUID id) {
        return pending.get(id);
    }

    public static void remove(UUID id) {
        pending.remove(id);
    }

    public static boolean isAwaiting(UUID id) {
        return pending.containsKey(id);
    }
}
