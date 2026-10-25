package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Parses a YAML list of condition maps (under a given key) into {@link Condition} objects.
 * Shared by the GUI registry and its item/bedrock parsers.
 */
final class ConditionListParser {

    private final SmartMenus plugin;
    private final ConditionFactory conditionFactory;

    ConditionListParser(SmartMenus plugin, ConditionFactory conditionFactory) {
        this.plugin = plugin;
        this.conditionFactory = conditionFactory;
    }

    List<Condition> parseFromKey(ConfigurationSection section, String key) {
        if (!section.isList(key)) {
            return Collections.emptyList();
        }

        List<?> conditionsList = section.getList(key);
        if (conditionsList == null) {
            return Collections.emptyList();
        }

        List<Condition> conditions = new ArrayList<>();
        for (Object condObj : conditionsList) {
            if (!(condObj instanceof Map)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> condMap = (Map<String, Object>) condObj;

            YamlConfiguration tempConfig = new YamlConfiguration();
            for (Map.Entry<String, Object> entry : condMap.entrySet()) {
                tempConfig.set(entry.getKey(), entry.getValue());
            }

            try {
                Condition condition = conditionFactory.parseCondition(tempConfig);
                if (condition != null) {
                    conditions.add(condition);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to parse condition: " + e.getMessage(), e);
            }
        }

        return conditions;
    }
}
