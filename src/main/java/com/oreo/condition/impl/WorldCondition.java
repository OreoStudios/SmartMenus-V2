package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorldCondition implements Condition {
    private final SmartMenus plugin;
    private final List<String> allowedWorlds;
    private final boolean blacklist;

    public WorldCondition(SmartMenus plugin, List<String> worlds) {
        this(plugin, worlds, false);
    }

    public WorldCondition(SmartMenus plugin, List<String> worlds, boolean blacklist) {
        this.plugin = plugin;
        this.allowedWorlds = worlds;
        this.blacklist = blacklist;

        for (int i = 0; i < allowedWorlds.size(); i++) {
            allowedWorlds.set(i, allowedWorlds.get(i).toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public boolean check(Player player) {
        String currentWorld = player.getWorld().getName().toLowerCase(Locale.ROOT);
        boolean inList = allowedWorlds.contains(currentWorld);

        return blacklist ? !inList : inList;
    }

    @Override
    public boolean take(Player player) {

        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        String currentWorld = player.getWorld().getName();

        Map<String, String> replacements = new HashMap<>();
        replacements.put("world", currentWorld);
        replacements.put("current", currentWorld);
        replacements.put("worlds", String.join(", ", allowedWorlds));

        if (blacklist) {
            return plugin.getMessageManager().getMessage("conditions.world.blacklisted", player, replacements);
        } else {
            return plugin.getMessageManager().getMessage("conditions.world.wrong_world", player, replacements);
        }
    }

    @Override
    public ConditionType getType() {
        return ConditionType.WORLD;
    }
}
