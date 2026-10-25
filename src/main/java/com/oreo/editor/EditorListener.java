package com.oreo.editor;

import com.oreo.SmartMenus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class EditorListener implements Listener {

    private final SmartMenus plugin;
    private final EditorManager manager;

    public EditorListener(SmartMenus plugin, EditorManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        EditorSession session = manager.getSession(player.getUniqueId());
        if (session == null) return;
        if (session.getOpenInventory() == null) return;
        if (!event.getInventory().equals(session.getOpenInventory())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        var click = event.getClick();
        var screen = session.getScreen();

        switch (screen) {
            case GUI_LIST      -> EditorHandlers.handleGuiListClick(plugin, player, session, manager, slot, click);
            case LAYOUT        -> EditorHandlers.handleLayoutClick(plugin, player, session, manager, slot, click);
            case BOTTOM_LAYOUT -> EditorHandlers.handleBottomLayoutClick(plugin, player, session, manager, slot, click);
            case SLOT_EDITOR   -> EditorHandlers.handleSlotEditorClick(plugin, player, session, manager, slot, click);
            case PROPERTIES    -> EditorHandlers.handlePropertiesClick(plugin, player, session, manager, slot, click);
            case ACTION_LIST   -> EditorHandlers.handleActionListClick(plugin, player, session, manager, slot, click);
            case ACTION_TYPE   -> EditorHandlers.handleActionTypeClick(plugin, player, session, manager, slot, click);
            case CONDITION_LIST, VIEW_REQ_LIST -> EditorHandlers.handleConditionListClick(plugin, player, session, manager, slot, click);
            case CONDITION_TYPE -> EditorHandlers.handleConditionTypeClick(plugin, player, session, manager, slot, click);
            case SWITCH_STATES -> EditorHandlers.handleSwitchStatesClick(plugin, player, session, manager, slot, click);
            case PAGINATION_ELEMENTS -> EditorHandlers.handlePaginationElementsClick(plugin, player, session, manager, slot, click);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        EditorSession session = manager.getSession(player.getUniqueId());
        if (session == null) return;

        if (session.isAwaitingChat()) return;

        if (session.getOpenInventory() != null && !event.getInventory().equals(session.getOpenInventory())) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            EditorSession s = manager.getSession(player.getUniqueId());
            if (s != null && !s.isAwaitingChat() && (s.getOpenInventory() == null || !player.getOpenInventory().getTopInventory().equals(s.getOpenInventory()))) {
                manager.removeSession(player.getUniqueId());
            }
        }, 2L);
    }
}
