package com.oreo.listener;

import com.oreo.gui.ArgManager;
import com.oreo.gui.BottomInventoryService;
import com.oreo.gui.GuiInventoryProvider;
import com.oreo.gui.NavigationManager;
import com.oreo.gui.PageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        GuiInventoryProvider.clearPlayerCache(id);
        NavigationManager.clearAll(id);
        PageManager.clearPlayer(id);
        ArgManager.clear(id);

        if (BottomInventoryService.getInstance().isActive(id)) {
            
            BottomInventoryService.getInstance().clearSession(id);
        }
    }
}
