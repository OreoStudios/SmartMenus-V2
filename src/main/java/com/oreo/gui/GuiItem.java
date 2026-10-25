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
        public Builder requirement(String req) { this.requirement = req; return this; }
        public Builder conditions(List<Condition> conditions) { this.conditions = conditions; return this; }
        public Builder itemType(String itemType) { this.itemType = itemType; return this; }
        public Builder itemId(String itemId) { this.itemId = itemId; return this; }
        public Builder customModelData(Integer cmd) { this.customModelData = cmd; return this; }
        public Builder clickActions(Map<String, List<Action>> clickActions) { this.clickActions = clickActions; return this; }
        public Builder viewRequirements(List<Condition> viewReqs) { this.viewRequirements = viewReqs; return this; }
        public Builder elseItem(GuiItem elseItem) { this.elseItem = elseItem; return this; }
        public Builder buttonType(ButtonType buttonType) { this.buttonType = buttonType; return this; }
        public Builder chatInputType(String chatInputType) { this.chatInputType = chatInputType; return this; }
        public Builder chatPromptMessage(String chatPromptMessage) { this.chatPromptMessage = chatPromptMessage; return this; }
        public Builder chatCancelWord(String chatCancelWord) { this.chatCancelWord = chatCancelWord; return this; }
        public Builder glow(boolean glow) { this.glow = glow; return this; }
        public Builder itemFlags(List<String> itemFlags) { this.itemFlags = itemFlags; return this; }
        public Builder enchantments(Map<String, Integer> enchantments) { this.enchantments = enchantments; return this; }
        public Builder autoUpdate(boolean autoUpdate) { this.autoUpdate = autoUpdate; return this; }
        public Builder updateInterval(int updateInterval) { this.updateInterval = updateInterval; return this; }
        public Builder permanent(boolean permanent) { this.permanent = permanent; return this; }
        public Builder dynamicSource(DynamicSource dynamicSource) { this.dynamicSource = dynamicSource; return this; }
        public Builder takeItem(boolean takeItem) { this.takeItem = takeItem; return this; }
        public Builder giveItem(boolean giveItem) { this.giveItem = giveItem; return this; }
        public Builder amount(int amount) { this.amount = amount; return this; }
        public Builder onPlaceCommands(List<String> onPlaceCommands) { this.onPlaceCommands = onPlaceCommands; return this; }
        public Builder onPlaceActions(List<Action> onPlaceActions) { this.onPlaceActions = onPlaceActions; return this; }
        public Builder removeOnPlace(boolean removeOnPlace) { this.removeOnPlace = removeOnPlace; return this; }
        public Builder cooldown(CooldownConfig cooldown) { this.cooldown = cooldown; return this; }

        public GuiItem build() {
            return new GuiItem(this);
        }
    }

    public int getSlot() { return slot; }
    public String getMaterial() { return material; }
    public String getName() { return name; }
    public List<String> getLore() { return Collections.unmodifiableList(lore); }
    public List<String> getCommands() { return Collections.unmodifiableList(commands); }
    public boolean isCloseOnClick() { return closeOnClick; }

    public double getPrice() { return purchase.price(); }
    public String getRequirement() { return purchase.requirement(); }
    public boolean hasPrice() { return purchase.price() > 0; }
    public boolean hasRequirement() { return purchase.requirement() != null && !purchase.requirement().isEmpty(); }

    public List<Condition> getConditions() { return Collections.unmodifiableList(conditions); }
    public boolean hasConditions() { return !conditions.isEmpty(); }
    public String getItemType() { return item.type(); }
    public String getItemId() { return item.id(); }
    public Integer getCustomModelData() { return item.customModelData(); }

    public boolean isCustomItem() {
        return "itemsadder".equalsIgnoreCase(item.type()) || "nexo".equalsIgnoreCase(item.type());
    }

    public boolean isVanillaItem() { return "vanilla".equalsIgnoreCase(item.type()); }

    public Map<String, List<Action>> getClickActions() { return Collections.unmodifiableMap(clickActions); }

    public List<Action> getActionsForClick(String clickKey) {
        if (clickActions.containsKey(clickKey)) return clickActions.get(clickKey);
        if (clickActions.containsKey("ANY")) return clickActions.get("ANY");
        if (clickActions.containsKey("LEGACY")) return clickActions.get("LEGACY");
        return Collections.emptyList();
    }

    public List<Condition> getViewRequirements() { return Collections.unmodifiableList(viewRequirements); }
    public GuiItem getElseItem() { return elseItem; }

    public ButtonType getButtonType() { return buttonType; }
    public String getChatInputType() { return chatInput.type(); }
    public String getChatPromptMessage() { return chatInput.promptMessage(); }
    public String getChatCancelWord() { return chatInput.cancelWord(); }

    public boolean isGlow() { return visuals.glow(); }
    public List<String> getItemFlags() { return Collections.unmodifiableList(visuals.itemFlags()); }
    public Map<String, Integer> getEnchantments() { return Collections.unmodifiableMap(visuals.enchantments()); }

    public boolean isAutoUpdate() { return autoUpdate.enabled(); }
    public int getUpdateInterval() { return autoUpdate.interval(); }

    public boolean isPermanent() { return permanent; }
    public DynamicSource getDynamicSource() { return dynamicSource; }
    public boolean isTakeItem() { return placement.takeItem(); }
    public boolean isGiveItem() { return placement.giveItem(); }
    public int getAmount() { return amount; }

    public List<String> getOnPlaceCommands() { return Collections.unmodifiableList(placement.onPlaceCommands()); }
    public List<Action> getOnPlaceActions() { return Collections.unmodifiableList(placement.onPlaceActions()); }
    public boolean isRemoveOnPlace() { return placement.removeOnPlace(); }
    public CooldownConfig getCooldown() { return cooldown; }

    @Override
    public String toString() {
        return "GuiItem{slot=" + slot + ", material='" + material + "', itemType='" + item.type()
                + "', conditions=" + conditions.size() + ", buttonType=" + buttonType + '}';
    }
}
