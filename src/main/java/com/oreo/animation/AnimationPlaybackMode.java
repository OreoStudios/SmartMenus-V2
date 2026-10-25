package com.oreo.animation;

import java.util.Locale;

/**
 * How an animation walks through its frames.
 *
 * <ul>
 *     <li>{@link #LOOP} — {@code 0,1,…,n-1,0,1,…} forever (or until the configured loop count is reached).</li>
 *     <li>{@link #ONCE} — {@code 0,1,…,n-1} then stops on the last frame.</li>
 *     <li>{@link #REVERSE} — {@code n-1,…,1,0,n-1,…}, i.e. a loop that runs backwards.</li>
 *     <li>{@link #PING_PONG} — {@code 0,1,…,n-1,n-2,…,1,0,1,…} bouncing between the two ends.</li>
 * </ul>
 */
public enum AnimationPlaybackMode {

    LOOP,
    ONCE,
    REVERSE,
    PING_PONG;

    /**
     * Parses a mode name leniently. Accepts case-insensitive names and both {@code PING_PONG}
     * and {@code PING-PONG}. Unknown or blank values fall back to {@link #LOOP}.
     *
     * @param value the raw config value
     * @return the matching mode, or {@link #LOOP} if none matches
     */
    public static AnimationPlaybackMode fromString(String value) {
        if (value == null || value.trim().isEmpty()) return LOOP;
        String normalized = value.trim().toUpperCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return LOOP;
        }
    }
}
