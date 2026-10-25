package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.action.Action;
import com.oreo.action.ActionFactory;
import com.oreo.bedrock.BedrockFormDefinition;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionFactory;
import com.oreo.util.CooldownConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads GUI definitions from the {@code guis/} folder. Item and Bedrock-form parsing are delegated to
 * {@link GuiItemParser} and {@link BedrockFormParser}; this class handles file discovery and assembles
 * the top-level {@link GuiDefinition}.
 */
public class GuiRegistry {

    private final SmartMenus plugin;
    private final Map<String, GuiDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, File> guiFiles = new HashMap<>();
    private final Map<Integer, String> npcToGuiMap = new HashMap<>();

    private final ConditionListParser conditionListParser;
    private final GuiItemParser itemParser;
    private final BedrockFormParser bedrockParser;

    public GuiRegistry(SmartMenus plugin) {
        this.plugin = plugin;
        ConditionFactory conditionFactory = new ConditionFactory(plugin);
        this.conditionListParser = new ConditionListParser(plugin, conditionFactory);
        this.itemParser = new GuiItemParser(plugin, conditionListParser);
        this.bedrockParser = new BedrockFormParser(plugin, conditionListParser);
    }

    public void cacheNpcBinding(int npcId, String guiId) { npcToGuiMap.put(npcId, guiId); }
    public String getGuiByNpc(int npcId) { return npcToGuiMap.get(npcId); }
    public void clearNpcBindings() { npcToGuiMap.clear(); }

    public void reload() {
        definitions.clear();
        guiFiles.clear();
        clearNpcBindings();

        File guiFolder = new File(plugin.getDataFolder(), "guis");
        if (!guiFolder.exists() && !guiFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create guis folder: " + guiFolder.getPath());
            return;
        }

        if (plugin.getPatternRegistry() != null) {
            plugin.getPatternRegistry().reload();
        }

        List<File> files = new ArrayList<>();
        collectYamlFiles(guiFolder, files);
        if (files.isEmpty()) {
            plugin.getLogger().warning("No GUI files found in guis/ folder.");
            return;
        }

        for (File file : files) {
            loadGuiFile(file);
        }

        plugin.getLogger().info("Loaded " + definitions.size() + " GUI definition(s).");
    }

    private void loadGuiFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection guisSection = config.getConfigurationSection("guis");
        if (guisSection == null) {
            guisSection = config;
        }

        Set<String> guiIds = new LinkedHashSet<>();
        for (String id : guisSection.getKeys(false)) {
            ConfigurationSection section = guisSection.getConfigurationSection(id);
            if (isGuiSection(section)) {
                guiIds.add(id);
            }
        }

        if (guiIds.isEmpty()) {
            plugin.getLogger().warning("No GUI definitions found in " + file.getName() + ".");
            return;
        }

        for (String id : guiIds) {
            try {
                GuiDefinition definition = loadGuiDefinition(id, guisSection.getConfigurationSection(id));
                if (definition != null) {
                    definitions.put(id, definition);
                    guiFiles.put(id, file);
                    if (definition.hasNpcBinding()) {
                        cacheNpcBinding(definition.getNpcId(), id);
                        plugin.getLogger().info("GUI '" + id + "' bound to NPC ID: " + definition.getNpcId());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load GUI '" + id + "': " + e.getMessage(), e);
            }
        }
    }

    private void collectYamlFiles(File folder, List<File> files) {
        File[] children = folder.listFiles();
        if (children == null) return;

        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            if (child.isDirectory()) {
                collectYamlFiles(child, files);
            } else if (child.getName().endsWith(".yml") || child.getName().endsWith(".yaml")) {
                files.add(child);
            }
        }
    }

    private boolean isGuiSection(ConfigurationSection section) {
        return section != null
                && (section.isSet("title")
                || section.isSet("rows")
                || section.isSet("items")
                || section.isSet("commands")
                || section.isSet("inventory-type")
                || section.isSet("inventory_type"));
    }

    private GuiDefinition loadGuiDefinition(String id, ConfigurationSection guiSection) {
        if (guiSection == null) return null;

        String title = guiSection.getString("title", id);
        int rows = Math.min(6, Math.max(1, guiSection.getInt("rows", 1)));
        List<String> guiCommands = guiSection.getStringList("commands");

        Integer npcId = guiSection.isSet("npc_id") ? guiSection.getInt("npc_id") : null;

        InventoryType inventoryType = null;
        if (guiSection.isSet("inventory-type") || guiSection.isSet("inventory_type")) {
            try {
                String typeName = guiSection.getString("inventory-type", guiSection.getString("inventory_type", "CHEST"));
                inventoryType = InventoryType.valueOf(typeName.toUpperCase(Locale.ENGLISH).replace('-', '_'));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown inventory-type for GUI " + id + ": " + guiSection.getString("inventory-type", guiSection.getString("inventory_type")));
            }
        }

        GuiItem fillItem = null;
        if (guiSection.isSet("fill")) {
            ConfigurationSection fillSection = guiSection.getConfigurationSection("fill");
            if (fillSection != null) {
                fillItem = itemParser.loadElseItem(id, fillSection);
            }
        }

        List<GuiDefinition.ArgDefinition> commandArgs = new ArrayList<>();
        if (guiSection.isList("args")) {
            for (Object argObj : guiSection.getList("args")) {
                if (!(argObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> argMap = (Map<String, Object>) argObj;
                String argName = argMap.containsKey("name") ? argMap.get("name").toString() : null;
                if (argName == null) continue;
                String argType = argMap.containsKey("type") ? argMap.get("type").toString() : "text";
                boolean required = argMap.containsKey("required") && Boolean.parseBoolean(argMap.get("required").toString());
                String defaultValue = argMap.containsKey("default") ? argMap.get("default").toString() : null;
                commandArgs.add(new GuiDefinition.ArgDefinition(argName, argType, required, defaultValue));
            }
        }

        boolean useBottomInventory = guiSection.getBoolean("use_bottom_inventory",
                guiSection.getBoolean("use-bottom-inventory", false));

        BottomInventoryMode bottomInventoryMode = BottomInventoryMode.DEFAULT;
        if (useBottomInventory && guiSection.isSet("bottom_inventory_mode")) {
            String modeStr = guiSection.getString("bottom_inventory_mode", "DEFAULT");
