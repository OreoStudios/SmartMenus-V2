package com.oreo.editor;

import com.oreo.SmartMenus;
import com.oreo.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EditorHandlers {

    public static void handleGuiListClick(SmartMenus plugin, Player player,
                                          EditorSession session, EditorManager manager,
                                          int slot, ClickType click) {

        if (slot == 45) { manager.removeSession(player.getUniqueId()); player.closeInventory(); return; }
        if (slot == 46) {
            manager.promptChat(player, session, EditorSession.ChatMode.NEW_GUI_ID,
                    "Enter a unique ID for the new GUI (letters, numbers, underscores):");
            return;
        }
        if (slot == 52 && session.getListPage() > 0) {
            session.setListPage(session.getListPage() - 1);
            EditorScreens.openGuiList(plugin, player, session); return;
        }
        if (slot == 53) {
            List<String> ids = new ArrayList<>(plugin.getGuiRegistry().getGuiIds());
            int totalPages = Math.max(1, (int) Math.ceil(ids.size() / 45.0));
            if (session.getListPage() < totalPages - 1) {
                session.setListPage(session.getListPage() + 1);
                EditorScreens.openGuiList(plugin, player, session);
            }
            return;
        }

        if (slot >= 0 && slot < 45) {
            List<String> sorted = new ArrayList<>(plugin.getGuiRegistry().getGuiIds());
            sorted.sort(String::compareToIgnoreCase);
            int idx = session.getListPage() * 45 + slot;
            if (idx >= sorted.size()) return;

            String guiId = sorted.get(idx);

            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {

                player.sendMessage(ColorUtil.color("&c\u26a0 To delete GUI '&f" + guiId
                        + "&c', manually delete its file from the guis/ folder and reload."));
                return;
            }

            java.io.File file = plugin.getGuiRegistry().getGuiFile(guiId);
            if (file == null) {
                player.sendMessage(ColorUtil.color("&cCannot find file for GUI: " + guiId));
                return;
            }
            session.loadExisting(guiId, file);
            manager.openLayoutEditor(player, session);
        }
    }

    public static void handleBottomLayoutClick(SmartMenus plugin, Player player,
                                               EditorSession session, EditorManager manager,
                                               int slot, ClickType click) {

        if (slot == 45) { manager.openPropertiesEditor(player, session); return; }
        if (slot == 46) { manager.saveGui(player, session); return; }
        if (slot >= 36) return;

        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            session.getConfig().set(session.getItemPath(slot), null);
            player.sendMessage(ColorUtil.color("&cRemoved bottom item from slot &f" + slot));
            EditorScreens.openBottomLayoutEditor(plugin, player, session);
            return;
        }
        if (click == ClickType.RIGHT) {
            manager.openSlotEditor(player, session, slot);
            return;
        }
        if (click == ClickType.LEFT) {
            if (!session.hasItem(slot)) {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held != null && !held.getType().isAir()) {
                    captureHeldItem(plugin, player, session, slot, held);
                    EditorScreens.openBottomLayoutEditor(plugin, player, session);
                } else {
                    manager.openSlotEditor(player, session, slot);
                }
            } else {
                manager.openSlotEditor(player, session, slot);
            }
        }
    }

    public static void handleLayoutClick(SmartMenus plugin, Player player,
                                         EditorSession session, EditorManager manager,
                                         int slot, ClickType click) {

        if (slot == 45) { session.setScreen(EditorSession.Screen.GUI_LIST); EditorScreens.openGuiList(plugin, player, session); return; }
        if (slot == 46) { manager.saveGui(player, session); return; }
        if (slot == 47) { manager.openPropertiesEditor(player, session); return; }
        if (slot == 52) {
            if (session.getCurrentPage() > 0) { session.setCurrentPage(session.getCurrentPage() - 1); EditorScreens.openLayoutEditor(plugin, player, session); }
            return;
        }
        if (slot == 53) {
            if (session.getCurrentPage() < session.getMaxPage()) { session.setCurrentPage(session.getCurrentPage() + 1); EditorScreens.openLayoutEditor(plugin, player, session); }
            return;
        }
        if (slot >= 45) return;

        int guiSlot = slot + session.getCurrentPage() * 45;
        if (guiSlot >= session.getTotalSlots()) return;

        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {

            session.getConfig().set(session.getItemPath(guiSlot), null);
            player.sendMessage(ColorUtil.color("&cRemoved item from slot &f" + guiSlot));
            EditorScreens.openLayoutEditor(plugin, player, session);
            return;
        }

        if (click == ClickType.RIGHT) {

            manager.openSlotEditor(player, session, guiSlot);
            return;
        }

        if (click == ClickType.LEFT) {
            if (!session.hasItem(guiSlot)) {

                ItemStack held = player.getInventory().getItemInMainHand();
                if (held != null && !held.getType().isAir()) {
                    captureHeldItem(plugin, player, session, guiSlot, held);
                    EditorScreens.openLayoutEditor(plugin, player, session);
                } else {
                    manager.openSlotEditor(player, session, guiSlot);
                }
            } else {
                manager.openSlotEditor(player, session, guiSlot);
            }
        }
    }

    private static void captureHeldItem(SmartMenus plugin, Player player,
                                        EditorSession session, int guiSlot, ItemStack held) {
        String path = session.getItemPath(guiSlot);
        session.getConfig().set(path + ".material", held.getType().name());

        org.bukkit.inventory.meta.ItemMeta meta = held.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                session.getConfig().set(path + ".name", meta.getDisplayName().replace('\u00a7', '&'));
            }
            if (meta.hasLore() && meta.getLore() != null) {
                List<String> lore = new ArrayList<>();
                for (String line : meta.getLore()) lore.add(line.replace('\u00a7', '&'));
                session.getConfig().set(path + ".lore", lore);
            }
            if (meta.hasCustomModelData()) {
                session.getConfig().set(path + ".custom_model_data", meta.getCustomModelData());
            }
        }
        player.sendMessage(ColorUtil.color("&aPlaced &f" + held.getType().name() + " &ain slot &f" + guiSlot));
    }

    public static void handleSlotEditorClick(SmartMenus plugin, Player player,
                                             EditorSession session, EditorManager manager,
                                             int slot, ClickType click) {
        int guiSlot = session.getSelectedSlot();
        String path = session.getItemPath(guiSlot);
        var config  = session.getConfig();

        switch (slot) {

            case 2  -> manager.promptChat(player, session, EditorSession.ChatMode.ITEM_MATERIAL, "Enter material name (e.g. DIAMOND_SWORD):");
            case 3  -> manager.promptChat(player, session, EditorSession.ChatMode.ITEM_NAME,     "Enter display name (supports &color codes):");
            case 4  -> manager.promptChat(player, session, EditorSession.ChatMode.ITEM_LORE,     "Enter lore lines separated by | (Line1|Line2):");
            case 5  -> { boolean c = config.getBoolean(path+".close",false); config.set(path+".close",!c); player.sendMessage(ColorUtil.color("&aClose: &f"+!c)); EditorScreens.openSlotEditor(plugin,player,session); }
            case 6  -> { boolean c = config.getBoolean(path+".glow",false);  config.set(path+".glow",!c);  player.sendMessage(ColorUtil.color("&aGlow: &f"+!c));  EditorScreens.openSlotEditor(plugin,player,session); }
            case 8  -> { if (session.isBottomMode()) manager.openBottomLayoutEditor(player,session); else manager.openLayoutEditor(player,session); }

            case 9  -> { if (!session.hasItem(guiSlot)) { player.sendMessage(ColorUtil.color("&cSet a material first.")); return; } manager.openActionList(player,session); }
            case 10 -> manager.promptChat(player, session, EditorSession.ChatMode.ITEM_COMMANDS, "Enter commands separated by | (use {player}):");
            case 11 -> cycleButtonType(session, path, click, player, plugin);
            case 12 -> cycleDynamicSource(session, path, click, player, plugin);

            case 13 -> handleContextSlot13(plugin, player, session, manager, path, config);
            case 14 -> handleContextSlot14(plugin, player, session, manager, path, config);
            case 15 -> handleContextSlot15(plugin, player, session, manager, path, config);
            case 16 -> handleContextSlot16(plugin, player, session, manager, path, config, click);

            case 18 -> { session.setEditingViewRequirements(false); manager.openConditionList(player,session); }
            case 19 -> manager.openViewReqList(player,session);
            case 20 -> handleElseItemClick(plugin, player, session, path, click);

            case 27 -> cycleItemType(session, path, click, player, plugin);
            case 28 -> manager.promptChat(player, session, EditorSession.ChatMode.ITEM_ITEM_ID, "Enter item ID (e.g. iasurvival:ruby  or  my_nexo_item):");
            case 29 -> manager.promptChat(player, session, EditorSession.ChatMode.ITEM_CUSTOM_MODEL_DATA, "Enter custom model data (number), or 0 to remove:");

            case 36 -> { boolean c=config.getBoolean(path+".update",false);     config.set(path+".update",!c);       player.sendMessage(ColorUtil.color("&eAuto-update: &f"+!c));  EditorScreens.openSlotEditor(plugin,player,session); }
            case 37 -> { boolean c=config.getBoolean(path+".is-permanent",false);config.set(path+".is-permanent",!c);player.sendMessage(ColorUtil.color("&ePermanent: &f"+!c));    EditorScreens.openSlotEditor(plugin,player,session); }
            case 38 -> { boolean c=config.getBoolean(path+".take-item",false);   config.set(path+".take-item",!c);    player.sendMessage(ColorUtil.color("&eTake item: &f"+!c));    EditorScreens.openSlotEditor(plugin,player,session); }
            case 39 -> { boolean c=config.getBoolean(path+".give-item",false);   config.set(path+".give-item",!c);    player.sendMessage(ColorUtil.color("&eGive item: &f"+!c));    EditorScreens.openSlotEditor(plugin,player,session); }

            case 45 -> toggleFlag(session, path, "HIDE_ENCHANTS",           player, plugin);
            case 46 -> toggleFlag(session, path, "HIDE_ATTRIBUTES",         player, plugin);
            case 47 -> toggleFlag(session, path, "HIDE_UNBREAKABLE",        player, plugin);
            case 48 -> toggleFlag(session, path, "HIDE_ADDITIONAL_TOOLTIP", player, plugin);
            case 53 -> {
                config.set(path, null);
                player.sendMessage(ColorUtil.color("&cItem removed from slot &f"+guiSlot));
                if (session.isBottomMode()) manager.openBottomLayoutEditor(player,session);
                else manager.openLayoutEditor(player,session);
            }
        }
    }

    private static final String[] BUTTON_TYPES = {"NONE","NEXT","PREVIOUS","DYNAMIC_PAGINATION","BACK","SWITCH","PAGINATION","HOME","CHAT_INPUT","INPUT","INVENTORY","JUMP","MAIN_MENU"};
    private static final String[] DYNAMIC_SOURCES = {"NONE","ONLINE_PLAYERS","WORLDS","PLAYER_INVENTORY","PLAYER_ENDERCHEST","PERMISSION_GROUPS"};
    private static final String[] ITEM_TYPES = {"vanilla","itemsadder","nexo","headdatabase"};
    private static final String[] INV_TYPES = {"CHEST","HOPPER","DISPENSER","FURNACE","BREWING_STAND"};

    private static void cycleButtonType(EditorSession session, String path, ClickType click, Player player, SmartMenus plugin) {
        String cur = session.getConfig().getString(path+".type","NONE");
        int idx = 0; for(int i=0;i<BUTTON_TYPES.length;i++) if(BUTTON_TYPES[i].equalsIgnoreCase(cur)){idx=i;break;}
        int next = (click==ClickType.RIGHT||click==ClickType.SHIFT_RIGHT) ? (idx-1+BUTTON_TYPES.length)%BUTTON_TYPES.length : (idx+1)%BUTTON_TYPES.length;
        String nv = BUTTON_TYPES[next];
        session.getConfig().set(path+".type", nv.equals("NONE") ? null : nv);
        player.sendMessage(ColorUtil.color("&eButton type: &f"+nv));
        EditorScreens.openSlotEditor(plugin,player,session);
    }
    private static void cycleDynamicSource(EditorSession session, String path, ClickType click, Player player, SmartMenus plugin) {
        String cur = session.getConfig().getString(path+".source","NONE");
        int idx = 0; for(int i=0;i<DYNAMIC_SOURCES.length;i++) if(DYNAMIC_SOURCES[i].equalsIgnoreCase(cur)){idx=i;break;}
        int next = (click==ClickType.RIGHT||click==ClickType.SHIFT_RIGHT) ? (idx-1+DYNAMIC_SOURCES.length)%DYNAMIC_SOURCES.length : (idx+1)%DYNAMIC_SOURCES.length;
        String nv = DYNAMIC_SOURCES[next];
        session.getConfig().set(path+".source", nv.equals("NONE") ? null : nv);
        player.sendMessage(ColorUtil.color("&eSource: &f"+nv));
        EditorScreens.openSlotEditor(plugin,player,session);
    }
    private static void cycleItemType(EditorSession session, String path, ClickType click, Player player, SmartMenus plugin) {
        String cur = session.getConfig().getString(path+".item_type","vanilla");
        int idx = 0; for(int i=0;i<ITEM_TYPES.length;i++) if(ITEM_TYPES[i].equalsIgnoreCase(cur)){idx=i;break;}
        int next = (click==ClickType.RIGHT||click==ClickType.SHIFT_RIGHT) ? (idx-1+ITEM_TYPES.length)%ITEM_TYPES.length : (idx+1)%ITEM_TYPES.length;
        String nv = ITEM_TYPES[next];
        session.getConfig().set(path+".item_type", nv.equals("vanilla") ? null : nv);
        player.sendMessage(ColorUtil.color("&eItem type: &f"+nv));
        EditorScreens.openSlotEditor(plugin,player,session);
    }
    private static void toggleFlag(EditorSession session, String path, String flag, Player player, SmartMenus plugin) {
        List<String> flags = new ArrayList<>(session.getConfig().getStringList(path+".item_flags"));
        if (flags.contains(flag)) { flags.remove(flag); player.sendMessage(ColorUtil.color("&cFlag removed: &f"+flag)); }
        else                      { flags.add(flag);    player.sendMessage(ColorUtil.color("&aFlag added: &f"+flag));   }
        session.getConfig().set(path+".item_flags", flags.isEmpty() ? null : flags);
        EditorScreens.openSlotEditor(plugin,player,session);
    }
    private static void handleElseItemClick(SmartMenus plugin, Player player, EditorSession session, String path, ClickType click) {
        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            session.getConfig().set(path+".else-item", null);
            player.sendMessage(ColorUtil.color("&cElse item removed."));
        } else {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held != null && !held.getType().isAir()) {
                session.getConfig().set(path+".else-item.material", held.getType().name());
                org.bukkit.inventory.meta.ItemMeta m = held.getItemMeta();
                if (m != null && m.hasDisplayName()) session.getConfig().set(path+".else-item.name", m.getDisplayName().replace('\u00a7','&'));
                player.sendMessage(ColorUtil.color("&aElse item set to: &f"+held.getType().name()));
            } else {
                player.sendMessage(ColorUtil.color("&cHold an item in your hand to set as else-item, or right-click to remove."));
                return;
            }
        }
        EditorScreens.openSlotEditor(plugin,player,session);
    }

    public static void handlePropertiesClick(SmartMenus plugin, Player player,
                                             EditorSession session, EditorManager manager,
                                             int slot, ClickType click) {
        String base = session.getBasePath();
        switch (slot) {
            case 0 -> manager.promptChat(player, session, EditorSession.ChatMode.GUI_TITLE,    "Enter GUI title (supports &color codes):");
