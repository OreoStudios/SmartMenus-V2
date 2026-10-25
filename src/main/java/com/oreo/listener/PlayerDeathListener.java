package com.oreo.listener;

import com.oreo.gui.BottomInventoryManager;
import com.oreo.gui.BottomInventoryMode;
import com.oreo.gui.BottomInventoryService;
import com.oreo.gui.GuiDefinition;
import com.oreo.gui.GuiInventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerDeathListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID id = player.getUniqueId();

        boolean hasGui    = GuiInventoryProvider.isGuiOpen(id);
        boolean hasBottom = BottomInventoryService.getInstance().isActive(id);

        if (!hasGui && !hasBottom) return;

        Inventory top = player.getOpenInventory().getTopInventory();
        top.clear();

        if (hasBottom) {
            BottomInventoryService.BottomSession bottomSession = BottomInventoryService.getInstance().getSession(id);

            if (bottomSession != null && bottomSession.mode == BottomInventoryMode.DEFAULT) {
                GuiDefinition def = bottomSession.definition;
                ItemStack[] snap = BottomInventoryManager.getSnapshot(id);

                event.getDrops().removeIf(GuiInventoryProvider::isGuiItem);
                if (!event.getKeepInventory() && snap != null && def != null) {
                    for (int screenSlot : def.getBottomItems().keySet()) {
                        int playerSlot = BottomInventoryManager.bottomScreenToPlayerSlot(screenSlot);
                        if (playerSlot < 0 || playerSlot >= 36) continue;
                        ItemStack real = snap[playerSlot];
                        if (real != null && real.getType() != Material.AIR) {
                            event.getDrops().add(real.clone());
                        }
                    }
                }
                if (def != null) {
                    for (int screenSlot : def.getBottomItems().keySet()) {
                        int playerSlot = BottomInventoryManager.bottomScreenToPlayerSlot(screenSlot);
                        if (playerSlot >= 0) player.getInventory().setItem(playerSlot, null);
                    }
                }
                BottomInventoryManager.restoreDefault(player);
            }
            
            BottomInventoryService.getInstance().clearSession(id);
        }

        GuiInventoryProvider.clearPlayerCache(id);

        event.getDrops().removeIf(GuiInventoryProvider::isGuiItem);
        stripGuiItems(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        stripGuiItems(event.getPlayer());
    }

    private static void stripGuiItems(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (GuiInventoryProvider.isGuiItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
    }
}
