package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class ExpressionCondition implements Condition {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

    private final SmartMenus plugin;
    private final String expression;

    public ExpressionCondition(SmartMenus plugin, String expression) {
        this.plugin = plugin;
        this.expression = expression;
    }

    @Override
    public boolean check(Player player) {
        if (expression == null || expression.isEmpty()) return false;
        try {
            String resolved = resolvePlaceholders(player, expression);
            return evalBoolean(resolved);
        } catch (Exception e) {
            plugin.getLogger().warning("ExpressionCondition failed for expression '" + expression + "': " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean take(Player player) {
        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        return com.oreo.util.ColorUtil.color("&cCondition not met: " + expression);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.EXPRESSION;
    }

    private String resolvePlaceholders(Player player, String text) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) papiClass.getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(null, player, text);
        } catch (Exception e) {
            return text;
        }
    }

    private boolean evalBoolean(String expr) {
        expr = expr.trim();

        String[] cmpOps = {">=", "<=", "==", "!=", ">", "<"};
        for (String op : cmpOps) {
            int idx = findOperatorIndex(expr, op);
            if (idx >= 0) {
                String left = expr.substring(0, idx).trim();
                String right = expr.substring(idx + op.length()).trim();
                double lv = evalArithmetic(left);
                double rv = evalArithmetic(right);
                switch (op) {
                    case ">=": return lv >= rv;
                    case "<=": return lv <= rv;
                    case "==": return lv == rv;
                    case "!=": return lv != rv;
                    case ">":  return lv > rv;
                    case "<":  return lv < rv;
                }
            }
        }

        return evalArithmetic(expr) != 0;
    }

    private int findOperatorIndex(String expr, String op) {
