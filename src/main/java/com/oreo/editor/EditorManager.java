package com.oreo.editor;

import com.oreo.SmartMenus;
import com.oreo.util.ColorUtil;
import com.oreo.util.Ids;
import com.oreo.util.SmartScheduler;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditorManager {

    private final SmartMenus plugin;
    private final Map<UUID, EditorSession> sessions = new HashMap<>();

    public EditorManager(SmartMenus plugin) {
        this.plugin = plugin;
    }

    public SmartMenus getPlugin() { return plugin; }

    public EditorSession getSession(UUID playerId) { return sessions.get(playerId); }

    public boolean hasSession(UUID playerId) { return sessions.containsKey(playerId); }

    public void removeSession(UUID playerId) { sessions.remove(playerId); }

    public void openEditor(Player player) {
        if (!player.hasPermission("smartmenus.editor") && !player.hasPermission("ogui.editor")) {
            player.sendMessage(ColorUtil.color("&cYou don't have permission to use the GUI editor."));
            return;
        }
        EditorSession session = sessions.computeIfAbsent(player.getUniqueId(), EditorSession::new);
        session.setScreen(EditorSession.Screen.GUI_LIST);
        session.setListPage(0);
        EditorScreens.openGuiList(plugin, player, session);
    }

    public void openLayoutEditor(Player player, EditorSession session) {
        session.setBottomMode(false);
        session.setScreen(EditorSession.Screen.LAYOUT);
        session.setCurrentPage(0);
        EditorScreens.openLayoutEditor(plugin, player, session);
    }

    public void openBottomLayoutEditor(Player player, EditorSession session) {
        session.setBottomMode(true);
        session.setScreen(EditorSession.Screen.BOTTOM_LAYOUT);
        EditorScreens.openBottomLayoutEditor(plugin, player, session);
    }

    public void openSlotEditor(Player player, EditorSession session, int slot) {
        session.setSelectedSlot(slot);
        session.setScreen(EditorSession.Screen.SLOT_EDITOR);
        EditorScreens.openSlotEditor(plugin, player, session);
    }

    public void openPropertiesEditor(Player player, EditorSession session) {
        session.setScreen(EditorSession.Screen.PROPERTIES);
        EditorScreens.openPropertiesEditor(plugin, player, session);
    }

    public void openActionList(Player player, EditorSession session) {
        session.setScreen(EditorSession.Screen.ACTION_LIST);
        EditorScreens.openActionList(plugin, player, session);
    }

    public void openActionTypePicker(Player player, EditorSession session) {
        session.setScreen(EditorSession.Screen.ACTION_TYPE);
        EditorScreens.openActionTypePicker(plugin, player, session);
    }

    public void openConditionList(Player player, EditorSession session) {
        session.setScreen(EditorSession.Screen.CONDITION_LIST);
        EditorScreens.openConditionList(plugin, player, session);
    }

    public void openConditionTypePicker(Player player, EditorSession session) {
        session.setScreen(EditorSession.Screen.CONDITION_TYPE);
        EditorScreens.openConditionTypePicker(plugin, player, session);
    }

    public void openViewReqList(Player player, EditorSession session) {
        session.setEditingViewRequirements(true);
        session.setScreen(EditorSession.Screen.VIEW_REQ_LIST);
        EditorScreens.openConditionList(plugin, player, session);
    }

    public void openSwitchStates(Player player, EditorSession session) {
        session.setScreen(EditorSession.Screen.SWITCH_STATES);
        EditorScreens.openSwitchStates(plugin, player, session);
    }

    public void openPaginationElements(Player player, EditorSession session) {
        session.setScreen(EditorSession.Screen.PAGINATION_ELEMENTS);
        EditorScreens.openPaginationElements(plugin, player, session);
    }

    public void saveGui(Player player, EditorSession session) {
        try {
            File file = session.getFile();
            if (file == null) {
                File guisDir = new File(plugin.getDataFolder(), "guis");
                File editorDir = new File(guisDir, "editor");
                if (!editorDir.exists()) editorDir.mkdirs();
                file = new File(editorDir, session.getGuiId() + ".yml");
                session.setFile(file);
            }
            session.getConfig().save(file);
            SmartScheduler.runTask(plugin, () -> {
                plugin.reloadGuis();
                player.sendMessage(ColorUtil.color("&a\u2714 GUI '&f" + session.getGuiId() + "&a' saved!"));
                player.sendMessage(ColorUtil.color("&7File: &f" + session.getFile().getName()));
            });
        } catch (IOException e) {
            player.sendMessage(ColorUtil.color("&c\u2717 Failed to save: " + e.getMessage()));
            plugin.getLogger().severe("[Editor] Save failed: " + e.getMessage());
        }
    }

    public void promptChat(Player player, EditorSession session, EditorSession.ChatMode mode, String prompt) {
        session.setChatMode(mode);
        player.closeInventory();
        player.sendMessage(ColorUtil.color("&8&m                                        "));
        player.sendMessage(ColorUtil.color("&e" + prompt));
        player.sendMessage(ColorUtil.color("&7Type &ccancel &7to cancel."));
        player.sendMessage(ColorUtil.color("&8&m                                        "));
    }

    public void handleChatInput(Player player, EditorSession session, String input) {
        EditorSession.ChatMode mode = session.getChatMode();
        session.clearChatMode();

        switch (mode) {
            case NEW_GUI_ID -> {
                String cleaned = Ids.slugify(input);
                if (cleaned.isEmpty()) {
                    player.sendMessage(ColorUtil.color("&cInvalid GUI ID."));
                    SmartScheduler.runTaskForPlayer(plugin, player, () -> openEditor(player));
                    return;
                }
                session.initNew(cleaned);
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openLayoutEditor(player, session));
            }
            case GUI_TITLE -> {
                session.getConfig().set(session.getBasePath() + ".title", input);
                player.sendMessage(ColorUtil.color("&aTitle set to: &f" + input));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openPropertiesEditor(player, session));
            }
            case GUI_COMMANDS -> {

                java.util.List<String> cmds = new java.util.ArrayList<>();
                for (String c : input.split(",")) {
                    String t = c.trim();
                    if (!t.isEmpty()) cmds.add(t.startsWith("/") ? t.substring(1) : t);
                }
                session.getConfig().set(session.getBasePath() + ".commands", cmds.isEmpty() ? null : cmds);
                player.sendMessage(ColorUtil.color("&aCommands set: &f" + String.join(", ", cmds)));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openPropertiesEditor(player, session));
            }
            case ITEM_MATERIAL -> {
                int slot = session.getSelectedSlot();
                String matName = input.toUpperCase().replace(" ", "_");
                org.bukkit.Material mat = org.bukkit.Material.getMaterial(matName);
                if (mat == null || mat.isAir()) {
                    player.sendMessage(ColorUtil.color("&cUnknown material: &f" + matName));
                    SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, slot));
                    return;
                }
                if (!session.hasItem(slot)) {
                    session.getConfig().set(session.getItemPath(slot) + ".material", matName);
                }
                session.getConfig().set(session.getItemPath(slot) + ".material", matName);
                player.sendMessage(ColorUtil.color("&aMaterial set: &f" + matName));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, slot));
            }
            case ITEM_NAME -> {
                int slot = session.getSelectedSlot();
                session.getConfig().set(session.getItemPath(slot) + ".name", input);
                player.sendMessage(ColorUtil.color("&aName set: &f" + input));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, slot));
            }
            case ITEM_LORE -> {
                int slot = session.getSelectedSlot();
                java.util.List<String> loreLines = new java.util.ArrayList<>();
                for (String line : input.split("\\|")) loreLines.add(line.trim());
                session.getConfig().set(session.getItemPath(slot) + ".lore", loreLines);
                player.sendMessage(ColorUtil.color("&aLore set (&f" + loreLines.size() + " lines&a)."));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, slot));
            }
            case ITEM_COMMANDS -> {
                int slot = session.getSelectedSlot();
                java.util.List<String> cmds = new java.util.ArrayList<>();
                for (String c : input.split("\\|")) {
                    String t = c.trim();
                    if (!t.isEmpty()) cmds.add(t);
                }
                session.getConfig().set(session.getItemPath(slot) + ".commands", cmds.isEmpty() ? null : cmds);
                player.sendMessage(ColorUtil.color("&aCommands set (&f" + cmds.size() + "&a)."));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, slot));
            }
            case ACTION_PARAM_0 -> {
                String actionType = session.getPendingActionType();
                String[] params = EditorScreens.getActionParams(actionType);
                if (params.length >= 2) {
                    session.setPendingActionParam0(input);
                    promptChat(player, session, EditorSession.ChatMode.ACTION_PARAM_1,
                            "Enter the " + params[1] + " for " + actionType + " (or 'skip'):");
                } else {

                    java.util.Map<String, Object> actionMap = new java.util.LinkedHashMap<>();
                    actionMap.put("type", actionType);
                    if (params.length >= 1) actionMap.put(params[0], input);
                    addActionToSlot(session, actionMap);
                    player.sendMessage(ColorUtil.color("&aAction added: &f" + actionType));
                    SmartScheduler.runTaskForPlayer(plugin, player, () -> openActionList(player, session));
                }
            }
            case ACTION_PARAM_1 -> {
                String actionType = session.getPendingActionType();
                String[] params = EditorScreens.getActionParams(actionType);
                java.util.Map<String, Object> actionMap = new java.util.LinkedHashMap<>();
                actionMap.put("type", actionType);
                if (params.length >= 1) actionMap.put(params[0], session.getPendingActionParam0());
                if (params.length >= 2 && !input.equalsIgnoreCase("skip")) actionMap.put(params[1], input);
                addActionToSlot(session, actionMap);
                player.sendMessage(ColorUtil.color("&aAction added: &f" + actionType));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openActionList(player, session));
            }
            case CONDITION_PARAM -> {

                String paramKey = session.getPendingConditionParamKeys().poll();
                if (paramKey != null) {
                    session.getPendingConditionParamValues().put(paramKey, input);
                }
                if (!session.getPendingConditionParamKeys().isEmpty()) {

                    String nextKey = session.getPendingConditionParamKeys().peek();
                    boolean isIgnoreCase = "ignore_case".equals(nextKey);
                    String prompt = isIgnoreCase
                            ? "Enter &e" + nextKey + " &7for condition &e" + session.getPendingConditionType() + " &7(or 'skip'):"
                            : "Enter &e" + nextKey + " &7for condition &e" + session.getPendingConditionType() + "&7:";
                    promptChat(player, session, EditorSession.ChatMode.CONDITION_PARAM, prompt);
                } else {

                    java.util.Map<String, Object> condMap = new java.util.LinkedHashMap<>();
                    condMap.put("type", session.getPendingConditionType());
                    for (var e : session.getPendingConditionParamValues().entrySet()) {
                        String val = e.getValue();
                        if (val.equalsIgnoreCase("skip")) {

                        } else if (val.equalsIgnoreCase("true")) {
                            condMap.put(e.getKey(), true);
                        } else if (val.equalsIgnoreCase("false")) {
                            condMap.put(e.getKey(), false);
                        } else {
                            try { condMap.put(e.getKey(), Double.parseDouble(val)); }
                            catch (NumberFormatException ex) { condMap.put(e.getKey(), val); }
                        }
                    }
                    addConditionToSlot(session, condMap);
                    session.clearConditionBuildState();
                    player.sendMessage(ColorUtil.color("&aCondition added: &f" + condMap.get("type")));
                    if (session.isEditingViewRequirements()) {
                        SmartScheduler.runTaskForPlayer(plugin, player, () -> openViewReqList(player, session));
                    } else {
                        SmartScheduler.runTaskForPlayer(plugin, player, () -> openConditionList(player, session));
                    }
                }
            }
            case ITEM_ITEM_ID -> {
                int slot = session.getSelectedSlot();
                session.getConfig().set(session.getItemPath(slot) + ".item_id", input);
                player.sendMessage(ColorUtil.color("&aItem ID set: &f" + input));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, slot));
            }
            case ITEM_CUSTOM_MODEL_DATA -> {
                int slot = session.getSelectedSlot();
                try {
                    int val = Integer.parseInt(input.trim());
                    session.getConfig().set(session.getItemPath(slot) + ".custom_model_data", val);
                    player.sendMessage(ColorUtil.color("&aCustom model data set: &f" + val));
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.color("&cMust be a number."));
                }
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, slot));
            }
            case PROPERTIES_NPC_ID -> {
                try {
                    int npcId = Integer.parseInt(input.trim());
                    session.getConfig().set(session.getBasePath() + ".npc_id", npcId);
                    player.sendMessage(ColorUtil.color("&aNPC ID set: &f" + npcId));
                } catch (NumberFormatException e) {
                    if (input.equalsIgnoreCase("none") || input.equals("0")) {
                        session.getConfig().set(session.getBasePath() + ".npc_id", null);
                        player.sendMessage(ColorUtil.color("&aNPC binding removed."));
                    } else {
                        player.sendMessage(ColorUtil.color("&cMust be a number (or 'none' to remove)."));
                    }
                }
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openPropertiesEditor(player, session));
            }
            case ITEM_TO_PAGE -> {
                int guiSlot = session.getSelectedSlot();
                String path = session.getItemPath(guiSlot);
                try {
                    int pageNum = Integer.parseInt(input.trim());
                    session.getConfig().set(path + ".to-page", pageNum);
                    player.sendMessage(ColorUtil.color("&aTo-page set: &f" + pageNum));
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.color("&cMust be a number."));
                }
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, guiSlot));
            }
            case ITEM_SWITCH_KEY -> {
                int guiSlot = session.getSelectedSlot();
                String path = session.getItemPath(guiSlot);
                session.getConfig().set(path + ".key", input);
                player.sendMessage(ColorUtil.color("&aSwitch key set: &f" + input));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, guiSlot));
            }
            case ITEM_ON_PLACE -> {
                int guiSlot = session.getSelectedSlot();
                String path = session.getItemPath(guiSlot);
                java.util.List<String> cmds = new java.util.ArrayList<>();
                for (String c : input.split("\\|")) { String t = c.trim(); if (!t.isEmpty()) cmds.add(t); }
                session.getConfig().set(path + ".on-place", cmds.isEmpty() ? null : cmds);
                player.sendMessage(ColorUtil.color("&aOn-place commands set (&f" + cmds.size() + "&a)."));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, guiSlot));
            }
            case SWITCH_STATE_KEY -> {
                session.setPendingSwitchStateKey(input);
                promptChat(player, session, EditorSession.ChatMode.SWITCH_STATE_MATERIAL,
                        "Enter material for state &e" + input + "&7 (e.g. DIAMOND):");
            }
            case SWITCH_STATE_MATERIAL -> {
                session.setPendingSwitchStateMaterial(input.toUpperCase());
                promptChat(player, session, EditorSession.ChatMode.SWITCH_STATE_NAME,
                        "Enter display name for state &e" + session.getPendingSwitchStateKey() + "&7:");
            }
            case SWITCH_STATE_NAME -> {
                session.setPendingSwitchStateName(input);
                promptChat(player, session, EditorSession.ChatMode.SWITCH_STATE_COMMANDS,
                        "Enter commands for state &e" + session.getPendingSwitchStateKey() + " &7(pipe-separated, or 'skip'):");
            }
            case SWITCH_STATE_COMMANDS -> {
                int guiSlot = session.getSelectedSlot();
                String path = session.getItemPath(guiSlot);
                String stateKey = session.getPendingSwitchStateKey();
                String statePath = path + ".buttons." + stateKey;
                session.getConfig().set(statePath + ".material", session.getPendingSwitchStateMaterial());
                session.getConfig().set(statePath + ".name", session.getPendingSwitchStateName());
                if (!input.equalsIgnoreCase("skip")) {
                    java.util.List<String> cmds = new java.util.ArrayList<>();
                    for (String c : input.split("\\|")) { String t = c.trim(); if (!t.isEmpty()) cmds.add(t); }
                    if (!cmds.isEmpty()) session.getConfig().set(statePath + ".commands", cmds);
                }
                player.sendMessage(ColorUtil.color("&aState '&f" + stateKey + "&a' added."));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSwitchStates(player, session));
            }
            case PAGINATION_SLOTS -> {
                int guiSlot = session.getSelectedSlot();
                String path = session.getItemPath(guiSlot);
                java.util.List<String> slotEntries = new java.util.ArrayList<>();
                for (String s : input.split(",")) { String t = s.trim(); if (!t.isEmpty()) slotEntries.add(t); }
                session.getConfig().set(path + ".slots", slotEntries.isEmpty() ? null : slotEntries);
                player.sendMessage(ColorUtil.color("&aPagination slots set: &f" + String.join(", ", slotEntries)));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openSlotEditor(player, session, guiSlot));
            }
            case PAGINATION_ELEMENT_TITLE -> {
                session.setPendingPaginationElementTitle(input);
                promptChat(player, session, EditorSession.ChatMode.PAGINATION_ELEMENT_DESC,
                        "Enter description for element (or 'skip'):");
            }
            case PAGINATION_ELEMENT_DESC -> {
                int guiSlot = session.getSelectedSlot();
                String path = session.getItemPath(guiSlot);
                java.util.Map<String, Object> elem = new java.util.LinkedHashMap<>();
                elem.put("title", session.getPendingPaginationElementTitle());
                if (!input.equalsIgnoreCase("skip")) elem.put("description", input);
                @SuppressWarnings("unchecked")
                java.util.List<Object> elemList = (java.util.List<Object>) session.getConfig().getList(path + ".elements");
                if (elemList == null) elemList = new java.util.ArrayList<>();
                elemList.add(elem);
                session.getConfig().set(path + ".elements", elemList);
                player.sendMessage(ColorUtil.color("&aElement '&f" + session.getPendingPaginationElementTitle() + "&a' added."));
                SmartScheduler.runTaskForPlayer(plugin, player, () -> openPaginationElements(player, session));
            }
            default -> SmartScheduler.runTaskForPlayer(plugin, player, () -> openEditor(player));
        }
    }

    @SuppressWarnings("unchecked")
    private void addActionToSlot(EditorSession session, java.util.Map<String, Object> actionMap) {
        String actionsPath = session.getItemPath(session.getSelectedSlot()) + ".actions";
        java.util.List<Object> actions = (java.util.List<Object>) session.getConfig().getList(actionsPath);
        if (actions == null) actions = new java.util.ArrayList<>();
        actions.add(actionMap);
        session.getConfig().set(actionsPath, actions);
    }

    @SuppressWarnings("unchecked")
    private void addConditionToSlot(EditorSession session, java.util.Map<String, Object> condMap) {
        String key = session.isEditingViewRequirements()
                ? session.getItemPath(session.getSelectedSlot()) + ".view-requirements"
                : session.getItemPath(session.getSelectedSlot()) + ".conditions";
        java.util.List<Object> list = (java.util.List<Object>) session.getConfig().getList(key);
        if (list == null) list = new java.util.ArrayList<>();
        list.add(condMap);
        session.getConfig().set(key, list);
    }
}
