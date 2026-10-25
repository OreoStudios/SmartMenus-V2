package com.oreo.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BottomInventoryManager {

    private static final Map<UUID, ItemStack[]> savedInventories = new ConcurrentHashMap<>();

    private static final Map<UUID, GuiDefinition> activeDefinitions = new ConcurrentHashMap<>();

    private BottomInventoryManager() {}

    public static void activateDefault(Player player, GuiDefinition definition) {
        UUID id = player.getUniqueId();

        if (!savedInventories.containsKey(id)) {
            ItemStack[] inv = player.getInventory().getContents();
            ItemStack[] saved = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                saved[i] = inv[i] != null ? inv[i].clone() : null;
            }
            savedInventories.put(id, saved);
        }
        activeDefinitions.put(id, definition);
    }

    public static void restoreDefault(Player player) {
        UUID id = player.getUniqueId();
        ItemStack[] saved = savedInventories.remove(id);
        activeDefinitions.remove(id);
        if (saved == null) return;

        for (int i = 0; i < 36; i++) {
            ItemStack item = saved[i];

            if (GuiInventoryProvider.isGuiItem(item)) item = null;
            player.getInventory().setItem(i, item);
        }
    }

    public static void updateSnapshotSlot(Player player, int slot, ItemStack item) {
        if (slot < 0 || slot >= 36) return;
        UUID id = player.getUniqueId();
        ItemStack[] saved = savedInventories.get(id);
        if (saved == null) return;
        saved[slot] = item != null ? item.clone() : null;
    }

    public static void clearWithoutRestore(UUID id) {
        savedInventories.remove(id);
        activeDefinitions.remove(id);
    }

    public static void clearActiveDefinition(UUID id) {
        activeDefinitions.remove(id);
    }

    public static boolean isActive(UUID id) {
        return activeDefinitions.containsKey(id);
    }

    public static GuiDefinition getActiveDefinition(UUID id) {
        return activeDefinitions.get(id);
    }

    public static ItemStack[] getSnapshot(UUID id) {
        return savedInventories.get(id);
    }

    public static int bottomScreenToPlayerSlot(int screenSlot) {
        if (screenSlot >= 0 && screenSlot <= 26) return screenSlot + 9;
        if (screenSlot >= 27 && screenSlot <= 35) return screenSlot - 27;
        return -1;
    }

    public static int rawSlotToBottomScreen(int rawSlot, int topInventorySize) {
        int screen = rawSlot - topInventorySize;
        if (screen >= 0 && screen <= 35) return screen;
        return -1;
    }
}
