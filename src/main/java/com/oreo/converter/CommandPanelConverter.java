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
