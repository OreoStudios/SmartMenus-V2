package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.action.Action;
import com.oreo.action.ActionFactory;
import com.oreo.condition.Condition;
import com.oreo.util.ColorUtil;
import com.oreo.util.CooldownConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/** Builds {@link GuiItem}s (and their nested else-items) from an item's config section. */
final class GuiItemParser {

    private final SmartMenus plugin;
    private final ConditionListParser conditions;

    GuiItemParser(SmartMenus plugin, ConditionListParser conditions) {
        this.plugin = plugin;
        this.conditions = conditions;
    }

    GuiItem loadItem(String guiId, int slot, ConfigurationSection itemSection) {
        try {
            String material = itemSection.getString("material", "STONE");
            String name = itemSection.getString("name", "");
            List<String> lore = ColorUtil.colorList(itemSection.getStringList("lore"));
            List<String> commands = itemSection.getStringList("commands");
            boolean closeOnClick = itemSection.getBoolean("close", false);

            double price = itemSection.getDouble("price", 0.0);
            String requirement = itemSection.getString("requirement", "");

            String itemType = itemSection.getString("item_type", "vanilla").toLowerCase();
            String itemId = itemSection.getString("item_id");
            Integer customModelData = itemSection.isSet("custom_model_data") ?
                    itemSection.getInt("custom_model_data") : null;

            if (material != null && material.toLowerCase().startsWith("hdb:")) {
                itemId = material.substring(4);
                itemType = "headdatabase";
                material = "PLAYER_HEAD";
            }
            if ("headdatabase".equalsIgnoreCase(itemType) && itemId == null) {
                itemId = itemSection.getString("item_id");
            }

            List<Condition> itemConditions = conditions.parseFromKey(itemSection, "conditions");
            if (!itemConditions.isEmpty()) {
                plugin.getLogger().info("  Slot " + slot + ": Loaded " + itemConditions.size() + " condition(s)");
            }
            if (!"vanilla".equals(itemType)) {
                plugin.getLogger().info("  Slot " + slot + ": Using " + itemType + " item: " + itemId);
            }

            Map<String, List<Action>> clickActions = new HashMap<>();

            if (itemSection.isList("actions")) {
                List<Action> anyActions = ActionFactory.parseActions(itemSection.getList("actions"));
                if (!anyActions.isEmpty()) clickActions.put("ANY", anyActions);
            }

            ConfigurationSection clickActionsSection = itemSection.getConfigurationSection("click_actions");
            if (clickActionsSection != null) {
                for (String clickType : clickActionsSection.getKeys(false)) {
                    if (clickActionsSection.isList(clickType)) {
                        List<Action> typeActions = ActionFactory.parseActions(clickActionsSection.getList(clickType));
                        if (!typeActions.isEmpty()) {
                            clickActions.put(clickType.toUpperCase(), typeActions);
                        }
                    }
                }
            }

            if (!commands.isEmpty()) {
                List<Action> legacyActions = new ArrayList<>();
                for (String cmd : commands) {
                    legacyActions.add(new ActionFactory.ConsoleCommandAction(cmd));
                }
                clickActions.put("LEGACY", legacyActions);
            }

            List<Condition> viewRequirements = conditions.parseFromKey(itemSection, "view-requirements");
            GuiItem elseItem = null;
            if (itemSection.isSet("else-item")) {
                ConfigurationSection elseSection = itemSection.getConfigurationSection("else-item");
                if (elseSection != null) {
                    elseItem = loadElseItem(guiId, elseSection);
                }
            }

            ButtonType buttonType = ButtonType.NONE;
            if (itemSection.isSet("type")) {
                buttonType = ButtonType.fromString(itemSection.getString("type", ""));
            }
            String chatInputType = itemSection.getString("chat-input-type", "TEXT");
            String chatPromptMessage = itemSection.getString("chat-prompt", "&7Please type in chat.");
            String chatCancelWord = itemSection.getString("chat-cancel-word", "cancel");

            boolean glow = itemSection.getBoolean("glow", false);
            List<String> itemFlags = itemSection.getStringList("item_flags");
            Map<String, Integer> enchantments = new HashMap<>();
            if (itemSection.isSet("enchantments")) {
                ConfigurationSection enchSection = itemSection.getConfigurationSection("enchantments");
                if (enchSection != null) {
                    for (String enchKey : enchSection.getKeys(false)) {
                        enchantments.put(enchKey, enchSection.getInt(enchKey, 1));
                    }
                }
            }

            boolean autoUpdate = itemSection.getBoolean("update", false);
            int updateInterval = itemSection.getInt("update-interval", 20);

            boolean permanent = itemSection.getBoolean("is-permanent", itemSection.getBoolean("permanent", false));
            DynamicSource dynamicSource = DynamicSource.fromString(itemSection.getString("source", ""));
            boolean takeItem = itemSection.getBoolean("take-item", itemSection.getBoolean("take_item", false));
            boolean giveItem = itemSection.getBoolean("give-item", itemSection.getBoolean("give_item", false));
            int amount = Math.max(1, itemSection.getInt("amount", 1));

            List<String> onPlaceCommands = new ArrayList<>();
            List<Action> onPlaceActions = new ArrayList<>();
