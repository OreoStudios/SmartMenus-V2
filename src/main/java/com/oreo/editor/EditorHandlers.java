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
            case 1 -> {
                int rows = session.getGuiRows();
                rows = (click==ClickType.LEFT||click==ClickType.SHIFT_LEFT) ? Math.min(6,rows+1) : Math.max(1,rows-1);
                session.getConfig().set(base+".rows", rows);
                player.sendMessage(ColorUtil.color("&aRows: &f"+rows));
                EditorScreens.openPropertiesEditor(plugin,player,session);
            }
            case 2 -> manager.promptChat(player, session, EditorSession.ChatMode.GUI_COMMANDS, "Enter commands separated by commas:");
            case 4 -> {
                boolean cur = session.getConfig().getBoolean(base+".use_bottom_inventory",false);
                session.getConfig().set(base+".use_bottom_inventory",!cur);
                player.sendMessage(ColorUtil.color("&aBottom inventory: &f"+!cur));
                EditorScreens.openPropertiesEditor(plugin,player,session);
            }
            case 5 -> manager.openBottomLayoutEditor(player,session);
            case 6 -> {
                String cur = session.getConfig().getString(base+".inventory_type", session.getConfig().getString(base+".inventory-type","CHEST"));
                int idx = 0; for(int i=0;i<INV_TYPES.length;i++) if(INV_TYPES[i].equalsIgnoreCase(cur)){idx=i;break;}
                int next = (click==ClickType.RIGHT||click==ClickType.SHIFT_RIGHT) ? (idx-1+INV_TYPES.length)%INV_TYPES.length : (idx+1)%INV_TYPES.length;
                String nv = INV_TYPES[next];
                session.getConfig().set(base+".inventory_type", nv.equals("CHEST") ? null : nv);
                player.sendMessage(ColorUtil.color("&eInventory type: &f"+nv));
                EditorScreens.openPropertiesEditor(plugin,player,session);
            }
            case 7 -> {
                if (click==ClickType.RIGHT||click==ClickType.SHIFT_RIGHT) {
                    session.getConfig().set(base+".npc_id", null);
                    player.sendMessage(ColorUtil.color("&cNPC binding removed."));
                    EditorScreens.openPropertiesEditor(plugin,player,session);
                } else {
                    manager.promptChat(player, session, EditorSession.ChatMode.PROPERTIES_NPC_ID, "Enter NPC ID number (or 'none' to remove):");
                }
            }
            case 8 -> manager.openLayoutEditor(player,session);
            case 9 -> {

                if (click==ClickType.RIGHT||click==ClickType.SHIFT_RIGHT) {
                    session.getConfig().set(base+".fill", null);
                    player.sendMessage(ColorUtil.color("&cFill item removed."));
                    EditorScreens.openPropertiesEditor(plugin,player,session);
                } else {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (held!=null && !held.getType().isAir()) {
                        session.getConfig().set(base+".fill.material", held.getType().name());
                        org.bukkit.inventory.meta.ItemMeta m = held.getItemMeta();
                        if (m!=null && m.hasDisplayName()) session.getConfig().set(base+".fill.name", m.getDisplayName().replace('\u00a7','&'));
                        player.sendMessage(ColorUtil.color("&aFill item set: &f"+held.getType().name()));
                    } else {
                        player.sendMessage(ColorUtil.color("&cHold the fill item in your hand, or right-click to remove."));
                        return;
                    }
                    EditorScreens.openPropertiesEditor(plugin,player,session);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void handleConditionListClick(SmartMenus plugin, Player player,
                                                EditorSession session, EditorManager manager,
                                                int slot, ClickType click) {
        boolean viewReqs = session.isEditingViewRequirements();
        String condKey   = viewReqs ? ".view-requirements" : ".conditions";
        String itemPath  = session.getItemPath(session.getSelectedSlot());

        if (slot == 45) { manager.openConditionTypePicker(player, session); return; }
        if (slot == 46 && viewReqs) {

            if (session.getConfig().isSet(itemPath + ".else-item")) {
                session.getConfig().set(itemPath + ".else-item", null);
                player.sendMessage(ColorUtil.color("&cElse item removed."));
            } else {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held != null && !held.getType().isAir()) {
                    session.getConfig().set(itemPath + ".else-item.material", held.getType().name());
                    player.sendMessage(ColorUtil.color("&aElse item set to: &f" + held.getType().name()));
                } else {
                    player.sendMessage(ColorUtil.color("&cHold the else-item in your hand."));
                    return;
                }
            }
            EditorScreens.openConditionList(plugin, player, session);
            return;
        }
        if (slot == 53) { session.setEditingViewRequirements(false); manager.openSlotEditor(player, session, session.getSelectedSlot()); return; }

        if (slot < 45) {
            List<Object> list = (List<Object>) session.getConfig().getList(itemPath + condKey);
            if (list == null || slot >= list.size()) return;
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                list.remove(slot);
                session.getConfig().set(itemPath + condKey, list.isEmpty() ? null : list);
                player.sendMessage(ColorUtil.color("&cCondition removed."));
                EditorScreens.openConditionList(plugin, player, session);
            }
        }
    }

    public static void handleConditionTypeClick(SmartMenus plugin, Player player,
                                                EditorSession session, EditorManager manager,
                                                int slot, ClickType click) {
        if (slot == 53) {
            if (session.isEditingViewRequirements()) manager.openViewReqList(player, session);
            else manager.openConditionList(player, session);
            return;
        }
        String[] conditionTypes = {
            "VAULT_MONEY","PERMISSION","XP_LEVEL","XP_POINTS","ITEM","ITEM_CUSTOM_MODEL",
            "ITEMSADDER","NEXO","WEATHER","WORLD","WORLDGUARD_REGION",
            "PLACEHOLDER_EQUALS","PLACEHOLDER_GREATER_THAN","PLACEHOLDER_LESS_THAN","PLACEHOLDER_CONTAINS",
            "EXPRESSION","SCRIPT","LUCKPERMS_GROUP","OREO_CURRENCY","OREO_WARPS"
        };
        if (slot >= 0 && slot < conditionTypes.length) {
            String type = conditionTypes[slot];
            session.clearConditionBuildState();
            session.setPendingConditionType(type);
            session.setPendingConditionParamKeys(EditorScreens.getConditionParamKeys(type));

            if (session.getPendingConditionParamKeys().isEmpty()) {

                java.util.Map<String, Object> cm = new java.util.LinkedHashMap<>();
                cm.put("type", type);
                addCondition(session, cm);
                player.sendMessage(ColorUtil.color("&aCondition added: &f" + type));
                if (session.isEditingViewRequirements()) manager.openViewReqList(player, session);
                else manager.openConditionList(player, session);
            } else {
                String firstKey = session.getPendingConditionParamKeys().peek();
                manager.promptChat(player, session, EditorSession.ChatMode.CONDITION_PARAM,
                        "Enter &e" + firstKey + " &7for condition &e" + type + "&7:");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addCondition(EditorSession session, java.util.Map<String, Object> condMap) {
        String key = session.isEditingViewRequirements()
                ? session.getItemPath(session.getSelectedSlot()) + ".view-requirements"
                : session.getItemPath(session.getSelectedSlot()) + ".conditions";
        List<Object> list = (List<Object>) session.getConfig().getList(key);
        if (list == null) list = new java.util.ArrayList<>();
        list.add(condMap);
        session.getConfig().set(key, list);
    }

    private static void handleContextSlot13(SmartMenus plugin, Player player, EditorSession session,
                                             EditorManager manager, String path, org.bukkit.configuration.file.YamlConfiguration config) {
        String btnType = config.getString(path + ".type", "NONE");
        switch (btnType.toUpperCase()) {
            case "JUMP" -> manager.promptChat(player, session, EditorSession.ChatMode.ITEM_TO_PAGE,
                    "Enter the page number to jump to (integer):");
            case "SWITCH" -> manager.promptChat(player, session, EditorSession.ChatMode.ITEM_SWITCH_KEY,
                    "Enter the switch key name:");
            case "PAGINATION" -> manager.promptChat(player, session, EditorSession.ChatMode.PAGINATION_SLOTS,
                    "Enter pagination slots as comma-separated ranges (e.g. 10-16,19-25):");
            default -> {}
        }
    }

    private static void handleContextSlot14(SmartMenus plugin, Player player, EditorSession session,
                                             EditorManager manager, String path, org.bukkit.configuration.file.YamlConfiguration config) {
        String btnType = config.getString(path + ".type", "NONE");
        switch (btnType.toUpperCase()) {
            case "SWITCH" -> manager.openSwitchStates(player, session);
            case "PAGINATION" -> manager.openPaginationElements(player, session);
            default -> {}
        }
    }

    private static void handleContextSlot15(SmartMenus plugin, Player player, EditorSession session,
                                             EditorManager manager, String path, org.bukkit.configuration.file.YamlConfiguration config) {
        String btnType = config.getString(path + ".type", "NONE");
        if (btnType.equalsIgnoreCase("INPUT")) {
            manager.promptChat(player, session, EditorSession.ChatMode.ITEM_ON_PLACE,
                    "Enter on-place commands (pipe-separated):");
        }
    }

    private static void handleContextSlot16(SmartMenus plugin, Player player, EditorSession session,
                                             EditorManager manager, String path, org.bukkit.configuration.file.YamlConfiguration config, ClickType click) {
        String btnType = config.getString(path + ".type", "NONE");
        if (btnType.equalsIgnoreCase("INPUT")) {
            boolean cur = config.getBoolean(path + ".remove-on-place", false);
            config.set(path + ".remove-on-place", !cur);
            player.sendMessage(com.oreo.util.ColorUtil.color("&eRemove on place: &f" + !cur));
            EditorScreens.openSlotEditor(plugin, player, session);
        }
    }

    @SuppressWarnings("unchecked")
    public static void handleSwitchStatesClick(SmartMenus plugin, Player player,
                                               EditorSession session, EditorManager manager,
                                               int slot, ClickType click) {
        int guiSlot = session.getSelectedSlot();
        String path = session.getItemPath(guiSlot);

        if (slot == 45) {
            manager.promptChat(player, session, EditorSession.ChatMode.SWITCH_STATE_KEY,
                    "Enter key name for the new state:");
            return;
        }
        if (slot == 53) {
            manager.openSlotEditor(player, session, guiSlot);
            return;
        }
        if (slot < 45) {
            var statesSection = session.getConfig().getConfigurationSection(path + ".buttons");
            if (statesSection == null) return;
            List<String> keys = new ArrayList<>(statesSection.getKeys(false));
            if (slot >= keys.size()) return;
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                String stateKey = keys.get(slot);
                session.getConfig().set(path + ".buttons." + stateKey, null);
                player.sendMessage(com.oreo.util.ColorUtil.color("&cState '&f" + stateKey + "&c' removed."));
                EditorScreens.openSwitchStates(plugin, player, session);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void handlePaginationElementsClick(SmartMenus plugin, Player player,
                                                      EditorSession session, EditorManager manager,
                                                      int slot, ClickType click) {
        int guiSlot = session.getSelectedSlot();
        String path = session.getItemPath(guiSlot);

        if (slot == 45) {
            manager.promptChat(player, session, EditorSession.ChatMode.PAGINATION_ELEMENT_TITLE,
                    "Enter title for the new pagination element:");
            return;
        }
        if (slot == 53) {
            manager.openSlotEditor(player, session, guiSlot);
            return;
        }
        if (slot < 45) {
            List<Object> elemList = (List<Object>) session.getConfig().getList(path + ".elements");
            if (elemList == null || slot >= elemList.size()) return;
            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                elemList.remove(slot);
                session.getConfig().set(path + ".elements", elemList.isEmpty() ? null : elemList);
                player.sendMessage(com.oreo.util.ColorUtil.color("&cElement removed."));
                EditorScreens.openPaginationElements(plugin, player, session);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void handleActionListClick(SmartMenus plugin, Player player,
                                             EditorSession session, EditorManager manager,
                                             int slot, ClickType click) {
        if (slot == 45) { manager.openActionTypePicker(player, session); return; }
        if (slot == 53) { manager.openSlotEditor(player, session, session.getSelectedSlot()); return; }

        if (slot < 45) {
            String actionsPath = session.getItemPath(session.getSelectedSlot()) + ".actions";
            List<Object> actions = (List<Object>) session.getConfig().getList(actionsPath);
            if (actions == null || slot >= actions.size()) return;

            if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
                actions.remove(slot);
                session.getConfig().set(actionsPath, actions.isEmpty() ? null : actions);
                player.sendMessage(ColorUtil.color("&cAction removed."));
                EditorScreens.openActionList(plugin, player, session);
            }
        }
    }

    public static void handleActionTypeClick(SmartMenus plugin, Player player,
                                             EditorSession session, EditorManager manager,
                                             int slot, ClickType click) {
        if (slot == 53) { manager.openActionList(player, session); return; }

        String[] actionTypes = {
                "PLAYER_MESSAGE", "BROADCAST", "ACTION_BAR", "TITLE",
                "CONSOLE_COMMAND", "PLAYER_COMMAND", "SOUND", "OPEN_GUI",
                "CLOSE", "GIVE_MONEY", "TAKE_MONEY", "GIVE_XP", "EFFECT", "SERVER_CONNECT"
        };

        if (slot >= 0 && slot < actionTypes.length) {
            String actionType = actionTypes[slot];
            session.setPendingActionType(actionType);
            session.setPendingActionParam0(null);

            String[] params = EditorScreens.getActionParams(actionType);
            if (params.length == 0) {

                java.util.Map<String, Object> actionMap = new java.util.LinkedHashMap<>();
                actionMap.put("type", actionType);
                String actPath = session.getItemPath(session.getSelectedSlot()) + ".actions";
                List<Object> actions = (List<Object>) session.getConfig().getList(actPath);
                if (actions == null) actions = new ArrayList<>();
                actions.add(actionMap);
                session.getConfig().set(actPath, actions);
                player.sendMessage(ColorUtil.color("&aAction added: &f" + actionType));
                manager.openActionList(player, session);
            } else {
                manager.promptChat(player, session, EditorSession.ChatMode.ACTION_PARAM_0,
                        "Enter the &e" + params[0] + " &7for &e" + actionType + "&7:");
            }
        }
    }
}
