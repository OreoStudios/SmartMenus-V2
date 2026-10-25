package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WeatherCondition implements Condition {
    private final SmartMenus plugin;
    private final String weatherType;

    public WeatherCondition(SmartMenus plugin, String weatherType) {
        this.plugin = plugin;
        this.weatherType = weatherType.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean check(Player player) {
        World world = player.getWorld();

        switch (weatherType) {
            case "clear":
            case "sun":
            case "sunny":
                return !world.hasStorm();

            case "rain":
            case "raining":
                return world.hasStorm() && !world.isThundering();

            case "thunder":
            case "thundering":
            case "storm":
                return world.isThundering();

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
        String current = getCurrentWeather(player.getWorld());

        Map<String, String> replacements = new HashMap<>();
        replacements.put("required", weatherType);
        replacements.put("current", current);

        return plugin.getMessageManager().getMessage("conditions.weather.wrong_weather", player, replacements);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.WEATHER;
    }

    private String getCurrentWeather(World world) {
        if (world.isThundering()) {
            return "thunder";
        } else if (world.hasStorm()) {
            return "rain";
        } else {
            return "clear";
        }
    }
}
