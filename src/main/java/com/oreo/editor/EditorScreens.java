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
