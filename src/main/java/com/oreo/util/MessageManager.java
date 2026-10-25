package com.oreo.util;

import com.oreo.SmartMenus;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final SmartMenus plugin;
    private FileConfiguration lang;
    private final Map<String, String> cache = new HashMap<>();
    private boolean placeholderAPIAvailable = false;

    public MessageManager(SmartMenus plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");

        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        lang = YamlConfiguration.loadConfiguration(langFile);
        cache.clear();

        placeholderAPIAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        plugin.getLogger().info("Loaded language file: lang.yml");
        if (placeholderAPIAvailable) {
            plugin.getLogger().info("PlaceholderAPI integration enabled for messages");
        }
    }

    public String getMessage(String path) {
        String message = lang.getString(path);

        if (message == null) {
            plugin.getLogger().warning("Message not found in lang.yml: " + path);
            return path;
        }

        return ColorUtil.color(message);
    }

    public String getMessage(String path, Map<String, String> replacements) {
        String message = getMessage(path);

        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return message;
    }

    public String getMessage(String path, Player player) {
        String message = getMessage(path);
        return parsePlaceholders(player, message);
    }

    public String getMessage(String path, Player player, Map<String, String> replacements) {
        String message = getMessage(path, replacements);
        return parsePlaceholders(player, message);
    }

    private String parsePlaceholders(Player player, String message) {
        if (!placeholderAPIAvailable || player == null) {
            return message;
        }

        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            message = (String) papiClass.getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(null, player, message);
        } catch (Exception ignored) {
            // PlaceholderAPI missing or a placeholder threw; keep the raw message.
        }

        return message;
    }

    public void send(CommandSender sender, String path) {
        if (sender instanceof Player) {
            sender.sendMessage(getMessage(path, (Player) sender));
        } else {
            sender.sendMessage(getMessage(path));
        }
    }

    public void send(CommandSender sender, String path, Map<String, String> replacements) {
        if (sender instanceof Player) {
            sender.sendMessage(getMessage(path, (Player) sender, replacements));
        } else {
            sender.sendMessage(getMessage(path, replacements));
        }
    }

    public String getPrefix() {
        return getMessage("general.prefix");
    }

    public boolean isPlaceholderAPIAvailable() {
        return placeholderAPIAvailable;
    }
}
