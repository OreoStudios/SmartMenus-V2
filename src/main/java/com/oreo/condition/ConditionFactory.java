package com.oreo.condition;

import com.oreo.SmartMenus;
import com.oreo.condition.impl.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.Locale;
import java.util.Map;

public class ConditionFactory {
    private final SmartMenus plugin;

    private static java.util.Map<String, com.oreo.api.SmartMenusAPI.ConditionParser> customRegistry = null;

    public static void setCustomRegistry(java.util.Map<String, com.oreo.api.SmartMenusAPI.ConditionParser> registry) {
        customRegistry = registry;
    }

    public ConditionFactory(SmartMenus plugin) {
        this.plugin = plugin;
    }

    public List<Condition> parseConditions(ConfigurationSection section) {
        List<Condition> conditions = new ArrayList<>();
        if (section == null || !section.isList("conditions")) {
            return conditions;
        }

        List<?> conditionsList = section.getList("conditions");
        if (conditionsList == null) return conditions;

        for (Object obj : conditionsList) {
            if (!(obj instanceof ConfigurationSection)) continue;
            ConfigurationSection conditionSection = (ConfigurationSection) obj;
            Condition condition = parseCondition(conditionSection);
            if (condition != null) conditions.add(condition);
        }
        return conditions;
    }

