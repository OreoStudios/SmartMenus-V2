package com.oreo.gui;

import com.oreo.SmartMenus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PatternRegistry {

    private final SmartMenus plugin;
    private final Map<String, ConfigurationSection> patterns = new HashMap<>();

    public PatternRegistry(SmartMenus plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        patterns.clear();
        File patternsDir = new File(plugin.getDataFolder(), "patterns");
        if (!patternsDir.exists()) {
            patternsDir.mkdirs();
            return;
        }

        File[] files = patternsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection patternsSection = config.getConfigurationSection("patterns");
                if (patternsSection == null) continue;

                for (String patternId : patternsSection.getKeys(false)) {
                    ConfigurationSection patternSection = patternsSection.getConfigurationSection(patternId);
                    if (patternSection == null) continue;
                    ConfigurationSection itemsSection = patternSection.getConfigurationSection("items");
                    if (itemsSection == null) continue;
                    patterns.put(patternId, itemsSection);
                    plugin.getLogger().info("Loaded pattern: " + patternId + " from " + file.getName());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load pattern file: " + file.getName() + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + patterns.size() + " pattern(s) from patterns/ folder.");
    }

    public ConfigurationSection getPattern(String id) {
        return patterns.get(id);
    }

    public void applyPattern(String id, ConfigurationSection targetItems, Map<String, String> vars) {
        ConfigurationSection patternItems = patterns.get(id);
        if (patternItems == null) {
            plugin.getLogger().warning("Pattern not found: " + id);
            return;
        }

        for (String key : patternItems.getKeys(false)) {

            if (targetItems.isSet(key)) continue;

            ConfigurationSection patternItem = patternItems.getConfigurationSection(key);
            if (patternItem == null) continue;

            for (String itemKey : patternItem.getKeys(true)) {
                Object value = patternItem.get(itemKey);
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (vars != null) {
                        for (Map.Entry<String, String> e : vars.entrySet()) {
                            strValue = strValue.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
                        }
                    }
                    targetItems.set(key + "." + itemKey, strValue);
                } else {
                    targetItems.set(key + "." + itemKey, value);
                }
            }
        }
    }
}
