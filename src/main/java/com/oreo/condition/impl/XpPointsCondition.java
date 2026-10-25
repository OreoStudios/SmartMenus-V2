package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class XpPointsCondition implements Condition {
    private final SmartMenus plugin;
    private final int points;

    public XpPointsCondition(SmartMenus plugin, int points) {
        this.plugin = plugin;
        this.points = points;
    }

    @Override
    public boolean check(Player player) {
        return player.getTotalExperience() >= points;
    }

    @Override
    public boolean take(Player player) {
        if (check(player)) {
            player.setTotalExperience(player.getTotalExperience() - points);
            updateExperience(player);
            return true;
        }
        return false;
    }

    @Override
    public String getErrorMessage(Player player) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("amount", String.valueOf(points));
        replacements.put("current", String.valueOf(player.getTotalExperience()));
        return plugin.getMessageManager().getMessage("conditions.xp_points.insufficient", player, replacements);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.XP_POINTS;
    }

    private void updateExperience(Player player) {
        int totalExp = player.getTotalExperience();
        int level = 0;
        int expForLevel = 0;

        while (expForLevel <= totalExp) {
            int expToNext = getExpToNextLevel(level);
            if (expForLevel + expToNext > totalExp) break;
            expForLevel += expToNext;
            level++;
        }

        player.setLevel(level);
        player.setExp((float) (totalExp - expForLevel) / getExpToNextLevel(level));
    }

    private int getExpToNextLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        else if (level <= 30) return 5 * level - 38;
        else return 9 * level - 158;
    }
}
