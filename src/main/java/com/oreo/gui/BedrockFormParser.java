package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.action.Action;
import com.oreo.action.ActionFactory;
import com.oreo.bedrock.BedrockButton;
import com.oreo.bedrock.BedrockFormDefinition;
import com.oreo.bedrock.BedrockFormInput;
import com.oreo.condition.Condition;
import com.oreo.util.Ids;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds a {@link BedrockFormDefinition} (buttons and inputs) from a GUI's {@code bedrock:} block. */
final class BedrockFormParser {

    private final SmartMenus plugin;
    private final ConditionListParser conditions;

    BedrockFormParser(SmartMenus plugin, ConditionListParser conditions) {
        this.plugin = plugin;
        this.conditions = conditions;
    }

    BedrockFormDefinition load(String guiId, ConfigurationSection section) {
        try {
            String typeStr = section.getString("type", "SIMPLE_FORM").toUpperCase(Locale.ENGLISH).replace('-', '_');
            BedrockFormDefinition.FormType type;
            try {
                type = BedrockFormDefinition.FormType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown bedrock form type '" + typeStr + "' in GUI " + guiId + ", defaulting to SIMPLE_FORM.");
                type = BedrockFormDefinition.FormType.SIMPLE_FORM;
            }

            String title   = section.getString("title", guiId);
            String content = section.getString("content", "");

            List<BedrockButton> buttons = new ArrayList<>();
            List<?> buttonList = section.getList("buttons");
            if (buttonList != null) {
                for (Object obj : buttonList) {
                    if (!(obj instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> btnMap = (Map<String, Object>) obj;
                    BedrockButton btn = loadButton(guiId, btnMap);
                    if (btn != null) buttons.add(btn);
                }
            }

            String confirmButton  = section.getString("confirm-button", "Confirm");
            String denyButton     = section.getString("deny-button", "Cancel");
            List<Action> confirmActions = ActionFactory.parseActions(section.getList("confirm-actions"));
            List<Action> denyActions    = ActionFactory.parseActions(section.getList("deny-actions"));

            List<BedrockFormInput> inputs = new ArrayList<>();
            List<?> inputList = section.getList("inputs");
            if (inputList != null) {
                for (Object obj : inputList) {
                    if (!(obj instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> inputMap = (Map<String, Object>) obj;
                    BedrockFormInput inp = loadInput(guiId, inputMap);
                    if (inp != null) inputs.add(inp);
                }
            }
            List<Action> submitActions    = ActionFactory.parseActions(section.getList("submit-actions"));
            String submitButtonText       = section.getString("submit-button", "Submit");
            List<Action> closeActions     = ActionFactory.parseActions(section.getList("close-actions"));

            String npcName     = section.getString("npc-name", section.getString("npc_name", "NPC"));
            String dialogueTag = section.getString("dialogue-tag", section.getString("dialogue_tag", guiId + ".dialogue"));

            plugin.getLogger().info("  Loaded bedrock form for GUI '" + guiId + "': type=" + type
                    + " buttons=" + buttons.size());

            return new BedrockFormDefinition.Builder(type)
                    .title(title)
                    .content(content)
                    .buttons(buttons)
                    .confirmButton(confirmButton)
                    .denyButton(denyButton)
                    .confirmActions(confirmActions)
                    .denyActions(denyActions)
                    .inputs(inputs)
                    .submitActions(submitActions)
                    .submitButtonText(submitButtonText)
                    .closeActions(closeActions)
                    .npcName(npcName)
                    .dialogueTag(dialogueTag)
                    .build();

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load bedrock: block for GUI " + guiId + ": " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private BedrockButton loadButton(String guiId, Map<String, Object> map) {
