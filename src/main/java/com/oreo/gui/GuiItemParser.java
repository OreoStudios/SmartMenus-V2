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
            List<?> rawOnPlace = itemSection.getList("on-place", Collections.emptyList());
            for (Object obj : rawOnPlace) {
                if (obj instanceof String s) {
                    onPlaceCommands.add(s);
                    onPlaceActions.add(new ActionFactory.ConsoleCommandAction(s));
                } else if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Action a = ActionFactory.parseAction((Map<String, Object>) obj);
                    if (a != null) onPlaceActions.add(a);
                }
            }
            boolean removeOnPlace = itemSection.getBoolean("remove-on-place", true);

            CooldownConfig itemCooldown = null;
            ConfigurationSection itemCooldownSection = itemSection.getConfigurationSection("cooldown");
            if (itemCooldownSection != null) {
                itemCooldown = CooldownConfig.parse(itemCooldownSection, guiId + ":slot:" + slot);
            }

            return new GuiItem.Builder()
                    .slot(slot)
                    .material(material)
                    .name(name)
                    .lore(lore)
                    .commands(commands)
                    .closeOnClick(closeOnClick)
                    .price(price)
                    .requirement(requirement)
                    .conditions(itemConditions)
                    .itemType(itemType)
                    .itemId(itemId)
                    .customModelData(customModelData)
                    .clickActions(clickActions)
                    .viewRequirements(viewRequirements)
                    .elseItem(elseItem)
                    .buttonType(buttonType)
                    .chatInputType(chatInputType)
                    .chatPromptMessage(chatPromptMessage)
                    .chatCancelWord(chatCancelWord)
                    .glow(glow)
                    .itemFlags(itemFlags)
                    .enchantments(enchantments)
                    .autoUpdate(autoUpdate)
                    .updateInterval(updateInterval)
                    .permanent(permanent)
                    .dynamicSource(dynamicSource)
                    .takeItem(takeItem)
                    .giveItem(giveItem)
                    .amount(amount)
                    .onPlaceCommands(onPlaceCommands)
                    .onPlaceActions(onPlaceActions)
                    .removeOnPlace(removeOnPlace)
                    .cooldown(itemCooldown)
                    .build();

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load item at slot " + slot + " in GUI " + guiId + ": " + e.getMessage(), e);
            return null;
        }
    }

    GuiItem loadElseItem(String guiId, ConfigurationSection section) {
        try {
            String material = section.getString("material", "STONE");
            String name = section.getString("name", "");
            List<String> lore = ColorUtil.colorList(section.getStringList("lore"));
            boolean closeOnClick = section.getBoolean("close", false);
            boolean glow = section.getBoolean("glow", false);
            List<String> itemFlags = section.getStringList("item_flags");

            Map<String, List<Action>> clickActions = new HashMap<>();
            if (section.isList("actions")) {
                List<Action> anyActions = ActionFactory.parseActions(section.getList("actions"));
                if (!anyActions.isEmpty()) clickActions.put("ANY", anyActions);
            }

            ConfigurationSection clickActionsSection = section.getConfigurationSection("click_actions");
            if (clickActionsSection != null) {
                for (String clickType : clickActionsSection.getKeys(false)) {
                    if (clickActionsSection.isList(clickType)) {
                        List<Action> typeActions = ActionFactory.parseActions(clickActionsSection.getList(clickType));
                        if (!typeActions.isEmpty()) clickActions.put(clickType.toUpperCase(), typeActions);
                    }
                }
            }

            String itemType = section.getString("item_type", "vanilla").toLowerCase();
            String itemId = section.getString("item_id");
            Integer customModelData = section.isSet("custom_model_data") ? section.getInt("custom_model_data") : null;
            boolean autoUpdate = section.getBoolean("update", false);
            int amount = Math.max(1, section.getInt("amount", 1));

            if (material != null && material.toLowerCase().startsWith("hdb:")) {
                itemId = material.substring(4);
                itemType = "headdatabase";
                material = "PLAYER_HEAD";
            }

            List<Condition> viewRequirements = conditions.parseFromKey(section, "view-requirements");
            GuiItem nestedElseItem = null;
            if (section.isSet("else-item")) {
                ConfigurationSection elseSection = section.getConfigurationSection("else-item");
                if (elseSection != null) {
                    nestedElseItem = loadElseItem(guiId, elseSection);
                }
            }

            return new GuiItem.Builder()
                    .material(material)
                    .name(name)
                    .lore(lore)
                    .closeOnClick(closeOnClick)
                    .itemType(itemType)
                    .itemId(itemId)
                    .customModelData(customModelData)
                    .clickActions(clickActions)
                    .viewRequirements(viewRequirements)
                    .elseItem(nestedElseItem)
                    .glow(glow)
                    .itemFlags(itemFlags)
                    .autoUpdate(autoUpdate)
                    .amount(amount)
                    .build();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load else-item/fill in GUI " + guiId + ": " + e.getMessage());
            return null;
        }
    }
}
