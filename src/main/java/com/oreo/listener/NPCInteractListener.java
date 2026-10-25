package com.oreo.listener;

import com.oreo.SmartMenus;
import com.oreo.gui.GuiDefinition;
import com.oreo.gui.GuiOpener;
import com.oreo.gui.GuiOpener.OpenOptions;
import fr.elias.npcs.events.NPCInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class NPCInteractListener implements Listener {

    private final SmartMenus plugin;

    public NPCInteractListener(SmartMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNPCInteract(NPCInteractEvent event) {
        Player player = event.getPlayer();
        int npcId = event.getNPCData().getId();
        NPCInteractEvent.InteractionType interactionType = event.getInteractionType();

        if (interactionType != NPCInteractEvent.InteractionType.RIGHT_CLICK) {
            return;
        }

        String guiId = plugin.getGuiRegistry().getGuiByNpc(npcId);
        if (guiId == null) {
            return;
        }

        GuiDefinition definition = plugin.getGuiRegistry().getGui(guiId);
        if (definition == null) {
            plugin.getLogger().warning("GUI '" + guiId + "' is bound to NPC " + npcId + " but doesn't exist!");
            return;
        }

        event.setCancelled(true);

        try {
            if (!GuiOpener.open(plugin, player, definition, OpenOptions.checked())) {
                return;
            }

            Map<String, String> replacements = new HashMap<>();
            replacements.put("gui", guiId);
            replacements.put("id", String.valueOf(npcId));
            plugin.getMessageManager().send(player, "npc.gui_opened", replacements);

            Map<String, String> consoleReplacements = new HashMap<>();
            consoleReplacements.put("player", player.getName());
            consoleReplacements.put("gui", guiId);
            consoleReplacements.put("id", String.valueOf(npcId));
            plugin.getLogger().info(
                    plugin.getMessageManager().getMessage("npc.interaction_logged", consoleReplacements)
            );

        } catch (Exception e) {

            Map<String, String> errorReplacements = new HashMap<>();
            errorReplacements.put("gui", guiId);
            plugin.getMessageManager().send(player, "npc.error_opening_gui", errorReplacements);

            plugin.getLogger().log(Level.SEVERE, "Failed to open GUI '" + guiId + "' for NPC " + npcId + ": " + e.getMessage(), e);
        }
    }
}
