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
                Map<String, String> replacements = new HashMap<>();
                replacements.put("world", warpLocation.getWorld().getName());
                replacements.put("current", playerLocation.getWorld().getName());
                return plugin.getMessageManager().getMessage("conditions.oreo_warps_location.wrong_world", player, replacements);
            }

            double distance = getHorizontalDistance(playerLocation, warpLocation);

            Map<String, String> replacements = new HashMap<>();
            replacements.put("warp", warpName);
            replacements.put("distance", String.format("%.1f", distance));
            replacements.put("radius", String.format("%.1f", radius));
            replacements.put("player", player.getName());

            if (customErrorMessage != null && !customErrorMessage.isEmpty()) {
                String message = customErrorMessage;
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    message = message.replace("{" + entry.getKey() + "}", entry.getValue());
                }

                if (getWarpService() != null && plugin.getMessageManager().isPlaceholderAPIAvailable()) {
                    try {
                        Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                        message = (String) papiClass.getMethod("setPlaceholders", Player.class, String.class)
                                .invoke(null, player, message);
                    } catch (Exception ignored) {
                        // PlaceholderAPI missing or a placeholder threw; keep the raw message.
                    }
                }

                return com.oreo.util.ColorUtil.color(message);
            }

            return plugin.getMessageManager().getMessage("conditions.oreo_warps_location.too_far", player, replacements);

        } catch (Exception e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("warp", warpName);
            return plugin.getMessageManager().getMessage("conditions.oreo_warps_location.warp_not_found", player, replacements);
        }
    }

    @Override
    public ConditionType getType() {
        return ConditionType.OREO_WARPS_LOCATION;
    }

    private WarpService getWarpService() {
        try {
            OreoEssentials oreo = (OreoEssentials) Bukkit.getPluginManager().getPlugin("OreoEssentials");
            return oreo != null && oreo.isEnabled() ? oreo.getWarpService() : null;
        } catch (Exception e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.oreo_warps_service_failed", replacements));
            return null;
        }
    }

    private double getHorizontalDistance(Location loc1, Location loc2) {
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
