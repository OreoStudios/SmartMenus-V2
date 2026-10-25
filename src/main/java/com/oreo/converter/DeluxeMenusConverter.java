package com.oreo.converter;

import com.oreo.util.Ids;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DeluxeMenusConverter {

    private record DmItem(ConfigurationSection section, int priority) {}

    public static File convert(File inputFile, File outputDir, String guiIdOverride) throws IOException {
        YamlConfiguration src = YamlConfiguration.loadConfiguration(inputFile);

        String guiId;
        if (guiIdOverride != null && !guiIdOverride.isBlank()) {
            guiId = Ids.slugify(guiIdOverride);
        } else {
            String fn = inputFile.getName();
            guiId = Ids.slugify(fn.endsWith(".yml") ? fn.substring(0, fn.length() - 4) : fn);
        }

        YamlConfiguration out = new YamlConfiguration();
        String base = guiId;

        String title = src.getString("menu_title", guiId);
        out.set(base + ".title", ConverterUtil.convertColors(title));

        int size = src.getInt("size", 54);
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(size / 9.0)));
        int maxSlot = rows * 9;
        out.set(base + ".rows", rows);

        List<String> commands = new ArrayList<>();
        Object openCmd = src.get("open_command");
        if (openCmd instanceof List<?> list) {
            for (Object o : list) {
                String t = String.valueOf(o).trim();
                if (!t.isBlank()) commands.add(t);
            }
        } else if (openCmd instanceof String s && !s.isBlank()) {
            commands.add(s.trim());
        }

        commands = new ArrayList<>(new LinkedHashSet<>(commands));
        if (!commands.isEmpty()) out.set(base + ".commands", commands);

        List<Map<String, Object>> openActions = parseCommandListToActions(src.getStringList("open_commands"));
        if (!openActions.isEmpty()) out.set(base + ".open-actions", openActions);

        List<Map<String, Object>> closeActions = parseCommandListToActions(src.getStringList("close_commands"));
        if (!closeActions.isEmpty()) out.set(base + ".close-actions", closeActions);

        ConfigurationSection openReqSec = src.getConfigurationSection("open_requirement");
        if (openReqSec != null) {
            List<Map<String, Object>> openReqs = convertViewRequirementsBlock(openReqSec);
            if (!openReqs.isEmpty()) out.set(base + ".open-requirements", openReqs);
        }

        ConfigurationSection itemsSec = src.getConfigurationSection("items");
        if (itemsSec == null) throw new IOException("Missing 'items' section in " + inputFile.getName());

        String fillMaterial = null;
        String fillName = null;

        Map<Integer, List<DmItem>> slotMap = new LinkedHashMap<>();

        for (String key : itemsSec.getKeys(false)) {
            ConfigurationSection item = itemsSec.getConfigurationSection(key);
            if (item == null) continue;

            String mat = ConverterUtil.normalizeMaterial(item.getString("material", "AIR"));
            if (mat.equalsIgnoreCase("AIR")) continue;

            List<Integer> slots = resolveSlots(item, size);
            slots = slots.stream().filter(s -> s >= 0 && s < maxSlot).toList();
            if (slots.isEmpty()) continue;

            String rawDispName = item.getString("display_name", "");
            String strippedName = rawDispName
                    .replaceAll("&#[0-9a-fA-F]{6}", "")
                    .replaceAll("&[0-9a-fk-orA-FK-OR]", "")
                    .trim();
            boolean hasName = !strippedName.isEmpty();

            boolean hasActions = item.isSet("click_commands") || item.isSet("left_click_commands")
                    || item.isSet("right_click_commands") || item.isSet("shift_left_click_commands")
                    || item.isSet("shift_right_click_commands");

            if (!hasActions && !hasName && slots.size() > 3 && mat.contains("GLASS_PANE")) {
                if (fillMaterial == null) {
                    fillMaterial = mat;
                    fillName = rawDispName;
                    continue;
                }
            }

            int priority = item.getInt("priority", 0);
            for (int slot : slots) {
                slotMap.computeIfAbsent(slot, k -> new ArrayList<>())
                       .add(new DmItem(item, priority));
            }
        }

        for (Map.Entry<Integer, List<DmItem>> entry : slotMap.entrySet()) {
            int slot = entry.getKey();
            List<DmItem> dmItems = entry.getValue();
            dmItems.sort(Comparator.comparingInt(i -> i.priority));
            writeItemChain(dmItems, 0, out, base + ".items." + slot);
        }

        if (fillMaterial != null) {
            out.set(base + ".fill.material", fillMaterial);
            String stripped = fillName == null ? "" : fillName
                    .replaceAll("&#[0-9a-fA-F]{6}", "")
                    .replaceAll("&[0-9a-fk-orA-FK-OR]", "")
                    .trim();
            if (!stripped.isEmpty()) {
                out.set(base + ".fill.name", ConverterUtil.convertColors(fillName));
            }
        }

        if (!outputDir.exists()) outputDir.mkdirs();
        File outputFile = new File(outputDir, guiId + ".yml");
        out.save(outputFile);
        return outputFile;
    }

    private static void writeItemChain(List<DmItem> items, int idx, YamlConfiguration out, String p) {
        if (idx >= items.size()) return;
        DmItem dmItem = items.get(idx);
        writeItemFields(dmItem.section, out, p);
        writeViewRequirements(dmItem.section, out, p);
        if (idx + 1 < items.size()) {
            writeItemChain(items, idx + 1, out, p + ".else-item");
        }
    }

    private static void writeItemFields(ConfigurationSection src, YamlConfiguration out, String p) {

        String mat = ConverterUtil.normalizeMaterial(src.getString("material", "STONE"));
        if (mat.equalsIgnoreCase("AIR")) return;
        out.set(p + ".material", mat);

        String name = src.getString("display_name");
        if (name != null && !name.isBlank()) out.set(p + ".name", ConverterUtil.convertColors(name));

        List<String> lore = src.getStringList("lore");
        if (!lore.isEmpty()) out.set(p + ".lore", ConverterUtil.convertColors(lore));

        int amount = src.getInt("amount", 1);
        if (amount > 1) out.set(p + ".amount", amount);

        if (src.isSet("model_data")) {
            out.set(p + ".custom_model_data", src.getInt("model_data"));
        } else if (src.isSet("custom_model_data")) {
            out.set(p + ".custom_model_data", src.getInt("custom_model_data"));
        }

        List<?> enchList = src.getList("enchantments");
        if (enchList != null && !enchList.isEmpty()) {
            Map<String, Integer> enchMap = new LinkedHashMap<>();
            for (Object e : enchList) {
                String eStr = String.valueOf(e).trim();
                String[] parts = eStr.split(":", 2);
                String enchName = parts[0].toLowerCase(Locale.ROOT);
                int enchLevel = parts.length >= 2 ? parseIntSafe(parts[1], 1) : 1;
                enchMap.put(enchName, enchLevel);
            }
            if (!enchMap.isEmpty()) out.set(p + ".enchantments", enchMap);
        }

        if (src.getBoolean("glow", false) || src.getBoolean("glint", false)) {
            out.set(p + ".glow", true);
        }

        if (src.getBoolean("update", false)) out.set(p + ".update", true);

        List<String> flags = new ArrayList<>();
        if (src.getBoolean("hide_attributes", false))     flags.add("HIDE_ATTRIBUTES");
        if (src.getBoolean("hide_enchants", false))       flags.add("HIDE_ENCHANTS");
        if (src.getBoolean("hide_potion_effects", false)) flags.add("HIDE_ADDITIONAL_TOOLTIP");
        if (src.getBoolean("hide_unbreakable", false))    flags.add("HIDE_UNBREAKABLE");
        if (!flags.isEmpty()) out.set(p + ".item_flags", flags);

        boolean closeOnClick = false;
        Map<String, List<Map<String, Object>>> perClick = new LinkedHashMap<>();

        String[][] clickMapping = {
            {"click_commands",             "ANY"},
            {"left_click_commands",        "LEFT"},
            {"right_click_commands",       "RIGHT"},
            {"shift_left_click_commands",  "SHIFT_LEFT"},
            {"shift_right_click_commands", "SHIFT_RIGHT"},
        };

        for (String[] mapping : clickMapping) {
            String dmField  = mapping[0];
            String smKey    = mapping[1];
            List<String> cmdList = src.getStringList(dmField);
            if (cmdList.isEmpty()) continue;

            List<Map<String, Object>> actions = new ArrayList<>();
            for (String cmd : cmdList) {
                cmd = cmd.trim();
                if (cmd.isEmpty()) continue;
                if (cmd.equalsIgnoreCase("[close]")) {
                    closeOnClick = true;
                } else if (cmd.equalsIgnoreCase("[refresh]")) {
                    out.set(p + ".update", true);
                } else if (isSkippedAction(cmd)) {

                } else {
                    Map<String, Object> action = parseAction(cmd);
                    if (action != null) actions.add(action);
                }
            }
            if (!actions.isEmpty()) perClick.put(smKey, actions);
        }

        if (closeOnClick) out.set(p + ".close", true);

        if (!perClick.isEmpty()) {

            if (perClick.size() == 1 && perClick.containsKey("ANY")) {
                out.set(p + ".actions", perClick.get("ANY"));
            } else {

                boolean onlyLR = perClick.size() == 2
                        && perClick.containsKey("LEFT") && perClick.containsKey("RIGHT")
                        && mapsListEqual(perClick.get("LEFT"), perClick.get("RIGHT"));
                if (onlyLR) {
                    out.set(p + ".actions", perClick.get("LEFT"));
                } else {
                    for (Map.Entry<String, List<Map<String, Object>>> e : perClick.entrySet()) {
                        out.set(p + ".click_actions." + e.getKey(), e.getValue());
                    }
                }
            }
        }
    }

    private static void writeViewRequirements(ConfigurationSection src, YamlConfiguration out, String p) {
        List<Map<String, Object>> converted = new ArrayList<>();

        String perm = src.getString("permission");
        if (perm != null && !perm.isBlank()) {
