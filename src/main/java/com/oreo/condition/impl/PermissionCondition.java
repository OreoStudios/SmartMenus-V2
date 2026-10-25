package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PermissionCondition implements Condition {
    private final SmartMenus plugin;
    private final String permission;

    public PermissionCondition(SmartMenus plugin, String permission) {
        this.plugin = plugin;
        this.permission = permission;
    }

    @Override
    public boolean check(Player player) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean take(Player player) {
        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("permission", permission);

        return plugin.getMessageManager().getMessage("conditions.permission.missing", player, replacements);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.PERMISSION;
    }
}
