package com.oreo.gui;

import com.oreo.bedrock.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

final class NativeDialogParser {
    private NativeDialogParser() {}

    static NativeDialogDefinition parse(String id, String title, ConfigurationSection section, BedrockFormParser parser) {
        if (section == null) throw new IllegalArgumentException("DIALOG requires a dialog block");
        String type = section.getString("type", "MULTI_ACTION").toUpperCase(Locale.ROOT);
        if (!Set.of("MULTI_ACTION", "CONFIRMATION").contains(type)) throw new IllegalArgumentException("Unknown dialog type " + type);
        int columns = section.getInt("columns", 2);
        if (columns < 1 || columns > 10) throw new IllegalArgumentException("columns must be 1..10");
        List<NativeDialogDefinition.Body> body = new ArrayList<>();
        if (section.isString("content")) body.add(new NativeDialogDefinition.Body(section.getString("content"), null));
        for (Map<?, ?> entry : section.getMapList("body")) {
            String kind = Objects.toString(entry.get("type"), "TEXT").toUpperCase(Locale.ROOT);
            String text = Objects.toString(entry.get("text"), "");
            if (kind.equals("TEXT")) body.add(new NativeDialogDefinition.Body(text, null));
            else if (kind.equals("ITEM")) {
                Material material = Material.matchMaterial(Objects.toString(entry.get("material"), ""));
                if (material == null || !material.isItem() || material.isAir()) throw new IllegalArgumentException("Invalid dialog item");
                body.add(new NativeDialogDefinition.Body(text, material));
            } else throw new IllegalArgumentException("Unknown body type " + kind);
        }
        List<BedrockFormInput> inputs = new ArrayList<>();
        Set<String> keys = new HashSet<>(), multiline = new HashSet<>();
        for (Map<String, Object> map : entries(section, "inputs")) {
            String key = Objects.toString(map.get("key"), "");
            if (!key.matches("[A-Za-z0-9_.-]{1,64}") || !keys.add(key)) throw new IllegalArgumentException("Invalid or duplicate input key " + key);
            String kind = Objects.toString(map.get("type"), "TEXT").toUpperCase(Locale.ROOT);
            if (kind.equals("MULTILINE_TEXT") || Boolean.TRUE.equals(map.get("multiline"))) multiline.add(key);
            map.put("type", switch (kind) {
                case "TEXT", "MULTILINE_TEXT" -> "INPUT";
                case "BOOLEAN" -> "TOGGLE";
                case "SINGLE_OPTION" -> "DROPDOWN";
                case "NUMBER_RANGE" -> "SLIDER";
                default -> kind;
            });
            BedrockFormInput input = parser.loadInput(id, map);
            if (input == null) throw new IllegalArgumentException("Invalid dialog input " + key);
            switch (input.getType()) {
                case INPUT -> { if (input.getMaxLength() > 4096) throw new IllegalArgumentException("max-length exceeds 4096"); }
                case DROPDOWN, STEP_SLIDER -> {
                    if (input.getOptions().isEmpty() || input.getDefaultIndex() < 0 || input.getDefaultIndex() >= input.getOptions().size())
                        throw new IllegalArgumentException("Invalid options for " + key);
                }
                case SLIDER -> {
                    if (!Float.isFinite(input.getMin()) || !Float.isFinite(input.getMax()) || !Float.isFinite(input.getStep())
                            || !Float.isFinite(input.getDefaultFloat()) || input.getMin() >= input.getMax() || input.getStep() <= 0
                            || input.getDefaultFloat() < input.getMin() || input.getDefaultFloat() > input.getMax())
                        throw new IllegalArgumentException("Invalid range for " + key);
                }
                case LABEL -> throw new IllegalArgumentException("Use body for labels");
                default -> {}
            }
            inputs.add(input);
        }
        List<BedrockButton> buttons = new ArrayList<>();
        for (Map<String, Object> map : entries(section, "buttons")) {
            if (map.containsKey("label")) map.put("text", map.get("label"));
            BedrockButton button = parser.loadButton(id, map);
            if (button == null) throw new IllegalArgumentException("Invalid dialog button");
            buttons.add(button);
        }
        if (buttons.isEmpty() || (type.equals("CONFIRMATION") && buttons.size() != 2))
            throw new IllegalArgumentException("Dialog requires buttons; CONFIRMATION requires exactly two");
        return new NativeDialogDefinition(section.getString("title", title), section.getString("external-title"),
                section.getBoolean("close-on-escape", true), columns, type.equals("CONFIRMATION"), body, inputs, buttons, multiline);
    }

    private static List<Map<String, Object>> entries(ConfigurationSection section, String key) {
        List<Map<String, Object>> result = new ArrayList<>();
        ConfigurationSection named = section.getConfigurationSection(key);
        if (named != null) {
            for (String name : named.getKeys(false)) {
                ConfigurationSection entry = named.getConfigurationSection(name);
                if (entry == null) throw new IllegalArgumentException("Invalid " + key + " entry " + name);
                Map<String, Object> values = new LinkedHashMap<>(entry.getValues(false));
                values.putIfAbsent("key", name);
                result.add(values);
            }
        } else {
            for (Map<?, ?> entry : section.getMapList(key)) {
                Map<String, Object> values = new LinkedHashMap<>();
                entry.forEach((k, v) -> values.put(String.valueOf(k), v));
                result.add(values);
            }
        }
        return result;
    }
}
