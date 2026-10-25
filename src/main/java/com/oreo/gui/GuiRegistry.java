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
            bottomInventoryMode = BottomInventoryMode.parse(modeStr, id, plugin.getLogger());
        }

        Map<Integer, GuiItem> items = new LinkedHashMap<>();
        ConfigurationSection itemsSection = guiSection.getConfigurationSection("items");

        if (guiSection.isList("patterns") && itemsSection != null && plugin.getPatternRegistry() != null) {
            for (Object patternObj : guiSection.getList("patterns")) {
                if (patternObj instanceof String) {
                    plugin.getPatternRegistry().applyPattern((String) patternObj, itemsSection, Collections.emptyMap());
                } else if (patternObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> patternMap = (Map<String, Object>) patternObj;
                    String patId = patternMap.containsKey("id") ? patternMap.get("id").toString() : null;
                    if (patId == null) continue;
                    Map<String, String> vars = new HashMap<>();
                    Object varsObj = patternMap.get("vars");
                    if (varsObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> varsMap = (Map<String, Object>) varsObj;
                        for (Map.Entry<String, Object> e : varsMap.entrySet()) {
                            vars.put(e.getKey(), e.getValue() != null ? e.getValue().toString() : "");
                        }
                    }
                    plugin.getPatternRegistry().applyPattern(patId, itemsSection, vars);
                }
            }
        }

        if (itemsSection != null) {
            for (String slotKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotKey);
                if (itemSection == null) continue;

                List<Integer> slots = parseSlots(slotKey, itemSection);
                if (slots.isEmpty()) {
                    plugin.getLogger().warning("Invalid slot '" + slotKey + "' in GUI " + id);
                    continue;
                }

                for (int slot : slots) {
                    GuiItem guiItem = itemParser.loadItem(id, slot, itemSection);
                    if (guiItem != null) {
                        items.put(slot, guiItem);
                    }
                }
            }
        }

        Map<Integer, GuiItem> bottomItems = new LinkedHashMap<>();
        ConfigurationSection bottomSection = guiSection.getConfigurationSection("bottom_items");
        if (bottomSection == null) bottomSection = guiSection.getConfigurationSection("bottom-items");
        if (bottomSection != null) {
            for (String slotKey : bottomSection.getKeys(false)) {
                ConfigurationSection itemSection = bottomSection.getConfigurationSection(slotKey);
                if (itemSection == null) continue;

                List<Integer> slots = parseSlots(slotKey, itemSection);
                for (int slot : slots) {
                    if (slot < 0 || slot > 35) {
                        plugin.getLogger().warning("bottom_items slot " + slot + " out of range (0-35) in GUI " + id);
                        continue;
                    }
                    GuiItem guiItem = itemParser.loadItem(id, slot, itemSection);
                    if (guiItem != null) bottomItems.put(slot, guiItem);
                }
            }
        }

        BedrockFormDefinition bedrockDefinition = null;
        boolean bedrockAutoConvert = guiSection.getBoolean("auto-convert",
                guiSection.getBoolean("auto_convert", false));

        ConfigurationSection bedrockSection = guiSection.getConfigurationSection("bedrock");
        if (bedrockSection != null) {
            bedrockDefinition = bedrockParser.load(id, bedrockSection);
        }

        if (bedrockDefinition != null && bedrockAutoConvert) {
            plugin.getLogger().info("  GUI '" + id + "': explicit bedrock: block takes priority over auto-convert.");
            bedrockAutoConvert = false;
        }

        List<Condition> openRequirements = conditionListParser.parseFromKey(guiSection, "open-requirements");
        List<Action> openActions = ActionFactory.parseActions(guiSection.getList("open-actions"));
        List<Action> closeActions = ActionFactory.parseActions(guiSection.getList("close-actions"));

        String openSound = null;
        float openSoundVolume = 1.0f;
        float openSoundPitch = 1.0f;
        if (guiSection.isSet("open_sound") || guiSection.isSet("open-sound")) {
            String soundKey = guiSection.isSet("open_sound") ? "open_sound" : "open-sound";
            if (guiSection.isConfigurationSection(soundKey)) {
                ConfigurationSection soundSection = guiSection.getConfigurationSection(soundKey);
                openSound = soundSection.getString("sound");
                openSoundVolume = (float) soundSection.getDouble("volume", 1.0);
                openSoundPitch = (float) soundSection.getDouble("pitch", 1.0);
            } else {
                openSound = guiSection.getString(soundKey);
            }
        }

        CooldownConfig openCooldown = null;
        ConfigurationSection cooldownSection = guiSection.getConfigurationSection("cooldown");
        if (cooldownSection == null) cooldownSection = guiSection.getConfigurationSection("open-cooldown");
        if (cooldownSection != null) {
            openCooldown = CooldownConfig.parse(cooldownSection, "gui:" + id);
        }

        return new GuiDefinition.Builder(id, title)
                .rows(rows)
                .items(items)
                .commands(guiCommands)
                .npcId(npcId)
                .fillItem(fillItem)
                .inventoryType(inventoryType)
                .commandArgs(commandArgs)
                .useBottomInventory(useBottomInventory)
                .bottomItems(bottomItems)
                .bedrockDefinition(bedrockDefinition)
                .bedrockAutoConvert(bedrockAutoConvert)
                .openRequirements(openRequirements)
                .openActions(openActions)
                .closeActions(closeActions)
                .openSound(openSound)
                .openSoundVolume(openSoundVolume)
                .openSoundPitch(openSoundPitch)
                .bottomInventoryMode(bottomInventoryMode)
                .openCooldown(openCooldown)
                .build();
    }

    public GuiDefinition getGui(String id) { return definitions.get(id); }

    public Set<String> getGuiIds() { return Collections.unmodifiableSet(definitions.keySet()); }

    public File getGuiFile(String id) { return guiFiles.get(id); }

    private List<Integer> parseSlots(String slotKey, ConfigurationSection itemSection) {
        List<Integer> slots = new ArrayList<>();

        if (itemSection.isSet("slot")) {
            addSlotValue(slots, itemSection.get("slot"));
        } else if (itemSection.isSet("slots")) {
            Object rawSlots = itemSection.get("slots");
            if (rawSlots instanceof List<?>) {
                for (Object value : (List<?>) rawSlots) {
                    addSlotValue(slots, value);
                }
            } else {
                addSlotValue(slots, rawSlots);
            }
        } else {
            addSlotValue(slots, slotKey);
        }

        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        for (int slot : slots) {
            if (slot >= 0) {
                unique.add(slot);
            }
        }
        return new ArrayList<>(unique);
    }

    private void addSlotValue(List<Integer> slots, Object value) {
        slots.addAll(SlotParser.parse(value));
    }
}
