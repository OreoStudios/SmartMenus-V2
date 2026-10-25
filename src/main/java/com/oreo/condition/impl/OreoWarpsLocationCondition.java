package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import fr.elias.oreoEssentials.OreoEssentials;
import fr.elias.oreoEssentials.modules.warps.WarpService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class OreoWarpsLocationCondition implements Condition {
    private final SmartMenus plugin;
    private final String warpName;
    private final double radius;
    private final String customErrorMessage;

    public OreoWarpsLocationCondition(SmartMenus plugin, String warpName) {
        this(plugin, warpName, 5.0, null);
    }

    public OreoWarpsLocationCondition(SmartMenus plugin, String warpName, double radius) {
        this(plugin, warpName, radius, null);
    }

    public OreoWarpsLocationCondition(SmartMenus plugin, String warpName, double radius, String customErrorMessage) {
        this.plugin = plugin;
        this.warpName = warpName;
        this.radius = radius;
        this.customErrorMessage = customErrorMessage;
    }

    @Override
    public boolean check(Player player) {
        WarpService service = getWarpService();
        if (service == null) return false;

        try {
            Location warpLocation = service.getWarp(warpName);
            if (warpLocation == null) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("warp", warpName);
                plugin.getLogger().warning(plugin.getMessageManager().getMessage("conditions.oreo_warps_location.warp_not_found", replacements));
                return false;
            }

            Location playerLocation = player.getLocation();

            if (!warpLocation.getWorld().equals(playerLocation.getWorld())) {
                return false;
            }

            double distance = getHorizontalDistance(playerLocation, warpLocation);

            return distance <= radius;

        } catch (Exception e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.oreo_warps_check_failed", replacements));
            return false;
        }
    }

    @Override
    public boolean take(Player player) {
        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        WarpService service = getWarpService();

        if (service == null) {
            return plugin.getMessageManager().getMessage("conditions.oreo_warps_location.unavailable", player);
        }

        try {
            Location warpLocation = service.getWarp(warpName);

            if (warpLocation == null) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("warp", warpName);
                return plugin.getMessageManager().getMessage("conditions.oreo_warps_location.warp_not_found", player, replacements);
            }

            Location playerLocation = player.getLocation();

            if (!warpLocation.getWorld().equals(playerLocation.getWorld())) {
