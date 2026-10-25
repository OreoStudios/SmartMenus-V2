package com.oreo.api;

import com.oreo.SmartMenus;
import com.oreo.action.Action;
import com.oreo.action.ActionFactory;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionFactory;
import com.oreo.gui.GuiDefinition;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class SmartMenusAPI {

    private static SmartMenusAPI instance;

    private final SmartMenus plugin;

    private final Map<String, Function<Map<String, Object>, Action>> customActions = new HashMap<>();

    private final Map<String, ConditionParser> customConditions = new HashMap<>();

    private SmartMenusAPI(SmartMenus plugin) {
        this.plugin = plugin;
    }

    public static void init(SmartMenus plugin) {
        instance = new SmartMenusAPI(plugin);
        ActionFactory.setCustomRegistry(instance.customActions);
        ConditionFactory.setCustomRegistry(instance.customConditions);
    }

    public static SmartMenusAPI get() {
        return instance;
    }

    public static SmartMenusAPI require() {
        if (instance == null) {
            throw new IllegalStateException("SmartMenus is not loaded. Make sure 'depend: [SmartMenus]' is in your plugin.yml.");
        }
        return instance;
    }

    public boolean openGui(Player player, String guiId) {
        GuiDefinition def = plugin.getGuiRegistry().getGui(guiId);
        if (def == null) return false;
        com.oreo.bedrock.BedrockManager bm = plugin.getBedrockManager();
        if (bm != null && (bm.openForBedrock(player, def) || bm.autoConvertForBedrock(player, def))) return true;
        def.createInventory(plugin.getInventoryManager(), plugin, player).open(player);
        return true;
    }

    public boolean openGui(Player player, String guiId, Map<String, String> args) {
        GuiDefinition def = plugin.getGuiRegistry().getGui(guiId);
        if (def == null) return false;
        com.oreo.gui.ArgManager.store(player.getUniqueId(), args);
        com.oreo.bedrock.BedrockManager bm = plugin.getBedrockManager();
        if (bm != null && (bm.openForBedrock(player, def) || bm.autoConvertForBedrock(player, def))) return true;
        def.createInventory(plugin.getInventoryManager(), plugin, player).open(player);
        return true;
    }

    public boolean isGuiLoaded(String guiId) {
        return plugin.getGuiRegistry().getGui(guiId) != null;
    }

    public GuiDefinition getGui(String guiId) {
        return plugin.getGuiRegistry().getGui(guiId);
    }

    public Set<String> getGuiIds() {
        return Collections.unmodifiableSet(plugin.getGuiRegistry().getGuiIds());
    }

    public void reloadGuis() {
        plugin.reloadGuis();
    }

    public boolean evalConditionScript(String script, Player player) {
        return plugin.getScriptEngine().evalCondition(script, player);
    }

    public void evalActionScript(String script, Player player) {
        plugin.getScriptEngine().evalAction(script, player);
    }

    public void registerAction(String type, Function<Map<String, Object>, Action> factory) {
        String key = type.toUpperCase();
        customActions.put(key, factory);
        plugin.getLogger().info("[SmartMenus API] Registered custom action type: " + key);
    }

    public void unregisterAction(String type) {
        customActions.remove(type.toUpperCase());
    }

    public void registerCondition(String type, ConditionParser factory) {
        String key = type.toUpperCase();
        customConditions.put(key, factory);
        plugin.getLogger().info("[SmartMenus API] Registered custom condition type: " + key);
    }

    public void unregisterCondition(String type) {
        customConditions.remove(type.toUpperCase());
    }

    @FunctionalInterface
    public interface ConditionParser {

        Condition parse(SmartMenus plugin, org.bukkit.configuration.ConfigurationSection section);
    }

    public SmartMenus getPlugin() {
        return plugin;
    }
}
