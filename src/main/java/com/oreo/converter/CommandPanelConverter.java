package com.oreo.converter;

import com.oreo.util.Ids;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CommandPanelConverter {

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

        String title = src.getString("title", guiId);
        out.set(base + ".title", ConverterUtil.convertColors(title));

        int rows = src.getInt("rows", 3);
        out.set(base + ".rows", rows);

        List<String> commands = new ArrayList<>();
        String mainCmd = src.getString("command");
        if (mainCmd != null && !mainCmd.isBlank()) commands.add(mainCmd.trim());
        for (String a : src.getStringList("commands")) {
            String t = a.trim(); if (!t.isBlank() && !commands.contains(t)) commands.add(t);
        }
        for (String a : src.getStringList("aliases")) {
            String t = a.trim(); if (!t.isBlank() && !commands.contains(t)) commands.add(t);
        }
        if (!commands.isEmpty()) out.set(base + ".commands", commands);

        ConfigurationSection itemsSec = src.getConfigurationSection("items");
        if (itemsSec == null) throw new IOException("Missing 'items' section");

        ConfigurationSection layoutSec = src.getConfigurationSection("layout");
        if (layoutSec == null) throw new IOException("Missing 'layout' section");

        List<String> fillKeys = layoutSec.getStringList("fill");
        if (!fillKeys.isEmpty()) {
            String fillKey = fillKeys.get(0).trim();
            String fillMat = resolveItemMaterial(itemsSec, fillKey);
            if (fillMat != null && !fillMat.equalsIgnoreCase("AIR")) {
                out.set(base + ".fill.material", fillMat);
            }
        }

        for (String slotStr : layoutSec.getKeys(false)) {
            if (slotStr.equalsIgnoreCase("fill")) continue;
            int slot;
            try { slot = Integer.parseInt(slotStr); } catch (NumberFormatException e) { continue; }

            List<String> itemKeys = layoutSec.getStringList(slotStr);
            if (itemKeys.isEmpty()) continue;

            String itemKey = itemKeys.get(0).trim();
            if (itemKey.isBlank()) continue;

            ConfigurationSection itemSec = itemsSec.getConfigurationSection(itemKey);
            if (itemSec == null) {

                String mat = itemsSec.getString(itemKey + ".material");
                if (mat != null && !mat.equalsIgnoreCase("AIR")) {
                    out.set(base + ".items." + slot + ".material", mat.toUpperCase());
                }
                continue;
            }

            convertItem(itemSec, out, base, slot);
        }

        if (!outputDir.exists()) outputDir.mkdirs();
        File outputFile = new File(outputDir, guiId + ".yml");
        out.save(outputFile);
        return outputFile;
    }

    private static void convertItem(ConfigurationSection src, YamlConfiguration out,
                                    String base, int slot) {
        String p = base + ".items." + slot;

        String material = src.getString("material", "STONE").toUpperCase();
        if (material.equalsIgnoreCase("AIR") || material.equalsIgnoreCase("NONE")) return;
        out.set(p + ".material", material);

        String name = src.getString("name");
        if (name != null && !name.isBlank()) out.set(p + ".name", ConverterUtil.convertColors(name));

        List<String> lore = src.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> cl = new ArrayList<>();
            for (String line : lore) cl.add(ConverterUtil.convertColors(line));
            out.set(p + ".lore", cl);
        }

        List<?> enchanted = src.getList("enchanted");
        if (enchanted != null && !enchanted.isEmpty()) {
            Object v = enchanted.get(0);
            if (Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(String.valueOf(v))) {
                out.set(p + ".glow", true);
            }
        }

        int stack = src.getInt("stack", 1);
        if (stack > 1) out.set(p + ".amount", stack);

        String itemModel = src.getString("item-model");
        if (itemModel != null && !itemModel.isBlank()) {
            try { out.set(p + ".custom_model_data", Integer.parseInt(itemModel.trim())); }
            catch (NumberFormatException ignored) {}
        }

        List<String> itemTypeList = src.getStringList("itemType");
        List<String> flags = new ArrayList<>();
        for (String t : itemTypeList) {
            switch (t.toLowerCase(Locale.ROOT)) {
                case "noattributes", "no_attributes" -> flags.add("HIDE_ATTRIBUTES");
                case "noenchants", "no_enchants"     -> flags.add("HIDE_ENCHANTS");
                case "nounbreakable"                 -> flags.add("HIDE_UNBREAKABLE");
                case "nopotioneffects"               -> flags.add("HIDE_ADDITIONAL_TOOLTIP");
            }
        }
        if (!flags.isEmpty()) out.set(p + ".item_flags", flags);

        ConfigurationSection actionsSec = src.getConfigurationSection("actions");
        boolean closeOnClick = false;
        List<Map<String, Object>> actions = new ArrayList<>();
        List<Map<String, Object>> conditions = new ArrayList<>();

        if (actionsSec != null) {

            for (String cmd : actionsSec.getStringList("commands")) {
                cmd = cmd.trim();
                if (cmd.isEmpty()) continue;

                if (cmd.equalsIgnoreCase("[close]")) {
                    closeOnClick = true;
                } else if (cmd.equalsIgnoreCase("[refresh]")) {
                    out.set(p + ".update", true);
                } else if (cmd.toLowerCase(Locale.ROOT).startsWith("[delay]")) {

                } else {
                    Map<String, Object> action = parseAction(cmd);
                    if (action != null) actions.add(action);
                }
            }

            for (String req : actionsSec.getStringList("requirements")) {
                req = req.trim();
                if (req.isEmpty()) continue;
                Map<String, Object> cond = parseRequirement(req);
                if (cond != null) conditions.add(cond);
            }

        }

        if (closeOnClick) out.set(p + ".close", true);
        if (!actions.isEmpty()) out.set(p + ".actions", actions);
        if (!conditions.isEmpty()) out.set(p + ".conditions", conditions);
    }

    private static Map<String, Object> parseAction(String cmd) {
        Map<String, Object> action = new LinkedHashMap<>();

        if (cmd.startsWith("[chat] ") || cmd.startsWith("[player] ")) {
            String c = cmd.startsWith("[chat] ") ? cmd.substring(7) : cmd.substring(9);
            c = c.trim();
            if (c.startsWith("/")) c = c.substring(1);
            action.put("type", "PLAYER_COMMAND");
            action.put("command", c);
        } else if (cmd.startsWith("[console] ")) {
            action.put("type", "CONSOLE_COMMAND");
            action.put("command", cmd.substring(10).trim());
        } else if (cmd.startsWith("[msg] ")) {
            action.put("type", "PLAYER_MESSAGE");
            action.put("message", ConverterUtil.convertColors(cmd.substring(6).trim()));
        } else if (cmd.startsWith("[broadcast] ")) {
            action.put("type", "BROADCAST");
            action.put("message", ConverterUtil.convertColors(cmd.substring(12).trim()));
        } else if (cmd.startsWith("[title] ")) {
            String titleStr = cmd.substring(8).trim();
            String[] parts = titleStr.split(";", 2);
            action.put("type", "TITLE");
            action.put("title", ConverterUtil.convertColors(parts[0]));
            if (parts.length > 1) action.put("subtitle", ConverterUtil.convertColors(parts[1]));
        } else if (cmd.startsWith("[sound] ")) {
            action.put("type", "SOUND");
            action.put("sound", cmd.substring(8).trim());
        } else if (cmd.startsWith("[open] ")) {
            action.put("type", "OPEN_GUI");
            action.put("gui", cmd.substring(7).trim());
        } else {
            return null;
        }

        return action;
    }

    private static Map<String, Object> parseRequirement(String req) {
        Map<String, Object> cond = new LinkedHashMap<>();

        if (req.startsWith("[permission] ")) {
            cond.put("type", "PERMISSION");
            cond.put("permission", req.substring(13).trim());
            return cond;
        }

        if (req.startsWith("[money] ") || req.startsWith("[vault] ")) {
            cond.put("type", "VAULT_MONEY");
            String amtStr = req.substring(8).trim();
            try { cond.put("amount", Double.parseDouble(amtStr)); }
            catch (NumberFormatException e) { cond.put("amount", amtStr); }
            return cond;
        }

        if (req.startsWith("[exp] ") || req.startsWith("[xp] ")) {
            cond.put("type", "XP_LEVEL");
            String amtStr = req.startsWith("[exp] ") ? req.substring(6).trim() : req.substring(5).trim();
            try { cond.put("amount", Integer.parseInt(amtStr)); }
            catch (NumberFormatException e) { cond.put("amount", amtStr); }
            return cond;
        }

        if (req.startsWith("[item] ")) {
            String params = req.substring(7);
            String material = null;
            Integer customModel = null;
            Integer amount = null;
            for (String part : params.split("\\s+")) {
                if (part.startsWith("material="))
                    material = part.substring(9).toUpperCase();
                else if (part.startsWith("custom="))
                    try { customModel = Integer.parseInt(part.substring(7)); } catch (NumberFormatException ignored) {}
                else if (part.startsWith("amount="))
                    try { amount = Integer.parseInt(part.substring(7)); } catch (NumberFormatException ignored) {}
            }
            if (customModel != null) {
                cond.put("type", "ITEM_CUSTOM_MODEL");
                if (material != null) cond.put("material", material);
                cond.put("custom_model_data", customModel);
            } else {
                cond.put("type", "ITEM");
                if (material != null) cond.put("material", material);
            }
            if (amount != null) cond.put("amount", amount);
            return cond;
        }

        if (req.startsWith("[luckperms] ") || req.startsWith("[group] ")) {
            cond.put("type", "LUCKPERMS_GROUP");
            cond.put("group", req.substring(req.indexOf(']') + 2).trim());
            return cond;
        }

        return null;
    }

    private static String resolveItemMaterial(ConfigurationSection itemsSec, String key) {
        ConfigurationSection sec = itemsSec.getConfigurationSection(key);
        if (sec != null) return sec.getString("material", "AIR").toUpperCase();

        String mat = itemsSec.getString(key + ".material");
        return mat != null ? mat.toUpperCase() : null;
    }
}
