package com.oreo.listener;

import com.oreo.SmartMenus;
import com.oreo.gui.BottomInventoryManager;
import com.oreo.gui.BottomInventoryMode;
import com.oreo.gui.BottomInventoryService;
import com.oreo.gui.GuiDefinition;
import com.oreo.gui.GuiItem;
import com.oreo.gui.GuiInventoryProvider;
import com.oreo.util.SmartScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Objects;
import java.util.UUID;

public class BottomInventoryListener implements Listener {

    private final SmartMenus plugin;

    public BottomInventoryListener(SmartMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        UUID id = player.getUniqueId();
        if (!BottomInventoryService.getInstance().isActive(id)) return;

        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();

        int bottomScreen = BottomInventoryManager.rawSlotToBottomScreen(rawSlot, topSize);
        if (bottomScreen >= 0) {
            event.setCancelled(true);

            GuiDefinition def = BottomInventoryService.getInstance().getActiveDefinition(id);
            if (def == null) return;

            GuiItem item = def.getBottomItems().get(bottomScreen);
            if (item == null) return;

            GuiInventoryProvider.executeBottomItemClick(plugin, player, item, event.getClick().name());

            BottomInventoryService.BottomSession sess = BottomInventoryService.getInstance().getSession(id);
            if (sess != null && sess.mode == BottomInventoryMode.PACKET_EVENT) {
                SmartScheduler.runTaskForPlayer(plugin, player, () ->
                        BottomInventoryService.getInstance().refreshVirtualSlots(player));
            }
            return;
        }

        if (event.isShiftClick() && rawSlot < topSize) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!BottomInventoryService.getInstance().isActive(player.getUniqueId())) return;

        int topSize = event.getView().getTopInventory().getSize();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        UUID id = player.getUniqueId();
        if (!BottomInventoryService.getInstance().isActive(id)) return;

        GuiDefinition closingDef = BottomInventoryService.getInstance().getActiveDefinition(id);
        String closingId = closingDef != null ? closingDef.getId() : null;

        SmartScheduler.runTaskForPlayer(plugin, player, () -> {
            GuiDefinition currentDef = BottomInventoryService.getInstance().getActiveDefinition(id);
            String currentId = currentDef != null ? currentDef.getId() : null;
            if (Objects.equals(currentId, closingId)) {
                BottomInventoryService.getInstance().close(player);
            }
        });
    }
}
