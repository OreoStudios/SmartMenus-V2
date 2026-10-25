package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.entity.Player;

public class ScriptCondition implements Condition {

    private final SmartMenus plugin;
    private final String script;

    public ScriptCondition(SmartMenus plugin, String script) {
        this.plugin = plugin;
        this.script = script;
    }

    @Override
    public boolean check(Player player) {
        return plugin.getScriptEngine().evalCondition(script, player);
    }

    @Override
    public boolean take(Player player) {
        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        return com.oreo.util.ColorUtil.color("&cScript condition not met.");
    }

    @Override
    public ConditionType getType() {
        return ConditionType.SCRIPT;
    }
}
