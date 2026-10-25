package com.oreo.editor;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.util.UUID;

public class EditorSession {

    public enum Screen {
        GUI_LIST, LAYOUT, BOTTOM_LAYOUT, SLOT_EDITOR, PROPERTIES, ACTION_LIST, ACTION_TYPE,
        CONDITION_LIST, CONDITION_TYPE, VIEW_REQ_LIST,
        SWITCH_STATES, PAGINATION_ELEMENTS
    }

    public enum ChatMode {
        NONE, NEW_GUI_ID, GUI_TITLE, GUI_COMMANDS,
        ITEM_NAME, ITEM_LORE, ITEM_MATERIAL, ITEM_COMMANDS,
        ACTION_PARAM_0, ACTION_PARAM_1,
        CONDITION_PARAM, ITEM_ITEM_ID, ITEM_CUSTOM_MODEL_DATA, PROPERTIES_NPC_ID,
        ITEM_TO_PAGE, ITEM_SWITCH_KEY, ITEM_ON_PLACE,
        SWITCH_STATE_KEY, SWITCH_STATE_MATERIAL, SWITCH_STATE_NAME, SWITCH_STATE_COMMANDS,
        PAGINATION_SLOTS, PAGINATION_ELEMENT_TITLE, PAGINATION_ELEMENT_DESC
    }

    private final UUID playerId;
    private String guiId;
    private File file;
    private YamlConfiguration config;
    private String basePath;

    private Screen screen = Screen.GUI_LIST;
    private Screen previousScreen = Screen.GUI_LIST;
    private int currentPage = 0;
    private int listPage = 0;
    private int selectedSlot = -1;
    private int selectedActionIndex = -1;
    private Inventory openInventory;
    private ChatMode chatMode = ChatMode.NONE;

    private String pendingActionType;
    private String pendingActionParam0;

    private String pendingSwitchStateKey;
    private String pendingSwitchStateMaterial;
    private String pendingSwitchStateName;

    private String pendingPaginationElementTitle;

    public EditorSession(UUID playerId) { this.playerId = playerId; }

    public void loadExisting(String guiId, File file) {
        this.guiId = guiId;
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
        this.basePath = config.isConfigurationSection("guis") ? "guis." + guiId : guiId;
        this.currentPage = 0;
        this.selectedSlot = -1;
        this.selectedActionIndex = -1;
    }

    public void initNew(String newGuiId) {
        this.guiId = newGuiId;
        this.file = null;
        this.config = new YamlConfiguration();
        this.basePath = newGuiId;
        config.set(basePath + ".title", newGuiId);
        config.set(basePath + ".rows", 3);
        this.currentPage = 0;
        this.selectedSlot = -1;
    }

    private boolean bottomMode = false;

    private boolean editingViewRequirements = false;
    private String pendingConditionType;
    private java.util.LinkedList<String> pendingConditionParamKeys = new java.util.LinkedList<>();
    private java.util.Map<String, String> pendingConditionParamValues = new java.util.LinkedHashMap<>();

    public boolean isEditingViewRequirements() { return editingViewRequirements; }
    public void setEditingViewRequirements(boolean b) { this.editingViewRequirements = b; }
    public String getPendingConditionType() { return pendingConditionType; }
    public void setPendingConditionType(String t) { this.pendingConditionType = t; }
    public java.util.LinkedList<String> getPendingConditionParamKeys() { return pendingConditionParamKeys; }
    public void setPendingConditionParamKeys(java.util.LinkedList<String> keys) { this.pendingConditionParamKeys = keys; }
    public java.util.Map<String, String> getPendingConditionParamValues() { return pendingConditionParamValues; }
    public void clearConditionBuildState() {
        pendingConditionType = null;
        pendingConditionParamKeys = new java.util.LinkedList<>();
        pendingConditionParamValues = new java.util.LinkedHashMap<>();
    }

    public String getItemPath(int slot) {
        return bottomMode ? basePath + ".bottom_items." + slot : basePath + ".items." + slot;
    }

    public boolean hasItem(int slot) {
        return config != null && config.isSet(getItemPath(slot) + ".material");
    }

    public boolean isBottomMode() { return bottomMode; }
    public void setBottomMode(boolean b) { this.bottomMode = b; }

    public int getGuiRows() {
        return config != null ? Math.min(6, Math.max(1, config.getInt(basePath + ".rows", 3))) : 3;
    }

    public String getGuiTitle() {
        return config != null ? config.getString(basePath + ".title", guiId != null ? guiId : "GUI") : "GUI";
    }

    public int getTotalSlots() { return getGuiRows() * 9; }

    public int getMaxPage() { return getTotalSlots() > 45 ? 1 : 0; }

    public UUID getPlayerId() { return playerId; }
    public String getGuiId() { return guiId; }
    public void setGuiId(String id) { this.guiId = id; }
    public File getFile() { return file; }
    public void setFile(File f) { this.file = f; }
    public YamlConfiguration getConfig() { return config; }
    public String getBasePath() { return basePath; }
    public void setBasePath(String bp) { this.basePath = bp; }
    public Screen getScreen() { return screen; }
    public void setScreen(Screen s) { this.previousScreen = this.screen; this.screen = s; }
    public Screen getPreviousScreen() { return previousScreen; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int p) { this.currentPage = p; }
    public int getListPage() { return listPage; }
    public void setListPage(int p) { this.listPage = p; }
    public int getSelectedSlot() { return selectedSlot; }
    public void setSelectedSlot(int s) { this.selectedSlot = s; }
    public int getSelectedActionIndex() { return selectedActionIndex; }
    public void setSelectedActionIndex(int i) { this.selectedActionIndex = i; }
    public Inventory getOpenInventory() { return openInventory; }
    public void setOpenInventory(Inventory inv) { this.openInventory = inv; }
    public ChatMode getChatMode() { return chatMode; }
    public void setChatMode(ChatMode m) { this.chatMode = m; }
    public boolean isAwaitingChat() { return chatMode != ChatMode.NONE; }
    public void clearChatMode() { this.chatMode = ChatMode.NONE; }
    public String getPendingActionType() { return pendingActionType; }
    public void setPendingActionType(String t) { this.pendingActionType = t; }
    public String getPendingActionParam0() { return pendingActionParam0; }
    public void setPendingActionParam0(String p) { this.pendingActionParam0 = p; }

    public String getPendingSwitchStateKey() { return pendingSwitchStateKey; }
    public void setPendingSwitchStateKey(String k) { this.pendingSwitchStateKey = k; }
    public String getPendingSwitchStateMaterial() { return pendingSwitchStateMaterial; }
    public void setPendingSwitchStateMaterial(String m) { this.pendingSwitchStateMaterial = m; }
    public String getPendingSwitchStateName() { return pendingSwitchStateName; }
    public void setPendingSwitchStateName(String n) { this.pendingSwitchStateName = n; }

    public String getPendingPaginationElementTitle() { return pendingPaginationElementTitle; }
    public void setPendingPaginationElementTitle(String t) { this.pendingPaginationElementTitle = t; }
}
