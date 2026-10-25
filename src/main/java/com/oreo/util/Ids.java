package com.oreo.util;

import java.util.Locale;

/**
 * Helpers for turning arbitrary labels/file names into stable GUI identifiers.
 */
public final class Ids {

    private Ids() {
    }

    /**
     * Normalizes a raw string into a canonical id: lower-cased (locale-independent) with every
     * character outside {@code [a-z0-9_]} replaced by an underscore.
     *
     * <p>Uses {@link Locale#ROOT} so the result never depends on the server's default locale
     * (e.g. the Turkish {@code i}/{@code ı} mapping).
     *
     * @param raw the input, may be {@code null}
     * @return the slug, or an empty string when {@code raw} is {@code null}
     */
    public static String slugify(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }
}
