package com.oreo.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public interface BottomInventoryRenderer {
    void open(Player player, GuiDefinition definition);
    void renderSlot(Player player, int playerSlot, ItemStack item);
    void close(Player player, Set<Integer> virtualPlayerSlots);
    BottomInventoryMode getMode();
}
