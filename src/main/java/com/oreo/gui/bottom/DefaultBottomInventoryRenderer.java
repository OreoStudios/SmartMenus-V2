package com.oreo.gui.bottom;

import com.oreo.gui.BottomInventoryManager;
import com.oreo.gui.BottomInventoryMode;
import com.oreo.gui.BottomInventoryRenderer;
import com.oreo.gui.GuiDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class DefaultBottomInventoryRenderer implements BottomInventoryRenderer {

    @Override
    public void open(Player player, GuiDefinition definition) {
        
    }

    @Override
    public void renderSlot(Player player, int playerSlot, ItemStack item) {
        player.getInventory().setItem(playerSlot, item);
    }

    @Override
    public void close(Player player, Set<Integer> virtualPlayerSlots) {
        BottomInventoryManager.restoreDefault(player);
    }

    @Override
    public BottomInventoryMode getMode() {
        return BottomInventoryMode.DEFAULT;
    }
}
