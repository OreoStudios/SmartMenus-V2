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

public class OreoWarpsCondition implements Condition {
    private final SmartMenus plugin;
    private final String warpName;
    private final boolean checkExists;
    private final boolean checkPermission;

    public OreoWarpsCondition(SmartMenus plugin, String warpName) {
        this(plugin, warpName, true, false);
    }

    public OreoWarpsCondition(SmartMenus plugin, String warpName, boolean checkExists, boolean checkPermission) {
        this.plugin = plugin;
        this.warpName = warpName;
        this.checkExists = checkExists;
        this.checkPermission = checkPermission;
    }

    @Override
    public boolean check(Player player) {
        WarpService service = getWarpService();
        if (service == null) return false;

        try {
            if (checkExists) {
                Location warpLocation = service.getWarp(warpName);
                if (warpLocation == null) {
                    return false;
                }
            }

            if (checkPermission) {
                if (!service.canUse(player, warpName)) {
                    return false;
                }
            }

            return true;
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
            return plugin.getMessageManager().getMessage("conditions.oreo_warps.unavailable", player);
        }

        try {
            Location warpLocation = service.getWarp(warpName);

            if (warpLocation == null) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("warp", warpName);
                return plugin.getMessageManager().getMessage("conditions.oreo_warps.warp_not_found", player, replacements);
            }

            if (checkPermission && !service.canUse(player, warpName)) {
                String requiredPerm = service.getWarpPermission(warpName);

                Map<String, String> replacements = new HashMap<>();
                replacements.put("warp", warpName);

                String message = plugin.getMessageManager().getMessage("conditions.oreo_warps.no_permission", player, replacements);

                if (requiredPerm != null) {
                    Map<String, String> permReplacements = new HashMap<>();
                    permReplacements.put("permission", requiredPerm);
                    message += plugin.getMessageManager().getMessage("conditions.oreo_warps.permission_required", player, permReplacements);
                }

                return message;
            }

            Map<String, String> replacements = new HashMap<>();
            replacements.put("warp", warpName);
            return plugin.getMessageManager().getMessage("conditions.oreo_warps.condition_not_met", player, replacements);
        } catch (Exception e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("warp", warpName);
            return plugin.getMessageManager().getMessage("conditions.oreo_warps.warp_not_found", player, replacements);
        }
    }

    @Override
    public ConditionType getType() {
        return ConditionType.OREO_WARPS;
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
}
