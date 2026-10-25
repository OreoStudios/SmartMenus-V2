package com.oreo.editor;

import com.oreo.SmartMenus;
import com.oreo.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class EditorChatListener implements Listener {

    private final SmartMenus plugin;
    private final EditorManager manager;

    public EditorChatListener(SmartMenus plugin, EditorManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        EditorSession session = manager.getSession(player.getUniqueId());
        if (session == null || !session.isAwaitingChat()) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            session.clearChatMode();
            player.sendMessage(ColorUtil.color("&cInput cancelled."));

            plugin.getServer().getScheduler().runTask(plugin, () -> reopenPreviousScreen(player, session));
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () ->
                manager.handleChatInput(player, session, input));
    }

    private void reopenPreviousScreen(Player player, EditorSession session) {
        switch (session.getScreen()) {
            case LAYOUT      -> EditorScreens.openLayoutEditor(plugin, player, session);
            case SLOT_EDITOR -> EditorScreens.openSlotEditor(plugin, player, session);
            case PROPERTIES  -> EditorScreens.openPropertiesEditor(plugin, player, session);
            case ACTION_LIST -> EditorScreens.openActionList(plugin, player, session);
            default          -> EditorScreens.openGuiList(plugin, player, session);
        }
    }
}
