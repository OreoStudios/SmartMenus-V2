package com.oreo.gui;

import com.oreo.action.Action;
import com.oreo.condition.Condition;
import com.oreo.util.CooldownConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiItem {

    /** Which underlying item the button renders (vanilla, ItemsAdder, Nexo, …). */
    public record ItemSpec(String type, String id, Integer customModelData) {
    }

    /** Chat-input button configuration. */
    public record ChatInput(String type, String promptMessage, String cancelWord) {
    }

    /** Cosmetic options applied to the rendered item. */
    public record Visuals(boolean glow, List<String> itemFlags, Map<String, Integer> enchantments) {
    }

    /** How often the button re-renders itself. */
    public record AutoUpdate(boolean enabled, int interval) {
    }

    /** Behaviour for input/place slots that accept a player's item. */
    public record Placement(boolean takeItem, boolean giveItem,
                            List<String> onPlaceCommands, List<Action> onPlaceActions,
                            boolean removeOnPlace) {
    }

    /** Legacy Vault shorthand ({@code price}/{@code requirement}); prefer conditions/actions for new menus. */
    public record LegacyPurchase(double price, String requirement) {
    }

    private final int slot;
    private final String material;
    private final String name;
    private final List<String> lore;
    private final List<String> commands;
    private final boolean closeOnClick;
    private final List<Condition> conditions;
    private final Map<String, List<Action>> clickActions;
    private final List<Condition> viewRequirements;
    private final GuiItem elseItem;
    private final ButtonType buttonType;
    private final boolean permanent;
    private final DynamicSource dynamicSource;
    private final int amount;
    private final CooldownConfig cooldown;

    private final ItemSpec item;
    private final ChatInput chatInput;
    private final Visuals visuals;
    private final AutoUpdate autoUpdate;
    private final Placement placement;
    private final LegacyPurchase purchase;

    private GuiItem(Builder b) {
        this.slot = b.slot;
        this.material = b.material;
        this.name = b.name;
        this.lore = b.lore == null ? Collections.emptyList() : b.lore;
        this.commands = b.commands == null ? Collections.emptyList() : b.commands;
        this.closeOnClick = b.closeOnClick;
        this.conditions = b.conditions == null ? Collections.emptyList() : b.conditions;
        this.clickActions = b.clickActions == null ? new HashMap<>() : b.clickActions;
        this.viewRequirements = b.viewRequirements == null ? Collections.emptyList() : b.viewRequirements;
        this.elseItem = b.elseItem;
        this.buttonType = b.buttonType == null ? ButtonType.NONE : b.buttonType;
        this.permanent = b.permanent;
        this.dynamicSource = b.dynamicSource == null ? DynamicSource.NONE : b.dynamicSource;
        this.amount = Math.max(1, b.amount);
        this.cooldown = b.cooldown;

        this.item = new ItemSpec(b.itemType == null ? "vanilla" : b.itemType, b.itemId, b.customModelData);
        this.chatInput = new ChatInput(
                b.chatInputType == null ? "TEXT" : b.chatInputType,
                b.chatPromptMessage == null ? "" : b.chatPromptMessage,
                b.chatCancelWord == null ? "cancel" : b.chatCancelWord);
        this.visuals = new Visuals(
                b.glow,
                b.itemFlags == null ? Collections.emptyList() : b.itemFlags,
                b.enchantments == null ? Collections.emptyMap() : b.enchantments);
        this.autoUpdate = new AutoUpdate(b.autoUpdate, b.updateInterval);
        this.placement = new Placement(
                b.takeItem, b.giveItem,
                b.onPlaceCommands == null ? Collections.emptyList() : b.onPlaceCommands,
                b.onPlaceActions == null ? Collections.emptyList() : b.onPlaceActions,
                b.removeOnPlace);
        this.purchase = new LegacyPurchase(b.price, b.requirement);
    }

    public static final class Builder {
        private int slot = 0;
        private String material = "STONE";
        private String name = "";
        private List<String> lore = Collections.emptyList();
        private List<String> commands = Collections.emptyList();
        private boolean closeOnClick = false;
        private double price = 0;
        private String requirement = "";
        private List<Condition> conditions = Collections.emptyList();
        private String itemType = "vanilla";
        private String itemId = null;
        private Integer customModelData = null;
        private Map<String, List<Action>> clickActions = new HashMap<>();
        private List<Condition> viewRequirements = Collections.emptyList();
        private GuiItem elseItem = null;
        private ButtonType buttonType = ButtonType.NONE;
        private String chatInputType = "TEXT";
        private String chatPromptMessage = "";
        private String chatCancelWord = "cancel";
        private boolean glow = false;
        private List<String> itemFlags = Collections.emptyList();
        private Map<String, Integer> enchantments = Collections.emptyMap();
        private boolean autoUpdate = false;
        private int updateInterval = 20;
        private boolean permanent = false;
        private DynamicSource dynamicSource = DynamicSource.NONE;
        private boolean takeItem = false;
        private boolean giveItem = false;
        private int amount = 1;
        private List<String> onPlaceCommands = Collections.emptyList();
        private List<Action> onPlaceActions = Collections.emptyList();
        private boolean removeOnPlace = true;
        private CooldownConfig cooldown = null;

        public Builder slot(int slot) { this.slot = slot; return this; }
        public Builder material(String material) { this.material = material; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder lore(List<String> lore) { this.lore = lore; return this; }
        public Builder commands(List<String> commands) { this.commands = commands; return this; }
        public Builder closeOnClick(boolean v) { this.closeOnClick = v; return this; }
        public Builder price(double price) { this.price = price; return this; }
