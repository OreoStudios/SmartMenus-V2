package com.oreo.editor;

import com.oreo.SmartMenus;
import com.oreo.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EditorScreens {

    public static ItemStack item(Material mat, String name, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta == null) return is;
        meta.setDisplayName(ColorUtil.color(name));
        if (lore.length > 0) {
            List<String> l = new ArrayList<>();
            for (String s : lore) l.add(ColorUtil.color(s));
            meta.setLore(l);
        }
        is.setItemMeta(meta);
        return is;
    }

    private static ItemStack sep() {
        return item(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    private static void fill(Inventory inv) {
        ItemStack s = sep();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, s);
        }
    }

    public static String[] getActionParams(String type) {
        if (type == null) return new String[]{"message"};
        return switch (type.toUpperCase()) {
            case "PLAYER_MESSAGE", "BROADCAST", "ACTION_BAR" -> new String[]{"message"};
            case "CONSOLE_COMMAND", "PLAYER_COMMAND"         -> new String[]{"command"};
            case "TITLE"                                     -> new String[]{"title", "subtitle"};
            case "SOUND"                                     -> new String[]{"sound"};
            case "OPEN_GUI"                                  -> new String[]{"gui"};
            case "CLOSE"                                     -> new String[]{};
            case "GIVE_MONEY", "TAKE_MONEY"                  -> new String[]{"amount"};
            case "GIVE_XP"                                   -> new String[]{"amount"};
            case "EFFECT"                                    -> new String[]{"effect", "duration"};
            case "SERVER_CONNECT"                            -> new String[]{"server"};
            default                                          -> new String[]{"message"};
        };
    }

    public static void openGuiList(SmartMenus plugin, Player player, EditorSession session) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.color("&8[SmartMenus Editor] &fGUI List"));

        Set<String> guiIds = plugin.getGuiRegistry().getGuiIds();
        List<String> sorted = new ArrayList<>(guiIds);
        sorted.sort(String::compareToIgnoreCase);

        int page = session.getListPage();
        int perPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil(sorted.size() / (double) perPage));
        page = Math.min(page, totalPages - 1);
        session.setListPage(page);

        int start = page * perPage;
        int end = Math.min(start + perPage, sorted.size());

        for (int i = start; i < end; i++) {
            String guiId = sorted.get(i);
            var def = plugin.getGuiRegistry().getGui(guiId);
            int itemCount = def != null ? def.getItems().size() : 0;
            String title = def != null ? def.getTitle() : guiId;
            int rows = def != null ? def.getRows() : 1;

            ItemStack guiItem = item(Material.WRITABLE_BOOK, "&e" + guiId,
                    "&7Title: &f" + title,
                    "&7Rows: &f" + rows + " &7| Items: &f" + itemCount,
                    "",
                    "&aLeft-click &7to edit",
                    "&cShift+click &7to delete");
            inv.setItem(i - start, guiItem);
        }

        inv.setItem(45, item(Material.BARRIER, "&c\u00d7 Close Editor"));
        inv.setItem(46, item(Material.LIME_DYE, "&a+ Create New GUI",
                "&7Opens a new empty GUI for editing"));
        inv.setItem(48, item(Material.BOOK, "&7Page &f" + (page + 1) + "&7/&f" + totalPages,
                "&7Total GUIs: &f" + sorted.size()));
        inv.setItem(52, page > 0
                ? item(Material.ARROW, "&c\u25c4 Previous Page")
                : item(Material.GRAY_STAINED_GLASS_PANE, "&8No previous page"));
        inv.setItem(53, page < totalPages - 1
                ? item(Material.ARROW, "&a\u25ba Next Page")
                : item(Material.GRAY_STAINED_GLASS_PANE, "&8No next page"));

        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    public static void openLayoutEditor(SmartMenus plugin, Player player, EditorSession session) {
        int rows = session.getGuiRows();
        int totalSlots = rows * 9;
        int page = session.getCurrentPage();
        int maxPage = session.getMaxPage();
        page = Math.min(page, maxPage);
        session.setCurrentPage(page);

        String title = ColorUtil.color("&8[Editor] &f" + session.getGuiId()
                + " &7[" + rows + "r]" + (maxPage > 0 ? " &7p" + (page + 1) + "/" + (maxPage + 1) : ""));
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int slotOffset = page * 45;
        for (int layoutSlot = 0; layoutSlot < 45; layoutSlot++) {
            int guiSlot = layoutSlot + slotOffset;
            if (guiSlot >= totalSlots) {

                inv.setItem(layoutSlot, item(Material.BLACK_STAINED_GLASS_PANE, "&8Out of range"));
                continue;
            }

            if (session.hasItem(guiSlot)) {
                inv.setItem(layoutSlot, buildItemFromConfig(session, guiSlot));
            } else {
                inv.setItem(layoutSlot, item(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                        "&7Empty Slot &8(#" + guiSlot + ")",
                        "&aLeft-click &7to place held item",
                        "&eRight-click &7to open slot editor"));
            }
        }

        inv.setItem(45, item(Material.ARROW, "&c\u2190 Back to List"));
        inv.setItem(46, item(Material.EMERALD, "&a\ud83d\udcbe Save GUI",
                "&7Saves the config file and reloads"));
        inv.setItem(47, item(Material.COMPARATOR, "&e\u2699 Properties",
                "&7Edit title, rows, commands"));
        inv.setItem(48, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(49, item(Material.PAPER, "&7GUI: &f" + session.getGuiId(),
                "&7Rows: &f" + rows + " | Total slots: &f" + totalSlots));
        inv.setItem(50, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(51, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(52, page > 0
                ? item(Material.RED_DYE, "&c\u25c4 Prev Page")
                : item(Material.GRAY_STAINED_GLASS_PANE, "&8No prev page"));
        inv.setItem(53, page < maxPage
                ? item(Material.LIME_DYE, "&a\u25ba Next Page")
                : item(Material.GRAY_STAINED_GLASS_PANE, "&8No next page"));

        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    @SuppressWarnings("unchecked")
    private static ItemStack buildItemFromConfig(EditorSession session, int guiSlot) {
        var config = session.getConfig();
        String path = session.getItemPath(guiSlot);

        String matName = config.getString(path + ".material", "STONE");
        Material mat;
        try {

            String mn = matName.toUpperCase();
            if (mn.startsWith("HDB:")) mn = "PLAYER_HEAD";
            mat = Material.valueOf(mn);
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String rawName = config.getString(path + ".name", "");
        meta.setDisplayName(rawName.isEmpty()
                ? ColorUtil.color("&f" + matName)
                : ColorUtil.color(rawName));

        List<String> lore = new ArrayList<>();
        List<String> cfgLore = config.getStringList(path + ".lore");
        lore.addAll(ColorUtil.colorList(cfgLore));

        boolean closeOnClick = config.getBoolean(path + ".close", false);
        var actionsList = config.getList(path + ".actions");
        int actionsCount = actionsList != null ? actionsList.size() : 0;
        List<String> cmdList = config.getStringList(path + ".commands");

        lore.add(ColorUtil.color("&8\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
        lore.add(ColorUtil.color("&7Slot: &f" + guiSlot));
        if (closeOnClick) lore.add(ColorUtil.color("&7Close on click: &ayes"));
        if (actionsCount > 0) lore.add(ColorUtil.color("&7Actions: &f" + actionsCount));
        if (!cmdList.isEmpty()) lore.add(ColorUtil.color("&7Commands: &f" + cmdList.size()));
        lore.add(ColorUtil.color("&8\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
        lore.add(ColorUtil.color("&eLeft-click &7to edit"));
        lore.add(ColorUtil.color("&cShift+click &7to remove"));
        lore.add(ColorUtil.color("&bPlace held &7to replace material"));

        meta.setLore(lore);

        if (config.getBoolean(path + ".glow", false)) {
            Enchantment unbreaking = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
            if (unbreaking != null) meta.addEnchant(unbreaking, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static void openBottomLayoutEditor(SmartMenus plugin, Player player, EditorSession session) {

        String title = ColorUtil.color("&8[Editor] &fBottom Items: " + session.getGuiId()
                + " &7(slots 0-35)");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        for (int layoutSlot = 0; layoutSlot < 36; layoutSlot++) {
            if (session.hasItem(layoutSlot)) {
                inv.setItem(layoutSlot, buildItemFromConfig(session, layoutSlot));
            } else {
                String rowHint = bottomSlotHint(layoutSlot);
                inv.setItem(layoutSlot, item(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                        "&7Bottom Slot &8#" + layoutSlot + " &7" + rowHint,
                        "&aLeft-click &7to place held item",
                        "&eRight-click &7to open slot editor"));
            }
        }

        for (int i = 36; i < 45; i++) {
            inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, "&8\u2500\u2500 Bottom Inventory \u2500\u2500"));
        }

        inv.setItem(45, item(Material.ARROW, "&c\u2190 Back to Properties"));
        inv.setItem(46, item(Material.EMERALD, "&a\ud83d\udcbe Save GUI",
                "&7Saves the config file and reloads"));
        inv.setItem(47, item(Material.BOOK, "&7Slot Map",
                "&7  &f0-8   &7\u2192 player inv row 1 (slots 9-17)",
                "&7  &f9-17  &7\u2192 player inv row 2 (slots 18-26)",
                "&7  &f18-26 &7\u2192 player inv row 3 (slots 27-35)",
                "&7  &f27-35 &7\u2192 hotbar (slots 0-8)"));

        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    private static String bottomSlotHint(int slot) {
        if (slot <= 8)  return "(inv row 1)";
        if (slot <= 17) return "(inv row 2)";
        if (slot <= 26) return "(inv row 3)";
        return "(hotbar)";
    }

    public static void openSlotEditor(SmartMenus plugin, Player player, EditorSession session) {
        int slot = session.getSelectedSlot();
        var config = session.getConfig();
        String path = session.getItemPath(slot);

        String matName    = config.getString(path + ".material", "STONE");
        String itemName   = config.getString(path + ".name", "&7(none)");
        List<String> lore = config.getStringList(path + ".lore");
        boolean closeOnClick = config.getBoolean(path + ".close", false);
        boolean glow      = config.getBoolean(path + ".glow", false);
        var actionsList   = config.getList(path + ".actions");
        int actionsCount  = actionsList != null ? actionsList.size() : 0;
        List<String> cmdList = config.getStringList(path + ".commands");
        String btnType    = config.getString(path + ".type", "NONE");
        String srcName    = config.getString(path + ".source", "NONE");
        var condList      = config.getList(path + ".conditions");
        int condCount     = condList != null ? condList.size() : 0;
        var viewReqList   = config.getList(path + ".view-requirements");
        int viewReqCount  = viewReqList != null ? viewReqList.size() : 0;
        boolean hasElse   = config.isSet(path + ".else-item");
        String itemType   = config.getString(path + ".item_type", "vanilla");
        String itemId     = config.getString(path + ".item_id", "&7(none)");
        boolean hasCmd    = config.isSet(path + ".custom_model_data");
        int cmdData       = config.getInt(path + ".custom_model_data", 0);
        boolean autoUpd   = config.getBoolean(path + ".update", false);
        boolean permanent = config.getBoolean(path + ".is-permanent", false);
        boolean takeItem  = config.getBoolean(path + ".take-item", false);
        boolean giveItem  = config.getBoolean(path + ".give-item", false);
        List<String> flags = config.getStringList(path + ".item_flags");

        String backLabel = session.isBottomMode() ? "&c\u2190 Back to Bottom Items" : "&c\u2190 Back to Layout";

        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.color("&8[Editor] &fSlot " + slot + " \u2014 " + matName));

        if (session.hasItem(slot)) {
            Material pm; try { String mn = matName.toUpperCase(); if (mn.startsWith("HDB:")) mn = "PLAYER_HEAD"; pm = Material.valueOf(mn); } catch (Exception e) { pm = Material.STONE; }
            ItemStack preview = new ItemStack(pm);
            ItemMeta meta = preview.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemName.equals("&7(none)") ? ColorUtil.color("&f" + matName) : ColorUtil.color(itemName));
                meta.setLore(new ArrayList<>(ColorUtil.colorList(lore)));
                if (glow) { Enchantment u = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking")); if (u != null) meta.addEnchant(u, 1, true); meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); }
                preview.setItemMeta(meta);
            }
            inv.setItem(0, preview);
        } else {
            inv.setItem(0, item(Material.GRAY_STAINED_GLASS_PANE, "&8Empty Slot #" + slot));
        }
        inv.setItem(2, item(Material.COMPASS,   "&eMaterial: &f" + matName, "&7Click to change"));
        inv.setItem(3, item(Material.NAME_TAG,  "&eName: &f" + itemName, "&7Click to change"));
        inv.setItem(4, item(Material.BOOK,      "&eLore &7(&f" + lore.size() + " lines&7)", "&7Separate lines with &f|"));
        inv.setItem(5, closeOnClick ? item(Material.LIME_CONCRETE,  "&aClose on Click: ON",  "&7Click to disable") : item(Material.RED_CONCRETE, "&cClose on Click: OFF", "&7Click to enable"));
        inv.setItem(6, glow          ? item(Material.BLAZE_POWDER,  "&eGlow: &aON",  "&7Click to disable") : item(Material.GUNPOWDER,     "&7Glow: &cOFF", "&7Click to enable"));
        inv.setItem(8, item(Material.ARROW, backLabel));

        inv.setItem(9,  item(Material.LIGHTNING_ROD,        "&eActions &7(&f" + actionsCount + "&7)",  "&7Click to manage"));
        inv.setItem(10, item(Material.CHAIN_COMMAND_BLOCK,  "&eCommands &7(&f" + cmdList.size() + "&7)", "&7Separate with &f|", "&7Use &f{player}"));
        inv.setItem(11, item(Material.OAK_SIGN,             "&eButton Type: &f" + btnType,   "&aLeft-click &7cycle forward", "&cRight-click &7cycle back"));
        inv.setItem(12, item(Material.FILLED_MAP,           "&eSource: &f" + srcName,         "&aLeft-click &7cycle forward", "&cRight-click &7cycle back"));

        switch (btnType.toUpperCase()) {
            case "JUMP" -> {
                int toPage = config.getInt(path + ".to-page", 0);
                inv.setItem(13, item(Material.BOOK, "&eTo Page: &f" + toPage, "&7Click to set page number"));
            }
            case "SWITCH" -> {
                String switchKey = config.getString(path + ".key", "&7(none)");
                var statesSection = config.getConfigurationSection(path + ".buttons");
                int stateCount = statesSection != null ? statesSection.getKeys(false).size() : 0;
                inv.setItem(13, item(Material.TRIPWIRE_HOOK, "&eSwitch Key: &f" + switchKey, "&7Click to set key"));
                inv.setItem(14, item(Material.COMPARATOR, "&eEdit States &7(&f" + stateCount + "&7)", "&7Click to manage switch states"));
            }
            case "PAGINATION" -> {
                List<String> paginationSlots = config.getStringList(path + ".slots");
                String slotsDisplay = paginationSlots.isEmpty() ? "&7(none)" : String.join(", ", paginationSlots);
                var elements = config.getList(path + ".elements");
                int elemCount = elements != null ? elements.size() : 0;
                inv.setItem(13, item(Material.REPEATER, "&ePagination Slots: &f" + slotsDisplay, "&7Click to set slots (e.g. 10-16,19-25)"));
                inv.setItem(14, item(Material.CHEST, "&eEdit Elements &7(&f" + elemCount + "&7)", "&7Click to manage pagination elements"));
            }
            case "INPUT" -> {
                var onPlaceList = config.getStringList(path + ".on-place");
                boolean removeOnPlace = config.getBoolean(path + ".remove-on-place", false);
                inv.setItem(15, item(Material.HOPPER, "&eOn-Place Commands &7(&f" + onPlaceList.size() + "&7)", "&7Click to set (pipe-separated)"));
                inv.setItem(16, removeOnPlace
                        ? item(Material.LIME_CONCRETE, "&aRemove On Place: ON", "&7Click to disable")
                        : item(Material.RED_CONCRETE, "&cRemove On Place: OFF", "&7Click to enable"));
            }
            default -> {}
        }

        inv.setItem(18, item(Material.GOLD_BLOCK,     "&eConditions &7(&f" + condCount + "&7)",          "&7Requirements to click this item", "&7Click to manage"));
        inv.setItem(19, item(Material.ENDER_EYE,      "&eView Requirements &7(&f" + viewReqCount + "&7)","&7Requirements to SEE this item",    "&7Click to manage"));
        inv.setItem(20, hasElse
                ? item(Material.MAP, "&eElse Item: &aSET", "&7Shown when view req fails", "&aClick to edit")
                : item(Material.MAP, "&eElse Item: &7NONE", "&7Shown when view req fails", "&aClick to set"));

        inv.setItem(27, item(Material.CRAFTING_TABLE, "&eItem Type: &f" + itemType,  "&aLeft-click &7cycle: vanilla\u2192itemsadder\u2192nexo\u2192headdatabase", "&cRight-click &7cycle back"));
        inv.setItem(28, item(Material.FEATHER,        "&eItem ID: &f" + itemId,      "&7For itemsadder/nexo/hdb items", "&7e.g. iasurvival:ruby", "&aClick to set"));
        inv.setItem(29, item(Material.NETHER_STAR,    hasCmd ? "&eCustom Model Data: &f" + cmdData : "&7Custom Model Data: &7not set", "&7For resource pack items", "&aClick to set (0 = remove)"));

        inv.setItem(36, autoUpd   ? item(Material.CLOCK,          "&eAuto-Update: &aON",   "&7Re-renders item each tick",  "&7Click to disable") : item(Material.CLOCK,          "&7Auto-Update: &cOFF", "&7Click to enable"));
        inv.setItem(37, permanent  ? item(Material.BEDROCK,        "&ePermanent: &aON",     "&7Item always shown",          "&7Click to disable") : item(Material.DIRT,           "&7Permanent: &cOFF",  "&7Click to enable"));
        inv.setItem(38, takeItem   ? item(Material.HOPPER,         "&eTake Item: &aON",     "&7Removes item on click",      "&7Click to disable") : item(Material.HOPPER,         "&7Take Item: &cOFF",  "&7Click to enable"));
        inv.setItem(39, giveItem   ? item(Material.CHEST,          "&eGive Item: &aON",     "&7Gives the item on click",    "&7Click to disable") : item(Material.CHEST,          "&7Give Item: &cOFF",  "&7Click to enable"));

        String[] flagNames = {"HIDE_ENCHANTS", "HIDE_ATTRIBUTES", "HIDE_UNBREAKABLE", "HIDE_ADDITIONAL_TOOLTIP"};
        Material[] flagMats = {Material.ENCHANTING_TABLE, Material.IRON_CHESTPLATE, Material.ANVIL, Material.BREWING_STAND};
        for (int i = 0; i < flagNames.length; i++) {
            boolean active = flags.contains(flagNames[i]);
            inv.setItem(45 + i, active
                    ? item(flagMats[i], "&a" + flagNames[i] + ": ON",  "&7Click to disable")
                    : item(flagMats[i], "&7" + flagNames[i] + ": OFF", "&7Click to enable"));
        }
        inv.setItem(53, item(Material.BARRIER, "&c\ud83d\uddd1 Remove Item", "&cClears this slot"));

        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    public static void openPropertiesEditor(SmartMenus plugin, Player player, EditorSession session) {
        Inventory inv = Bukkit.createInventory(null, 36,
                ColorUtil.color("&8[Editor] &fGUI Properties: " + session.getGuiId()));
        var config = session.getConfig();
        String base = session.getBasePath();

        String currentTitle = session.getGuiTitle();
        int currentRows     = session.getGuiRows();
        List<String> cmds   = config.getStringList(base + ".commands");
        boolean useBottom   = config.getBoolean(base + ".use_bottom_inventory", false);
        boolean hasFill     = config.isSet(base + ".fill.material");
        String fillMat      = config.getString(base + ".fill.material", "none");
        String invType      = config.getString(base + ".inventory_type", config.getString(base + ".inventory-type", "CHEST"));
        int npcId           = config.getInt(base + ".npc_id", -1);

        inv.setItem(0, item(Material.NAME_TAG,    "&eGUI Title",     "&7Current: &f" + currentTitle,  "", "&aClick to edit via chat"));
        inv.setItem(1, item(Material.LADDER,      "&eRows: &f" + currentRows, "&7Range: 1-6", "&aLeft-click &7increase", "&cRight-click &7decrease"));
        inv.setItem(2, item(Material.COMMAND_BLOCK, "&eCommands (&f" + cmds.size() + ")",
                cmds.isEmpty() ? "&7(none)" : "&7" + String.join(", ", cmds),
                "", "&aClick to edit via chat", "&7Separate with commas"));
        inv.setItem(3, item(Material.FILLED_MAP,  "&eGUI ID: &f" + session.getGuiId(), "&8Cannot be changed here"));

        inv.setItem(4, useBottom
                ? item(Material.LIME_CONCRETE, "&aBottom Inventory: ON",  "&7Click to disable")
                : item(Material.RED_CONCRETE,  "&cBottom Inventory: OFF", "&7Click to enable"));
        inv.setItem(5, item(Material.CHEST, "&eEdit Bottom Items",
                "&7Configure player inventory area slots",
                useBottom ? "&aEnabled" : "&cEnable Bottom Inventory first"));

        inv.setItem(6, item(Material.CRAFTING_TABLE, "&eInventory Type: &f" + invType,
                "&aLeft-click &7cycle: CHEST\u2192HOPPER\u2192DISPENSER\u2192FURNACE",
                "&cRight-click &7cycle back"));

        inv.setItem(7, npcId >= 0
                ? item(Material.SPAWNER, "&eNPC ID: &f" + npcId, "&7ModeledNPCs binding", "&aClick to change", "&cRight-click to remove")
                : item(Material.SPAWNER, "&7NPC ID: &7not set",  "&7Click to set ModeledNPCs ID"));

        inv.setItem(8, item(Material.ARROW, "&c\u2190 Back to Layout"));

        ItemStack fillDisplay;
        if (hasFill) {
            Material fm = Material.GRAY_STAINED_GLASS_PANE;
            try { fm = Material.valueOf(fillMat.toUpperCase()); } catch (Exception ignored) {}
            fillDisplay = item(fm, "&eFill Item: &f" + fillMat, "&7Background item for empty slots", "&aLeft-click &7place held item as fill", "&cRight-click &7remove fill");
        } else {
            fillDisplay = item(Material.GRAY_STAINED_GLASS_PANE, "&7Fill Item: &7not set", "&7Background for empty slots", "&aLeft-click &7place held item as fill");
        }
        inv.setItem(9, fillDisplay);

        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    @SuppressWarnings("unchecked")
    public static void openActionList(SmartMenus plugin, Player player, EditorSession session) {
        int slot = session.getSelectedSlot();
        String actionsPath = session.getItemPath(slot) + ".actions";
        List<Object> actionsList = (List<Object>) session.getConfig().getList(actionsPath);
        if (actionsList == null) actionsList = new ArrayList<>();

        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.color("&8[Editor] &fActions for Slot " + slot));

        for (int i = 0; i < Math.min(actionsList.size(), 45); i++) {
            Object obj = actionsList.get(i);
            String type = "UNKNOWN";
            List<String> paramLore = new ArrayList<>();
            if (obj instanceof java.util.Map<?, ?> map) {
                Object typeObj = map.get("type");
                type = typeObj != null ? typeObj.toString() : "UNKNOWN";
                for (var entry : map.entrySet()) {
                    if (!"type".equals(String.valueOf(entry.getKey()))) {
                        paramLore.add(ColorUtil.color("&7" + entry.getKey() + "&8: &f" + entry.getValue()));
                    }
                }
            }
            Material mat = actionTypeMaterial(type);
            List<String> lore = new ArrayList<>();
            lore.addAll(paramLore);
            lore.add(ColorUtil.color(""));
            lore.add(ColorUtil.color("&cShift+click &7to remove"));
            ItemStack actionItem = item(mat, "&e" + type);
            ItemMeta meta = actionItem.getItemMeta();
            if (meta != null) { meta.setLore(lore); actionItem.setItemMeta(meta); }
            inv.setItem(i, actionItem);
        }

        inv.setItem(45, item(Material.LIME_DYE, "&a+ Add Action",
                "&7Click to add a new action to this slot"));
        inv.setItem(53, item(Material.ARROW, "&c\u2190 Back to Slot Editor"));

        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    public static void openActionTypePicker(SmartMenus plugin, Player player, EditorSession session) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.color("&8[Editor] &fSelect Action Type"));

        record ActionEntry(String type, Material mat, String desc) {}
        List<ActionEntry> types = List.of(
                new ActionEntry("PLAYER_MESSAGE",   Material.PAPER,               "Send a message to the player"),
                new ActionEntry("BROADCAST",        Material.BELL,                "Broadcast a message to all"),
                new ActionEntry("ACTION_BAR",       Material.KNOWLEDGE_BOOK,      "Display action bar text"),
                new ActionEntry("TITLE",            Material.OAK_SIGN,            "Show a title + subtitle"),
                new ActionEntry("CONSOLE_COMMAND",  Material.COMMAND_BLOCK,       "Run a command as console"),
                new ActionEntry("PLAYER_COMMAND",   Material.REDSTONE,            "Run a command as the player"),
                new ActionEntry("SOUND",            Material.NOTE_BLOCK,          "Play a sound"),
                new ActionEntry("OPEN_GUI",         Material.ENDER_EYE,           "Open another GUI"),
                new ActionEntry("CLOSE",            Material.BARRIER,             "Close the GUI"),
                new ActionEntry("GIVE_MONEY",       Material.EMERALD,             "Give Vault money to player"),
                new ActionEntry("TAKE_MONEY",       Material.GOLD_NUGGET,         "Take Vault money from player"),
                new ActionEntry("GIVE_XP",          Material.EXPERIENCE_BOTTLE,   "Give XP to player"),
                new ActionEntry("EFFECT",           Material.POTION,              "Apply a potion effect"),
                new ActionEntry("SERVER_CONNECT",   Material.END_PORTAL_FRAME,  "BungeeCord server connect")
        );

        for (int i = 0; i < types.size(); i++) {
            var e = types.get(i);
            String[] params = getActionParams(e.type());
            String paramsStr = params.length == 0 ? "&7(no params)" : "&7Params: &f" + String.join(", ", params);
            inv.setItem(i, item(e.mat(), "&e" + e.type(), "&7" + e.desc(), paramsStr));
        }

        inv.setItem(53, item(Material.ARROW, "&c\u2190 Back to Actions"));
        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    @SuppressWarnings("unchecked")
    public static void openConditionList(SmartMenus plugin, Player player, EditorSession session) {
        boolean viewReqs = session.isEditingViewRequirements();
        String condKey   = viewReqs ? ".view-requirements" : ".conditions";
        String heading   = viewReqs ? "View Requirements" : "Conditions";
        int slot         = session.getSelectedSlot();
        String path      = session.getItemPath(slot);

        List<Object> list = (List<Object>) session.getConfig().getList(path + condKey);
        if (list == null) list = new java.util.ArrayList<>();

        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.color("&8[Editor] &f" + heading + " for Slot " + slot));

        for (int i = 0; i < Math.min(list.size(), 45); i++) {
            Object obj = list.get(i);
            String type = "UNKNOWN";
            List<String> paramLore = new java.util.ArrayList<>();
            if (obj instanceof java.util.Map<?, ?> map) {
                Object t = map.get("type"); type = t != null ? t.toString() : "UNKNOWN";
                for (var e : map.entrySet()) {
                    if (!"type".equals(String.valueOf(e.getKey()))) {
                        paramLore.add(ColorUtil.color("&7" + e.getKey() + "&8: &f" + e.getValue()));
                    }
                }
            }
            List<String> lore = new java.util.ArrayList<>(paramLore);
            lore.add(ColorUtil.color(""));
            lore.add(ColorUtil.color("&cShift+click &7to remove"));
            ItemStack ci = item(condTypeMaterial(type), "&e" + type);
            ItemMeta m = ci.getItemMeta(); if (m != null) { m.setLore(lore); ci.setItemMeta(m); }
            inv.setItem(i, ci);
        }

        inv.setItem(45, item(Material.LIME_DYE, "&a+ Add " + heading.replace("s",""),  "&7Click to add a new condition"));
        if (viewReqs) {
            boolean hasElse = session.getConfig().isSet(path + ".else-item");
            inv.setItem(46, hasElse
                    ? item(Material.MAP, "&eElse Item: &aSET", "&7Shown when requirements fail", "&aClick to clear")
                    : item(Material.MAP, "&7Else Item: &7not set", "&7Shown when requirements fail", "&aLeft-click &7place held as else-item"));
        }
        inv.setItem(53, item(Material.ARROW, "&c\u2190 Back to Slot Editor"));

        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    public static void openConditionTypePicker(SmartMenus plugin, Player player, EditorSession session) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.color("&8[Editor] &fSelect Condition Type"));

        record CondEntry(String type, Material mat, String desc, String params) {}
        List<CondEntry> types = List.of(
            new CondEntry("VAULT_MONEY",             Material.EMERALD,          "Vault balance check",            "amount"),
            new CondEntry("PERMISSION",              Material.PAPER,            "Permission node check",          "permission"),
            new CondEntry("XP_LEVEL",                Material.EXPERIENCE_BOTTLE,"XP level check",                 "amount"),
            new CondEntry("XP_POINTS",               Material.EXPERIENCE_BOTTLE,"XP points check",                "amount"),
            new CondEntry("ITEM",                    Material.CHEST,            "Has item in inventory",          "material, amount"),
            new CondEntry("ITEM_CUSTOM_MODEL",        Material.NETHER_STAR,      "Has custom model item",          "material, custom_model_data, amount"),
            new CondEntry("ITEMSADDER",              Material.AMETHYST_SHARD,   "Has ItemsAdder item",            "item_id, amount"),
            new CondEntry("NEXO",                    Material.PRISMARINE_SHARD, "Has Nexo item",                  "item_id, amount"),
            new CondEntry("WEATHER",                 Material.SUNFLOWER,        "World weather check",            "weather (clear/rain/thunder)"),
            new CondEntry("WORLD",                   Material.GRASS_BLOCK,      "Player in world",                "world"),
            new CondEntry("WORLDGUARD_REGION",       Material.MAP,              "Player in WG region",            "region"),
            new CondEntry("PLACEHOLDER_EQUALS",      Material.COMPARATOR,       "PlaceholderAPI equals",          "placeholder, value"),
            new CondEntry("PLACEHOLDER_GREATER_THAN",Material.COMPARATOR,       "PlaceholderAPI greater than",    "placeholder, value"),
            new CondEntry("PLACEHOLDER_LESS_THAN",   Material.COMPARATOR,       "PlaceholderAPI less than",       "placeholder, value"),
            new CondEntry("PLACEHOLDER_CONTAINS",    Material.COMPARATOR,       "PlaceholderAPI contains",        "placeholder, value"),
            new CondEntry("EXPRESSION",              Material.REDSTONE,         "Math/logic expression",          "expression"),
            new CondEntry("SCRIPT",                  Material.REDSTONE_TORCH,   "JavaScript script",              "script"),
            new CondEntry("LUCKPERMS_GROUP",         Material.PLAYER_HEAD,      "LuckPerms group membership",     "group"),
            new CondEntry("OREO_CURRENCY",           Material.GOLD_INGOT,       "OreoEssentials currency",        "currency, amount"),
            new CondEntry("OREO_WARPS",              Material.ENDER_PEARL,      "OreoEssentials warp check",      "warp")
        );

        for (int i = 0; i < types.size(); i++) {
            var e = types.get(i);
            inv.setItem(i, item(e.mat(), "&e" + e.type(), "&7" + e.desc(), "&7Params: &f" + e.params()));
        }

        inv.setItem(53, item(Material.ARROW, "&c\u2190 Back"));
        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    private static Material condTypeMaterial(String type) {
        return switch (type.toUpperCase()) {
            case "VAULT_MONEY"              -> Material.EMERALD;
            case "PERMISSION"               -> Material.PAPER;
            case "XP_LEVEL", "XP_POINTS"   -> Material.EXPERIENCE_BOTTLE;
            case "ITEM"                     -> Material.CHEST;
            case "ITEM_CUSTOM_MODEL"        -> Material.NETHER_STAR;
            case "ITEMSADDER"               -> Material.AMETHYST_SHARD;
            case "NEXO"                     -> Material.PRISMARINE_SHARD;
            case "WEATHER"                  -> Material.SUNFLOWER;
            case "WORLD"                    -> Material.GRASS_BLOCK;
            case "WORLDGUARD_REGION"        -> Material.MAP;
            case "PLACEHOLDER_EQUALS", "PLACEHOLDER_GREATER_THAN",
                 "PLACEHOLDER_LESS_THAN", "PLACEHOLDER_CONTAINS" -> Material.COMPARATOR;
            case "EXPRESSION"               -> Material.REDSTONE;
            case "SCRIPT"                   -> Material.REDSTONE_TORCH;
            case "LUCKPERMS_GROUP"          -> Material.PLAYER_HEAD;
            case "OREO_CURRENCY"            -> Material.GOLD_INGOT;
            case "OREO_WARPS"               -> Material.ENDER_PEARL;
            default                         -> Material.PAPER;
        };
    }

    public static java.util.LinkedList<String> getConditionParamKeys(String type) {
        String[] arr = switch (type.toUpperCase()) {
            case "VAULT_MONEY"               -> new String[]{"amount"};
            case "PERMISSION"                -> new String[]{"permission"};
            case "XP_LEVEL", "XP_POINTS"    -> new String[]{"amount"};
            case "ITEM"                      -> new String[]{"material", "amount"};
            case "ITEM_CUSTOM_MODEL"         -> new String[]{"material", "custom_model_data", "amount"};
            case "ITEMSADDER", "NEXO"        -> new String[]{"item_id", "amount"};
            case "WEATHER"                   -> new String[]{"weather"};
            case "WORLD"                     -> new String[]{"world"};
            case "WORLDGUARD_REGION"         -> new String[]{"region"};
            case "PLACEHOLDER_EQUALS", "PLACEHOLDER_GREATER_THAN",
                 "PLACEHOLDER_LESS_THAN", "PLACEHOLDER_CONTAINS" -> new String[]{"placeholder", "value", "ignore_case"};
            case "EXPRESSION"                -> new String[]{"expression"};
            case "SCRIPT"                    -> new String[]{"script"};
            case "LUCKPERMS_GROUP"           -> new String[]{"group"};
            case "OREO_CURRENCY"             -> new String[]{"currency", "amount"};
            case "OREO_WARPS"                -> new String[]{"warp"};
            default                          -> new String[]{"value"};
        };
        return new java.util.LinkedList<>(java.util.Arrays.asList(arr));
    }

    @SuppressWarnings("unchecked")
    public static void openSwitchStates(SmartMenus plugin, Player player, EditorSession session) {
        int guiSlot = session.getSelectedSlot();
        String path = session.getItemPath(guiSlot);
        var config = session.getConfig();

        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.color("&8[Editor] &fSwitch States: Slot " + guiSlot));

        var statesSection = config.getConfigurationSection(path + ".buttons");
        if (statesSection != null) {
            int i = 0;
            for (String key : statesSection.getKeys(false)) {
                if (i >= 45) break;
                String matName = config.getString(path + ".buttons." + key + ".material", "STONE");
                String stateName = config.getString(path + ".buttons." + key + ".name", key);
                List<String> cmds = config.getStringList(path + ".buttons." + key + ".commands");
                Material mat;
                try { mat = Material.valueOf(matName.toUpperCase()); } catch (Exception e) { mat = Material.STONE; }
                inv.setItem(i++, item(mat, "&e" + key,
                        "&7Material: &f" + matName,
                        "&7Name: &f" + stateName,
                        "&7Commands: &f" + cmds.size(),
                        "",
                        "&cShift+click &7to remove"));
            }
        }

        inv.setItem(45, item(Material.LIME_DYE, "&a+ Add State", "&7Click to add a new switch state"));
        inv.setItem(53, item(Material.ARROW, "&c\u2190 Back to Slot Editor"));

        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    @SuppressWarnings("unchecked")
    public static void openPaginationElements(SmartMenus plugin, Player player, EditorSession session) {
        int guiSlot = session.getSelectedSlot();
        String path = session.getItemPath(guiSlot);
        var config = session.getConfig();

        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.color("&8[Editor] &fPagination Elements: Slot " + guiSlot));

        var rawList = config.getList(path + ".elements");
        if (rawList != null) {
            int i = 0;
            for (Object obj : rawList) {
                if (i >= 45) break;
                String title = "&7(no title)";
                String desc = "&7(no description)";
                if (obj instanceof java.util.Map<?, ?> map) {
                    Object t = map.get("title"); if (t != null) title = t.toString();
                    Object d = map.get("description"); if (d != null) desc = d.toString();
                }
                inv.setItem(i++, item(Material.PAPER, "&e" + title,
                        "&7Description: &f" + desc,
                        "",
                        "&cShift+click &7to remove"));
            }
        }

        inv.setItem(45, item(Material.LIME_DYE, "&a+ Add Element", "&7Click to add a pagination element"));
        inv.setItem(53, item(Material.ARROW, "&c\u2190 Back to Slot Editor"));

        fill(inv);
        player.openInventory(inv);
        session.setOpenInventory(inv);
    }

    private static Material actionTypeMaterial(String type) {
        return switch (type.toUpperCase()) {
            case "PLAYER_MESSAGE"  -> Material.PAPER;
            case "BROADCAST"       -> Material.BELL;
            case "ACTION_BAR"      -> Material.KNOWLEDGE_BOOK;
            case "TITLE"           -> Material.OAK_SIGN;
            case "CONSOLE_COMMAND" -> Material.COMMAND_BLOCK;
            case "PLAYER_COMMAND"  -> Material.REDSTONE;
            case "SOUND"           -> Material.NOTE_BLOCK;
            case "OPEN_GUI"        -> Material.ENDER_EYE;
            case "CLOSE"           -> Material.BARRIER;
            case "GIVE_MONEY"      -> Material.EMERALD;
            case "TAKE_MONEY"      -> Material.GOLD_NUGGET;
            case "GIVE_XP"         -> Material.EXPERIENCE_BOTTLE;
            case "EFFECT"          -> Material.POTION;
            case "SERVER_CONNECT"  -> Material.END_PORTAL_FRAME;
            default                -> Material.PAPER;
        };
    }
}
