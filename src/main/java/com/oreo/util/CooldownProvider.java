package com.oreo.util;

import java.util.UUID;

public interface CooldownProvider {
    /** Returns millis remaining, or 0 if not on cooldown. */
    long remainingMillis(String id, UUID uuid);
    /** Applies a cooldown for {@code durationMillis} milliseconds. */
    void apply(String id, UUID uuid, long durationMillis);
    /** Removes any active cooldown. */
    void clear(String id, UUID uuid);
    /** Called on plugin disable — flush/close resources. */
    void shutdown();
}
