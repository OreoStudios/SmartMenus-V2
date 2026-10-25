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
        try {
            String text      = map.containsKey("text")       ? map.get("text").toString()       : "Button";
            String imageType = map.containsKey("image-type") ? map.get("image-type").toString() : null;
            String imageData = map.containsKey("image-data") ? map.get("image-data").toString() : null;

            List<Condition> buttonConditions = Collections.emptyList();
            if (map.containsKey("conditions") && map.get("conditions") instanceof List) {
                YamlConfiguration tmp = new YamlConfiguration();
                tmp.set("conditions", map.get("conditions"));
                ConfigurationSection tmpSection = tmp.createSection("__btn");
                tmpSection.set("conditions", map.get("conditions"));
                buttonConditions = conditions.parseFromKey(tmpSection, "conditions");
            }

            List<Action> actions = Collections.emptyList();
            if (map.containsKey("actions") && map.get("actions") instanceof List) {
                actions = ActionFactory.parseActions((List<?>) map.get("actions"));
            }

            return new BedrockButton(text, imageType, imageData, buttonConditions, actions);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse bedrock button in GUI " + guiId + ": " + e.getMessage());
            return null;
        }
    }

    private BedrockFormInput loadInput(String guiId, Map<String, Object> map) {
        try {
            String typeStr = map.containsKey("type") ? map.get("type").toString().toUpperCase() : "INPUT";
            String label   = map.containsKey("label") ? map.get("label").toString() : "";

            String key     = map.containsKey("key")   ? map.get("key").toString()   : Ids.slugify(label);

            switch (typeStr) {
                case "INPUT":
                    return BedrockFormInput.input(
                            key, label,
                            map.containsKey("placeholder") ? map.get("placeholder").toString() : "",
                            map.containsKey("default")     ? map.get("default").toString()     : "",
                            map.containsKey("max-length")  ? Integer.parseInt(map.get("max-length").toString()) : 256);
                case "DROPDOWN": {
                    List<String> opts = map.containsKey("options") && map.get("options") instanceof List
                            ? toStringList((List<?>) map.get("options"))
                            : new ArrayList<>();
                    int defIdx = map.containsKey("default-index") ? Integer.parseInt(map.get("default-index").toString()) : 0;
                    return BedrockFormInput.dropdown(key, label, opts, defIdx);
                }
                case "SLIDER": {
                    float min  = map.containsKey("min")   ? Float.parseFloat(map.get("min").toString())   : 0f;
                    float max  = map.containsKey("max")   ? Float.parseFloat(map.get("max").toString())   : 100f;
                    float step = map.containsKey("step")  ? Float.parseFloat(map.get("step").toString())  : 1f;
                    float def  = map.containsKey("default") ? Float.parseFloat(map.get("default").toString()) : min;
                    return BedrockFormInput.slider(key, label, min, max, step, def);
                }
                case "STEP_SLIDER": {
                    List<String> steps = map.containsKey("steps") && map.get("steps") instanceof List
                            ? toStringList((List<?>) map.get("steps"))
                            : new ArrayList<>();
                    int defIdx = map.containsKey("default-index") ? Integer.parseInt(map.get("default-index").toString()) : 0;
                    return BedrockFormInput.stepSlider(key, label, steps, defIdx);
                }
                case "TOGGLE": {
                    boolean def = map.containsKey("default") && Boolean.parseBoolean(map.get("default").toString());
                    return BedrockFormInput.toggle(key, label, def);
                }
                case "LABEL":
                    return BedrockFormInput.label(label);
                default:
                    plugin.getLogger().warning("Unknown bedrock input type '" + typeStr + "' in GUI " + guiId);
                    return null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse bedrock input in GUI " + guiId + ": " + e.getMessage());
            return null;
        }
    }

    private List<String> toStringList(List<?> raw) {
        List<String> result = new ArrayList<>();
        for (Object o : raw) {
            if (o != null) result.add(o.toString());
        }
        return result;
    }
}
