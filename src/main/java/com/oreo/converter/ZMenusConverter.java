package com.oreo.converter;

import com.oreo.util.Ids;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ZMenusConverter {

    public static File convert(File inputFile, File outputDir, String guiIdOverride) throws IOException {
        YamlConfiguration src = YamlConfiguration.loadConfiguration(inputFile);

        if (!src.isSet("name") && !src.isSet("size") && src.isSet("commands")) {
            throw new IOException(
                    "This looks like a ZMenus command-registration file (commands.yml), not a GUI definition. " +
                    "Convert the actual inventory YAML file instead (e.g. rtp.yml).");
        }

        String guiId;
        if (guiIdOverride != null && !guiIdOverride.isBlank()) {
            guiId = Ids.slugify(guiIdOverride);
        } else {
            String fn = inputFile.getName();
            guiId = Ids.slugify(fn.endsWith(".yml") ? fn.substring(0, fn.length() - 4) : fn);
        }

        YamlConfiguration out = new YamlConfiguration();
        String base = guiId;

        String title = src.getString("name", guiId);
        out.set(base + ".title", ConverterUtil.convertColors(title));

        int size = src.getInt("size", 54);
        int rows = Math.max(1, Math.min(6, size / 9));
        out.set(base + ".rows", rows);

        ConfigurationSection itemsSec = src.getConfigurationSection("items");
        if (itemsSec == null) throw new IOException("Missing 'items' section");

        String fillMaterial = null;
        Set<Integer> fillSlots = new HashSet<>();

        for (String key : itemsSec.getKeys(false)) {
            ConfigurationSection entry = itemsSec.getConfigurationSection(key);
            if (entry == null) continue;

            ConfigurationSection itemSec = entry.getConfigurationSection("item");
            String mat = itemSec != null
                    ? ConverterUtil.normalizeMaterial(itemSec.getString("material", "AIR"))
                    : "AIR";
            if (mat.equalsIgnoreCase("AIR")) continue;

            List<Integer> slots = resolveSlots(entry, size);
            if (slots.isEmpty()) continue;

            boolean hasActions = entry.isSet("actions");
            boolean hasName    = itemSec != null && itemSec.isSet("name");
            boolean hasLore    = itemSec != null && !itemSec.getStringList("lore").isEmpty();

            if (!hasActions && !hasName && !hasLore && slots.size() > 3 && mat.contains("GLASS_PANE")) {
                if (fillMaterial == null) {
                    fillMaterial = mat;
                    fillSlots.addAll(slots);
                    continue;
                }
            }

            convertEntry(entry, itemSec, out, base, slots);
        }

        if (fillMaterial != null) out.set(base + ".fill.material", fillMaterial);

        if (!outputDir.exists()) outputDir.mkdirs();
        File outputFile = new File(outputDir, guiId + ".yml");
        out.save(outputFile);
        return outputFile;
    }

    private static void convertEntry(ConfigurationSection entry, ConfigurationSection itemSec,
                                     YamlConfiguration out, String base, List<Integer> slots) {
        for (int slot : slots) {
            String p = base + ".items." + slot;

            if (itemSec != null) {
                String mat = ConverterUtil.normalizeMaterial(itemSec.getString("material", "STONE"));
                if (mat.equalsIgnoreCase("AIR")) continue;
                out.set(p + ".material", mat);

                String name = itemSec.getString("name");
                if (name != null && !name.isBlank()) out.set(p + ".name", ConverterUtil.convertColors(name));

                List<String> lore = itemSec.getStringList("lore");
                if (!lore.isEmpty()) out.set(p + ".lore", ConverterUtil.convertColors(lore));

                if (itemSec.getBoolean("glow", false)) out.set(p + ".glow", true);

                List<String> zmFlags = itemSec.getStringList("flags");
                if (!zmFlags.isEmpty()) {
                    List<String> smFlags = new ArrayList<>();
                    for (String f : zmFlags) {
                        switch (f.toUpperCase(Locale.ROOT)) {
                            case "HIDE_ATTRIBUTES"        -> smFlags.add("HIDE_ATTRIBUTES");
                            case "HIDE_ENCHANTS"          -> smFlags.add("HIDE_ENCHANTS");
                            case "HIDE_UNBREAKABLE"       -> smFlags.add("HIDE_UNBREAKABLE");
                            case "HIDE_ADDITIONAL_TOOLTIP", "HIDE_POTION_EFFECTS" -> smFlags.add("HIDE_ADDITIONAL_TOOLTIP");
                        }
                    }
                    if (!smFlags.isEmpty()) out.set(p + ".item_flags", smFlags);
                }
            }

            List<?> actionList = entry.getList("actions");
            if (actionList == null || actionList.isEmpty()) continue;

            boolean closeOnClick = false;
            List<Map<String, Object>> actions = new ArrayList<>();

            for (Object obj : actionList) {
                if (!(obj instanceof Map<?, ?> raw)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> actionMap = (Map<String, Object>) raw;

                String type = String.valueOf(actionMap.getOrDefault("type", "")).toLowerCase(Locale.ROOT).trim();

                switch (type) {
                    case "player command", "player_command" -> {
                        List<?> cmds = getCmdList(actionMap);
                        boolean inChat = Boolean.TRUE.equals(actionMap.get("command-in-chat"));
                        for (Object c : cmds) {
                            String cs = String.valueOf(c).trim();
                            if (cs.isEmpty()) continue;
                            if (cs.startsWith("/")) cs = cs.substring(1);
                            Map<String, Object> a = new LinkedHashMap<>();
                            a.put("type", "PLAYER_COMMAND");
                            a.put("command", cs);
                            actions.add(a);
                        }
                    }
                    case "console command", "console_command" -> {
                        for (Object c : getCmdList(actionMap)) {
                            String cs = String.valueOf(c).trim();
                            if (cs.isEmpty()) continue;
                            Map<String, Object> a = new LinkedHashMap<>();
                            a.put("type", "CONSOLE_COMMAND");
                            a.put("command", cs);
                            actions.add(a);
                        }
                    }
                    case "close" -> closeOnClick = true;
                    case "open inventory", "open_inventory" -> {
                        Map<String, Object> a = new LinkedHashMap<>();
                        a.put("type", "OPEN_GUI");
                        a.put("gui", String.valueOf(actionMap.getOrDefault("inventory", "")).trim());
                        actions.add(a);
                    }
                    case "sound" -> {
                        Map<String, Object> a = new LinkedHashMap<>();
                        a.put("type", "SOUND");
                        a.put("sound", String.valueOf(actionMap.getOrDefault("sound", "")).trim());
                        actions.add(a);
                    }
                    case "message" -> {
                        Map<String, Object> a = new LinkedHashMap<>();
                        a.put("type", "PLAYER_MESSAGE");
                        a.put("message", ConverterUtil.convertColors(String.valueOf(actionMap.getOrDefault("message", ""))));
                        actions.add(a);
                    }
                    case "broadcast" -> {
                        Map<String, Object> a = new LinkedHashMap<>();
                        a.put("type", "BROADCAST");
                        a.put("message", ConverterUtil.convertColors(String.valueOf(actionMap.getOrDefault("message", ""))));
                        actions.add(a);
                    }
                    case "connect", "server_connect" -> {
                        Map<String, Object> a = new LinkedHashMap<>();
                        a.put("type", "SERVER_CONNECT");
                        a.put("server", String.valueOf(actionMap.getOrDefault("server", "")).trim());
                        actions.add(a);
                    }

                }
            }

            if (closeOnClick) out.set(p + ".close", true);
            if (!actions.isEmpty()) out.set(p + ".actions", actions);
        }
    }

    private static List<Integer> resolveSlots(ConfigurationSection entry, int maxSize) {

        if (entry.isInt("slot")) return List.of(entry.getInt("slot"));
        if (entry.isString("slot")) {
            try { return List.of(Integer.parseInt(entry.getString("slot", "").trim())); }
            catch (NumberFormatException ignored) {}
        }

        List<?> slotList = entry.getList("slots");
        if (slotList != null && !slotList.isEmpty()) return ConverterUtil.expandSlots(slotList);
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static List<?> getCmdList(Map<String, Object> actionMap) {
        Object raw = actionMap.get("commands");
        if (raw instanceof List<?> list) return list;
        if (raw instanceof String s) return List.of(s);
        return Collections.emptyList();
    }
}
