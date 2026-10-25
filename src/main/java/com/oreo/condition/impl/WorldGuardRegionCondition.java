package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class WorldGuardRegionCondition implements Condition {
    private final SmartMenus plugin;
    private final String regionId;
    private final boolean requireMember;

    public WorldGuardRegionCondition(SmartMenus plugin, String regionId, boolean requireMember) {
        this.plugin = plugin;
        this.regionId = regionId;
        this.requireMember = requireMember;
    }

    @Override
    public boolean check(Player player) {
        if (!isAvailable()) return false;

        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuard);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);

            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object location = bukkitAdapterClass.getMethod("adapt", org.bukkit.Location.class).invoke(null, player.getLocation());

            Object regionSet = query.getClass().getMethod("getApplicableRegions",
                    Class.forName("com.sk89q.worldedit.util.Location")).invoke(query, location);

            Iterable<?> regions = (Iterable<?>) regionSet;

            for (Object region : regions) {
                String id = (String) region.getClass().getMethod("getId").invoke(region);
                if (id.equalsIgnoreCase(regionId)) {
                    if (requireMember) {
                        Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
                        Object wgPlugin = wgPluginClass.getMethod("inst").invoke(null);
                        Object localPlayer = wgPluginClass.getMethod("wrapPlayer", Player.class).invoke(wgPlugin, player);

                        boolean isMember = (boolean) region.getClass().getMethod("isMember",
                                Class.forName("com.sk89q.worldguard.domains.Association")).invoke(region, localPlayer);
                        boolean isOwner = (boolean) region.getClass().getMethod("isOwner",
                                Class.forName("com.sk89q.worldguard.domains.Association")).invoke(region, localPlayer);

                        return isMember || isOwner;
                    }
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.worldguard_check_failed", replacements));
            return false;
        }
    }

    @Override
    public boolean take(Player player) {
        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("region", regionId);

        if (requireMember) {
            return plugin.getMessageManager().getMessage("conditions.worldguard_region.not_member", player, replacements);
        } else {
            return plugin.getMessageManager().getMessage("conditions.worldguard_region.not_in_region", player, replacements);
        }
    }

    @Override
    public ConditionType getType() {
        return ConditionType.WORLDGUARD_REGION;
    }

    private boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }
}
