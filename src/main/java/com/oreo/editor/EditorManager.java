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
