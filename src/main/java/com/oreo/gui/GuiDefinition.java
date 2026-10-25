package com.oreo.gui;

import com.oreo.SmartMenus;
import com.oreo.action.Action;
import com.oreo.bedrock.BedrockFormDefinition;
import com.oreo.condition.Condition;
import com.oreo.util.ColorUtil;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GuiDefinition {

    public static class ArgDefinition {
        public final String name;
        public final String type;
        public final boolean required;
        public final String defaultValue;

        public ArgDefinition(String name, String type, boolean required, String defaultValue) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.defaultValue = defaultValue;
        }
    }

    private final String id;
    private final String title;
    private final int rows;
    private final Map<Integer, GuiItem> items;
    private final List<String> commands;
    private final Integer npcId;
    private final GuiItem fillItem;
    private final InventoryType inventoryType;
    private final List<ArgDefinition> commandArgs;
    private final boolean useBottomInventory;
    private final Map<Integer, GuiItem> bottomItems;
    private final BedrockFormDefinition bedrockDefinition;
    private final boolean bedrockAutoConvert;
    private final List<Condition> openRequirements;
    private final List<Action> openActions;
    private final List<Action> closeActions;
    private final String openSound;
    private final float openSoundVolume;
    private final float openSoundPitch;
    private final BottomInventoryMode bottomInventoryMode;
    private final com.oreo.util.CooldownConfig openCooldown;

    private GuiDefinition(String id, String title, int rows, Map<Integer, GuiItem> items,
                          List<String> commands, Integer npcId,
                          GuiItem fillItem, InventoryType inventoryType,
                          List<ArgDefinition> commandArgs,
                          boolean useBottomInventory, Map<Integer, GuiItem> bottomItems,
                          BedrockFormDefinition bedrockDefinition, boolean bedrockAutoConvert,
                          List<Condition> openRequirements, List<Action> openActions, List<Action> closeActions,
                          String openSound, float openSoundVolume, float openSoundPitch,
                          BottomInventoryMode bottomInventoryMode, com.oreo.util.CooldownConfig openCooldown) {
        this.id = id;
        this.title = title;
        this.rows = rows;
        this.items = items != null ? items : Collections.emptyMap();
        this.commands = commands != null ? commands : Collections.emptyList();
        this.npcId = npcId;
        this.fillItem = fillItem;
        this.inventoryType = inventoryType != null ? inventoryType : InventoryType.CHEST;
        this.commandArgs = commandArgs != null ? commandArgs : Collections.emptyList();
        this.useBottomInventory = useBottomInventory;
        this.bottomItems = bottomItems != null ? bottomItems : Collections.emptyMap();
        this.bedrockDefinition = bedrockDefinition;
        this.bedrockAutoConvert = bedrockAutoConvert;
        this.openRequirements = openRequirements != null ? openRequirements : Collections.emptyList();
        this.openActions = openActions != null ? openActions : Collections.emptyList();
        this.closeActions = closeActions != null ? closeActions : Collections.emptyList();
        this.openSound = openSound;
        this.openSoundVolume = openSoundVolume;
        this.openSoundPitch = openSoundPitch;
        this.bottomInventoryMode = bottomInventoryMode != null ? bottomInventoryMode : BottomInventoryMode.DEFAULT;
        this.openCooldown = openCooldown;
    }

    public static final class Builder {
        private final String id;
        private final String title;
        private int rows = 1;
        private Map<Integer, GuiItem> items = Collections.emptyMap();
        private List<String> commands = Collections.emptyList();
        private Integer npcId = null;
        private GuiItem fillItem = null;
        private InventoryType inventoryType = InventoryType.CHEST;
        private List<ArgDefinition> commandArgs = Collections.emptyList();
        private boolean useBottomInventory = false;
        private Map<Integer, GuiItem> bottomItems = Collections.emptyMap();
        private BedrockFormDefinition bedrockDefinition = null;
        private boolean bedrockAutoConvert = false;
        private List<Condition> openRequirements = Collections.emptyList();
        private List<Action> openActions = Collections.emptyList();
        private List<Action> closeActions = Collections.emptyList();
        private String openSound = null;
        private float openSoundVolume = 1.0f;
        private float openSoundPitch = 1.0f;
        private BottomInventoryMode bottomInventoryMode = BottomInventoryMode.DEFAULT;
        private com.oreo.util.CooldownConfig openCooldown = null;

        public Builder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public Builder rows(int rows) { this.rows = rows; return this; }
        public Builder items(Map<Integer, GuiItem> items) { this.items = items; return this; }
        public Builder commands(List<String> commands) { this.commands = commands; return this; }
        public Builder npcId(Integer npcId) { this.npcId = npcId; return this; }
        public Builder fillItem(GuiItem fillItem) { this.fillItem = fillItem; return this; }
