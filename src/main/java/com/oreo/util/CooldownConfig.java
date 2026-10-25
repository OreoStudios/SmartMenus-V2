package com.oreo.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CooldownConfig {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)\\s*([smhd])", Pattern.CASE_INSENSITIVE);

    private final String id;
    private final long durationMillis;
    private final String bypassPermission;
    private final String message;
    private final String title;
    private final String subtitle;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;
    private final boolean actionBar;
    private final String sound;

    private CooldownConfig(String id, long durationMillis, String bypassPermission,
                           String message, String title, String subtitle,
                           int fadeIn, int stay, int fadeOut, boolean actionBar, String sound) {
        this.id = id;
        this.durationMillis = durationMillis;
        this.bypassPermission = bypassPermission;
        this.message = message;
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
        this.actionBar = actionBar;
        this.sound = sound;
    }

    public String getId() { return id; }
    public long getDurationMillis() { return durationMillis; }
    public String getBypassPermission() { return bypassPermission; }
    public String getMessage() { return message; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getFadeIn() { return fadeIn; }
    public int getStay() { return stay; }
    public int getFadeOut() { return fadeOut; }
    public boolean isActionBar() { return actionBar; }
    public String getSound() { return sound; }

    public boolean isEnabled() { return durationMillis > 0; }

    public static CooldownConfig parse(ConfigurationSection section, String defaultId) {
        if (section == null) return null;
        if (section.isSet("enabled") && !section.getBoolean("enabled", true)) return null;

        long millis = 0L;
        if (section.isSet("duration")) {
            millis += parseDuration(section.getString("duration", ""));
        }
        if (section.isSet("days")) {
            millis += Math.round(section.getDouble("days") * 86_400_000.0);
        }
        if (section.isSet("hours")) {
            millis += Math.round(section.getDouble("hours") * 3_600_000.0);
