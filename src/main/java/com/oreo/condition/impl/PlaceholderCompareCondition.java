package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderCompareCondition implements Condition {

    public enum CompareMode {
        EQUALS, CONTAINS, GREATER_THAN, LESS_THAN
    }

    private final SmartMenus plugin;
    private final String placeholder;
    private final String value;
    private final CompareMode mode;
    private final boolean ignoreCase;

    public PlaceholderCompareCondition(SmartMenus plugin, String placeholder,
                                       String value, CompareMode mode, boolean ignoreCase) {
        this.plugin = plugin;
        this.placeholder = placeholder;
        this.value = value;
        this.mode = mode;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public boolean check(Player player) {
        String resolved = resolvePlaceholder(player, placeholder);
        if (resolved == null) return false;

        switch (mode) {
            case EQUALS:
                return ignoreCase
                        ? resolved.equalsIgnoreCase(value)
                        : resolved.equals(value);

            case CONTAINS:
                return ignoreCase
                        ? resolved.toLowerCase().contains(value.toLowerCase())
                        : resolved.contains(value);

            case GREATER_THAN:
                try {
                    return Double.parseDouble(resolved) > Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return false;
                }

            case LESS_THAN:
                try {
                    return Double.parseDouble(resolved) < Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return false;
                }

            default:
                return false;
        }
    }

    @Override
    public boolean take(Player player) {
        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        return com.oreo.util.ColorUtil.color(
                "&cCondition not met: " + placeholder + " " + mode.name().toLowerCase() + " " + value);
    }

    @Override
    public ConditionType getType() {
        switch (mode) {
            case EQUALS:       return ConditionType.PLACEHOLDER_EQUALS;
            case CONTAINS:     return ConditionType.PLACEHOLDER_CONTAINS;
            case GREATER_THAN: return ConditionType.PLACEHOLDER_GREATER_THAN;
            case LESS_THAN:    return ConditionType.PLACEHOLDER_LESS_THAN;
            default:           return ConditionType.PLACEHOLDER_EQUALS;
        }
    }

    private String resolvePlaceholder(Player player, String ph) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return ph;
        }
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) papiClass.getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(null, player, ph);
        } catch (Exception e) {
            return ph;
        }
    }
}
