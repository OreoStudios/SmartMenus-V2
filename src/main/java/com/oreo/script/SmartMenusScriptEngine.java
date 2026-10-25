package com.oreo.script;

import com.oreo.SmartMenus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public class SmartMenusScriptEngine {

    private final SmartMenus plugin;
    private ScriptEngine engine;
    private boolean unavailableWarned = false;
    private boolean initialized = false;

    public SmartMenusScriptEngine(SmartMenus plugin) {
        this.plugin = plugin;
    }

    private ScriptEngine getEngine() {
        if (initialized) return engine;
        initialized = true;

        ScriptEngineManager manager = new ScriptEngineManager();

        engine = manager.getEngineByName("nashorn");
        if (engine == null) {
            engine = manager.getEngineByName("javascript");
        }
        if (engine == null && !unavailableWarned) {
            unavailableWarned = true;
            plugin.getLogger().warning(
                    "[SmartMenus] Script conditions/actions require Nashorn (Java 8-14) or GraalVM JavaScript. " +
                    "Scripts will be skipped on this server.");
        }
        return engine;
    }

    public boolean evalCondition(String script, Player player) {
        if (script == null || script.isEmpty()) return false;

        if (player != null) script = resolvePlaceholders(script, player);

        ScriptEngine eng = getEngine();
        if (eng == null) {

            return evalSimpleExpression(script.trim());
        }
        try {
            Bindings bindings = new SimpleBindings();
            bindings.put("player", player);
            Object result = eng.eval(script, bindings);
            if (result instanceof Boolean) return (Boolean) result;
            if (result instanceof Number) return ((Number) result).doubleValue() != 0;
            if (result instanceof String) return Boolean.parseBoolean((String) result);
            return result != null;
        } catch (ScriptException e) {
            plugin.getLogger().warning("ScriptCondition error: " + e.getMessage());
            return false;
        }
    }

    private boolean evalSimpleExpression(String expr) {

        String[] operators = {">=", "<=", "!=", "==", ">", "<"};
        for (String op : operators) {
            int idx = expr.indexOf(op);
            if (idx < 0) continue;
            String left  = expr.substring(0, idx).trim();
            String right = expr.substring(idx + op.length()).trim();
            try {
                double l = Double.parseDouble(left);
                double r = Double.parseDouble(right);
                switch (op) {
                    case "==": return l == r;
                    case "!=": return l != r;
                    case ">=": return l >= r;
                    case "<=": return l <= r;
                    case ">":  return l > r;
                    case "<":  return l < r;
                }
            } catch (NumberFormatException e) {

                switch (op) {
                    case "==": return left.equalsIgnoreCase(right);
                    case "!=": return !left.equalsIgnoreCase(right);
                    default:   return false;
                }
            }
        }

        return !expr.isEmpty() && !expr.equalsIgnoreCase("false") && !expr.equals("0");
    }

    private String resolvePlaceholders(String text, Player player) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return text;
        }
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) papiClass.getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(null, player, text);
        } catch (Exception e) {
            return text;
        }
    }

    public void evalAction(String script, Player player) {
        ScriptEngine eng = getEngine();
        if (eng == null) return;
        if (script == null || script.isEmpty()) return;
        try {
            Bindings bindings = new SimpleBindings();
            bindings.put("player", player);
            bindings.put("plugin", plugin);
            eng.eval(script, bindings);
        } catch (ScriptException e) {
            plugin.getLogger().warning("ScriptAction error: " + e.getMessage());
        }
    }
}
