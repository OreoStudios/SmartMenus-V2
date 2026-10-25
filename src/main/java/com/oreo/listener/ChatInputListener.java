package com.oreo.listener;

import com.oreo.SmartMenus;
import com.oreo.action.Action;
import com.oreo.gui.ChatInputManager;
import com.oreo.util.ColorUtil;
import com.oreo.util.SmartScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

public class ChatInputListener implements Listener {

    private final SmartMenus plugin;

    public ChatInputListener(SmartMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!ChatInputManager.isAwaiting(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        ChatInputManager.PendingInput pending = ChatInputManager.get(player.getUniqueId());

        if (pending == null) {
            ChatInputManager.remove(player.getUniqueId());
            return;
        }

        String input = event.getMessage().trim();
        String cancelWord = pending.cancelWord() != null ? pending.cancelWord() : "cancel";

        if (input.equalsIgnoreCase(cancelWord)) {
            ChatInputManager.remove(player.getUniqueId());
            player.sendMessage(ColorUtil.color("&cInput cancelled."));
            return;
        }

        String inputType = pending.inputType() != null ? pending.inputType().toUpperCase() : "TEXT";
        switch (inputType) {
            case "NUMBER": {
                try {
                    Double.parseDouble(input);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.color("&cPlease enter a valid number."));
                    return;
                }
                break;
            }
            case "ONLINE_PLAYER": {
                Player target = Bukkit.getPlayer(input);
                if (target == null) {
                    player.sendMessage(ColorUtil.color("&cPlayer '" + input + "' is not online."));
                    return;
                }
                break;
            }
            default:
                break;
        }

        final String finalInput = input;
        ChatInputManager.remove(player.getUniqueId());

        SmartScheduler.runTaskForPlayer(plugin, player, () -> {
            Map<String, String> vars = new HashMap<>(pending.baseVars() != null ? pending.baseVars() : Map.of());
            vars.put("input", finalInput);
            vars.put("player", player.getName());
            for (Action action : pending.onComplete()) {
                try {
                    action.execute(plugin, player, vars);
                } catch (Exception e) {
                    plugin.getLogger().warning("ChatInput action failed: " + e.getMessage());
                }
            }
        });
    }
}
