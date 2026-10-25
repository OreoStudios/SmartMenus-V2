package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import fr.elias.npcs.api.ModeledNPCsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ModeledNPCCondition implements Condition {
    private final SmartMenus plugin;
    private final int npcId;
    private final double radius;

    public ModeledNPCCondition(SmartMenus plugin, int npcId) {
        this(plugin, npcId, 5.0);
    }

    public ModeledNPCCondition(SmartMenus plugin, int npcId, double radius) {
        this.plugin = plugin;
        this.npcId = npcId;
        this.radius = radius;
    }

    @Override
    public boolean check(Player player) {
        if (!isAvailable()) {
            return false;
        }

        try {
            ModeledNPCsAPI api = ModeledNPCsAPI.get();

            if (!api.getAllNPCIds().contains(npcId)) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("id", String.valueOf(npcId));
                plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.npc_not_exist", replacements));
                return false;
            }

            Location npcLocation = api.getNPCLocation(npcId);
            if (npcLocation == null) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("id", String.valueOf(npcId));
                plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.npc_location_unavailable", replacements));
                return false;
            }

            Location playerLocation = player.getLocation();

            if (!npcLocation.getWorld().equals(playerLocation.getWorld())) {
                return false;
            }

            double distance = playerLocation.distance(npcLocation);
            return distance <= radius;

        } catch (Exception e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.npc_proximity_check_failed", replacements));
            return false;
        }
    }

    @Override
    public boolean take(Player player) {
        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        if (!isAvailable()) {
            return plugin.getMessageManager().getMessage("conditions.modeled_npc.unavailable", player);
        }

        try {
            ModeledNPCsAPI api = ModeledNPCsAPI.get();

            if (!api.getAllNPCIds().contains(npcId)) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("id", String.valueOf(npcId));
                return plugin.getMessageManager().getMessage("conditions.modeled_npc.npc_not_found", player, replacements);
            }

            String npcName = api.getNPCDisplayName(npcId);
            if (npcName == null) {
                npcName = "NPC #" + npcId;
            }

            Location npcLocation = api.getNPCLocation(npcId);
            if (npcLocation == null) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("npc", npcName);
                return plugin.getMessageManager().getMessage("conditions.modeled_npc.location_unavailable", player, replacements);
            }

            Location playerLocation = player.getLocation();

            if (!npcLocation.getWorld().equals(playerLocation.getWorld())) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("world", npcLocation.getWorld().getName());
                return plugin.getMessageManager().getMessage("conditions.modeled_npc.wrong_world", player, replacements);
            }

            double distance = playerLocation.distance(npcLocation);

            Map<String, String> replacements = new HashMap<>();
            replacements.put("npc", npcName);
            replacements.put("distance", String.format("%.1f", distance));
            replacements.put("radius", String.format("%.1f", radius));

            return plugin.getMessageManager().getMessage("conditions.modeled_npc.too_far", player, replacements);

        } catch (Exception e) {
            return plugin.getMessageManager().getMessage("conditions.modeled_npc.unavailable", player);
        }
    }

    @Override
    public ConditionType getType() {
        return ConditionType.MODELED_NPC;
    }

    private boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("ModeledNPCs") != null
                && ModeledNPCsAPI.get() != null;
    }

    public int getNpcId() {
        return npcId;
    }

    public double getRadius() {
        return radius;
    }
}
