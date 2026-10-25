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
        }
        if (section.isSet("minutes")) {
            millis += Math.round(section.getDouble("minutes") * 60_000.0);
        }
        if (section.isSet("seconds")) {
            millis += Math.round(section.getDouble("seconds") * 1000.0);
        }
        if (millis <= 0L) return null;

        String id = section.getString("id", defaultId);
        String bypass = section.getString("bypass-permission", section.getString("bypass_permission", null));

        String message = resolveText(section, "message",
                "&cYou must wait &e%time_left% &cbefore doing this again.");
        String title = resolveText(section, "title", "&c&lWAIT");
        String subtitle = resolveText(section, "subtitle", "&e%time_left% &7remaining");

        int fadeIn = section.getInt("fade-in", section.getInt("fade_in", 10));
        int stay = section.getInt("stay", 40);
        int fadeOut = section.getInt("fade-out", section.getInt("fade_out", 10));
        boolean actionBar = section.getBoolean("action-bar", section.getBoolean("action_bar", false));
        String sound = section.getString("sound", null);

        return new CooldownConfig(id, millis, bypass, message, title, subtitle,
                fadeIn, stay, fadeOut, actionBar, sound);
    }

    private static String resolveText(ConfigurationSection section, String key, String def) {
        if (!section.isSet(key)) return def;
        String value = section.getString(key, "");
        return value == null ? "" : value;
    }

    public static long parseDuration(String raw) {
        if (raw == null) return 0L;
        String trimmed = raw.trim().toLowerCase(Locale.ENGLISH);
        if (trimmed.isEmpty()) return 0L;

        if (trimmed.matches("\\d+")) {
            try {
                return Long.parseLong(trimmed) * 1000L;
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        long total = 0L;
        Matcher matcher = DURATION_PATTERN.matcher(trimmed);
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            switch (matcher.group(2).toLowerCase(Locale.ENGLISH)) {
                case "d": total += value * 86_400_000L; break;
                case "h": total += value * 3_600_000L; break;
                case "m": total += value * 60_000L; break;
                case "s": total += value * 1000L; break;
                default: break;
            }
        }
        return total;
    }
}