    public Condition parseCondition(ConfigurationSection section) {
        String type = section.getString("type");
        if (type == null) return null;

        try {
            switch (type.toUpperCase(Locale.ROOT)) {
                case "VAULT_MONEY":
                case "MONEY":
                    return new VaultMoneyCondition(plugin, section.getDouble("amount", 0.0));

                case "OREO_CURRENCY":
                case "CURRENCY":
                    return new OreoCurrencyCondition(plugin,
                            section.getString("currency"),
                            section.getDouble("amount", 0.0));

                case "OREO_WARPS":
                case "OREO_WARP":
                case "WARPS":
                case "WARP":
                    return new OreoWarpsCondition(plugin,
                            section.getString("warp"),
                            section.getBoolean("check_exists", true),
                            section.getBoolean("check_permission", false));

                case "OREO_WARPS_LOCATION":
                case "OREO_WARP_LOCATION":
                case "WARP_LOCATION":
                case "AT_WARP":
                    return new OreoWarpsLocationCondition(plugin,
                            section.getString("warp"),
                            section.getDouble("radius", 5.0),
                            section.getString("error_message"));

                case "XP_LEVEL":
                case "LEVEL":
                    return new XpLevelCondition(plugin, section.getInt("amount", 0));

                case "XP_POINTS":
                case "XP":
                    return new XpPointsCondition(plugin, section.getInt("amount", 0));

                case "ITEM":
                    Material mat = Material.matchMaterial(section.getString("material"));
                    if (mat == null) {
                        Map<String, String> replacements = new HashMap<>();
                        replacements.put("material", section.getString("material", "UNKNOWN"));
                        plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.invalid_material", replacements));
                        return null;
                    }
                    return new ItemCondition(plugin, mat, section.getInt("amount", 1));

                case "ITEM_CUSTOM_MODEL":
                    Material mat2 = Material.matchMaterial(section.getString("material"));
                    if (mat2 == null) {
                        Map<String, String> replacements = new HashMap<>();
                        replacements.put("material", section.getString("material", "UNKNOWN"));
                        plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.invalid_material", replacements));
                        return null;
                    }
                    return new ItemCondition(plugin, mat2, section.getInt("amount", 1),
                            section.getInt("custom_model_data"));

                case "ITEMSADDER":
                    return new ItemsAdderCondition(plugin,
                            section.getString("item_id"),
                            section.getInt("amount", 1));

                case "NEXO":
                    return new NexoCondition(plugin,
                            section.getString("item_id"),
                            section.getInt("amount", 1));

                case "PERMISSION":
                    return new PermissionCondition(plugin, section.getString("permission"));

                case "WORLDGUARD_REGION":
                case "REGION":
                    return new WorldGuardRegionCondition(plugin,
                            section.getString("region"),
                            section.getBoolean("require_member", false));

                case "PLACEHOLDER":
                    return new PlaceholderCondition(plugin,
                            section.getString("placeholder"),
                            section.getString("operator", "=="),
                            section.getString("value"),
                            section.getString("error_message"));

                case "WEATHER":
                    return new WeatherCondition(plugin, section.getString("weather", "clear"));

                case "WORLD":
                    List<String> worlds = section.getStringList("worlds");
                    if (worlds.isEmpty()) {
                        String singleWorld = section.getString("world");
                        if (singleWorld != null) {
                            worlds = new ArrayList<>();
                            worlds.add(singleWorld);
                        }
                    }
                    if (worlds.isEmpty()) {
                        plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.world_condition_no_worlds"));
                        return null;
                    }
                    return new WorldCondition(plugin, worlds, section.getBoolean("blacklist", false));
                case "LUCKPERMS_GROUP":
                case "LUCKPERMS_RANK":
                case "LUCKPERMS":
                case "GROUP":
                case "RANK":
                    return new LuckPermsGroupCondition(
                            plugin,
                            section.getString("group"),
                            section.getBoolean("include_inherited", true),
                            section.getBoolean("negate", false),
                            section.getString("error_message")
                    );

                case "MODELED_NPC":
                case "NPC":
                case "NEAR_NPC":
                    return new ModeledNPCCondition(plugin,
                            section.getInt("npc_id"),
                            section.getDouble("radius", 5.0));

                case "EXPRESSION":
                    return new ExpressionCondition(plugin, section.getString("expression", ""));

                case "PLACEHOLDER_EQUALS":
                    return new PlaceholderCompareCondition(plugin,
                            section.getString("placeholder", ""),
                            section.getString("value", ""),
                            PlaceholderCompareCondition.CompareMode.EQUALS,
                            section.getBoolean("ignore_case", true));

                case "PLACEHOLDER_CONTAINS":
                    return new PlaceholderCompareCondition(plugin,
                            section.getString("placeholder", ""),
                            section.getString("value", ""),
                            PlaceholderCompareCondition.CompareMode.CONTAINS,
                            section.getBoolean("ignore_case", true));

                case "PLACEHOLDER_GREATER_THAN":
                    return new PlaceholderCompareCondition(plugin,
                            section.getString("placeholder", ""),
                            section.getString("value", "0"),
                            PlaceholderCompareCondition.CompareMode.GREATER_THAN,
                            false);

                case "PLACEHOLDER_LESS_THAN":
                    return new PlaceholderCompareCondition(plugin,
                            section.getString("placeholder", ""),
                            section.getString("value", "0"),
                            PlaceholderCompareCondition.CompareMode.LESS_THAN,
                            false);

                case "SCRIPT":

                    return new ScriptCondition(plugin,
                            section.getString("script", section.getString("expression", "")));

                default:

                    if (customRegistry != null) {
                        com.oreo.api.SmartMenusAPI.ConditionParser factory = customRegistry.get(type.toUpperCase(Locale.ROOT));
                        if (factory != null) {
                            try {
                                return factory.parse(plugin, section);
                            } catch (Exception e) {
                                Map<String, String> errReplacements = new HashMap<>();
                                errReplacements.put("error", e.getMessage());
                                plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.condition_parse_failed", errReplacements));
                            }
                        }
                    }
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("type", type);
                    plugin.getLogger().warning(plugin.getMessageManager().getMessage("errors.unknown_condition_type", replacements));
                    return null;
            }
        } catch (Exception e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            plugin.getLogger().log(Level.WARNING, plugin.getMessageManager().getMessage("errors.condition_parse_failed", replacements), e);
            return null;
        }
    }
}
