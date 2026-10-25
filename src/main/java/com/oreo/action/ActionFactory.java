package com.oreo.action;

import com.oreo.SmartMenus;
import com.oreo.gui.GuiDefinition;
import com.oreo.gui.GuiOpener;
import com.oreo.gui.GuiOpener.OpenOptions;
import com.oreo.gui.NavigationManager;
import com.oreo.util.ColorUtil;
import com.oreo.util.Ids;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

public class ActionFactory {

    private static final Random RANDOM = new Random();

    private static Map<String, Function<Map<String, Object>, Action>> customRegistry = null;

    public static void setCustomRegistry(Map<String, Function<Map<String, Object>, Action>> registry) {
        customRegistry = registry;
    }

    @SuppressWarnings("unchecked")
    public static List<Action> parseActions(List<?> raw) {
        List<Action> actions = new ArrayList<>();
        if (raw == null) return actions;
        for (Object obj : raw) {
            if (obj instanceof Map) {
                try {
                    Action a = parseAction((Map<String, Object>) obj);
                    if (a != null) actions.add(a);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[SmartMenus] Failed to parse action entry: " + e.getMessage());
                }
            }
        }
        return actions;
    }

    public static Action parseAction(Map<String, Object> map) {
        if (map == null) return null;
        Object typeObj = map.get("type");
        if (typeObj == null) return null;
        String type = typeObj.toString().toUpperCase();

        return switch (type) {
            case "PLAYER_MESSAGE" -> new PlayerMessageAction(str(map, "message", ""));
            case "BROADCAST" -> new BroadcastAction(str(map, "message", ""));
            case "ACTION_BAR" -> new ActionBarAction(str(map, "message", ""));
            case "TITLE" -> new TitleAction(
                    str(map, "title", ""),
                    str(map, "subtitle", ""),
                    intVal(map, "fade-in", 10),
                    intVal(map, "stay", 40),
                    intVal(map, "fade-out", 10));
            case "SOUND" -> new SoundAction(
                    str(map, "sound", "UI_BUTTON_CLICK"),
                    floatVal(map, "volume", 1.0f),
                    floatVal(map, "pitch", 1.0f),
                    false);
            case "BROADCAST_SOUND" -> new SoundAction(
                    str(map, "sound", "UI_BUTTON_CLICK"),
                    floatVal(map, "volume", 1.0f),
                    floatVal(map, "pitch", 1.0f),
                    true);
            case "TELEPORT" -> new TeleportAction(
                    str(map, "world", "world"),
                    doubleVal(map, "x", 0.0),
                    doubleVal(map, "y", 64.0),
                    doubleVal(map, "z", 0.0),
                    floatVal(map, "yaw", 0.0f),
                    floatVal(map, "pitch", 0.0f));
            case "CONSOLE_COMMAND" -> new ConsoleCommandAction(str(map, "command", ""));
            case "PLAYER_COMMAND" -> new PlayerCommandAction(str(map, "command", ""));
            case "RANDOM_CONSOLE_COMMAND" -> new RandomConsoleCommandAction(strList(map, "commands"));
            case "RANDOM_PLAYER_COMMAND" -> new RandomPlayerCommandAction(strList(map, "commands"));
            case "CHANCE_REWARD" -> new ChanceRewardAction(rewardList(map, "rewards"));
            case "OPEN_INVENTORY" -> new OpenInventoryAction(str(map, "inventory", ""));
            case "OPEN_GUI" -> new OpenInventoryAction(str(map, "gui", str(map, "inventory", "")));
            case "CLOSE_INVENTORY" -> new CloseInventoryAction();
            case "OPEN_BEDROCK_FORM" -> new OpenBedrockFormAction(str(map, "inventory", str(map, "gui", "")));
            case "OPEN_BEDROCK_DIALOGUE" -> new OpenBedrockDialogueAction(str(map, "inventory", str(map, "gui", "")));
            case "BACK" -> new BackAction();
            case "SERVER_CONNECT" -> new ServerConnectAction(str(map, "server", ""));
            case "SCRIPT" -> new ScriptAction(str(map, "script", ""));
            default -> parseCustomAction(type, map);
        };
    }

    private static Action parseCustomAction(String type, Map<String, Object> map) {
        if (customRegistry == null) return null;
        Function<Map<String, Object>, Action> factory = customRegistry.get(type);
        if (factory == null) return null;
        try {
            return factory.apply(map);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SmartMenus] Custom action type '" + type + "' threw: " + e.getMessage());
            return null;
        }
    }

    private static String str(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value != null ? value.toString() : fallback;
    }

    private static int intVal(Map<String, Object> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) return number.intValue();
        if (value != null) {
            try { return Integer.parseInt(value.toString()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private static float floatVal(Map<String, Object> map, String key, float fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) return number.floatValue();
        if (value != null) {
            try { return Float.parseFloat(value.toString()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private static double doubleVal(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) return number.doubleValue();
        if (value != null) {
            try { return Double.parseDouble(value.toString()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private static List<String> strList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object element : list) {
                if (element != null) result.add(element.toString());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rewardList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object element : list) {
                if (element instanceof Map) result.add((Map<String, Object>) element);
            }
        }
        return result;
    }

    public static String replaceVars(String text, Player player, Map<String, String> vars) {
        if (text == null) return "";
        if (player != null) text = text.replace("{player}", player.getName());
        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                text = text.replace("%" + entry.getKey() + "%", entry.getValue() != null ? entry.getValue() : "");
            }
        }
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                text = PlaceholderAPI.setPlaceholders(player, text);
            } catch (NoClassDefFoundError | RuntimeException ignored) {
                // PlaceholderAPI not loaded or a placeholder expansion failed; leave the text as-is.
            }
        }
        return text;
    }

    public static final class ConsoleCommandAction implements Action {
        private final String command;
        public ConsoleCommandAction(String command) { this.command = command; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            String cmd = replaceVars(command, player, vars);
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private static final class PlayerMessageAction implements Action {
        private final String message;
        PlayerMessageAction(String message) { this.message = message; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            player.sendMessage(ColorUtil.color(replaceVars(message, player, vars)));
        }
    }

    private static final class BroadcastAction implements Action {
        private final String message;
        BroadcastAction(String message) { this.message = message; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            Bukkit.broadcastMessage(ColorUtil.color(replaceVars(message, player, vars)));
        }
    }

    private static final class ActionBarAction implements Action {
        private final String message;
        ActionBarAction(String message) { this.message = message; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            String msg = ColorUtil.color(replaceVars(message, player, vars));
            Component component =
                    LegacyComponentSerializer
                            .legacySection().deserialize(msg);
            ((Audience) player).sendActionBar(component);
        }
    }

    private static final class TitleAction implements Action {
        private final String title, subtitle;
        private final int fadeIn, stay, fadeOut;
        TitleAction(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            this.title = title; this.subtitle = subtitle;
            this.fadeIn = fadeIn; this.stay = stay; this.fadeOut = fadeOut;
        }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            LegacyComponentSerializer legacy =
                    LegacyComponentSerializer.legacySection();
            Title adventureTitle = Title.title(
                    legacy.deserialize(ColorUtil.color(replaceVars(title, player, vars))),
                    legacy.deserialize(ColorUtil.color(replaceVars(subtitle, player, vars))),
                    Title.Times.times(
                            Duration.ofMillis(fadeIn * 50L),
                            Duration.ofMillis(stay * 50L),
                            Duration.ofMillis(fadeOut * 50L)
                    )
            );
            ((Audience) player).showTitle(adventureTitle);
        }
    }

    private static final class SoundAction implements Action {
        private final String soundName;
        private final float volume, pitch;
        private final boolean broadcast;
        SoundAction(String soundName, float volume, float pitch, boolean broadcast) {
            this.soundName = soundName; this.volume = volume; this.pitch = pitch;
            this.broadcast = broadcast;
        }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            Sound sound;
            try {
                sound = Sound.valueOf(replaceVars(soundName, player, vars).toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown sound: " + soundName);
                return;
            }
            if (broadcast) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, volume, pitch);
                }
            } else {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
    }

    private static final class TeleportAction implements Action {
        private final String world;
        private final double x, y, z;
        private final float yaw, pitch;
        TeleportAction(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            World w = Bukkit.getWorld(replaceVars(world, player, vars));
            if (w == null) {
                plugin.getLogger().warning("Teleport: world not found: " + world);
                return;
            }
            player.teleport(new Location(w, x, y, z, yaw, pitch));
        }
    }

    private static final class PlayerCommandAction implements Action {
        private final String command;
        PlayerCommandAction(String command) { this.command = command; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            String cmd = replaceVars(command, player, vars);
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            player.performCommand(cmd);
        }
    }

    private static final class RandomConsoleCommandAction implements Action {
        private final List<String> commands;
        RandomConsoleCommandAction(List<String> commands) { this.commands = commands; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            if (commands.isEmpty()) return;
            String cmd = replaceVars(commands.get(RANDOM.nextInt(commands.size())), player, vars);
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private static final class RandomPlayerCommandAction implements Action {
        private final List<String> commands;
        RandomPlayerCommandAction(List<String> commands) { this.commands = commands; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            if (commands.isEmpty()) return;
            String cmd = replaceVars(commands.get(RANDOM.nextInt(commands.size())), player, vars);
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            player.performCommand(cmd);
        }
    }

    private static final class OpenInventoryAction implements Action {
        private final String guiId;
        OpenInventoryAction(String guiId) { this.guiId = guiId; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            String id = replaceVars(guiId, player, vars);
            GuiDefinition def = plugin.getGuiRegistry().getGui(id);
            if (def == null) {

                String normalizedId = Ids.slugify(id);
                if (!normalizedId.equals(id)) {
                    def = plugin.getGuiRegistry().getGui(normalizedId);
                }
            }
            if (def == null) {
                plugin.getLogger().warning("OPEN_GUI: GUI not found: '" + id
                        + "'. Known GUIs: " + plugin.getGuiRegistry().getGuiIds());
                return;
            }
            GuiOpener.open(plugin, player, def, OpenOptions.checked());
        }
    }

    private static final class CloseInventoryAction implements Action {
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            player.closeInventory();
        }
    }

    public static final class OpenBedrockFormAction implements Action {
        private final String guiId;
        public OpenBedrockFormAction(String guiId) { this.guiId = guiId; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            String id = replaceVars(guiId, player, vars);
            GuiDefinition def = plugin.getGuiRegistry().getGui(id);
            if (def == null) {
                plugin.getLogger().warning("OPEN_BEDROCK_FORM: GUI not found: " + id);
                return;
            }
            GuiOpener.open(plugin, player, def, OpenOptions.unchecked());
        }
    }

    public static final class OpenBedrockDialogueAction implements Action {
        private final String guiId;
        public OpenBedrockDialogueAction(String guiId) { this.guiId = guiId; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            String id = replaceVars(guiId, player, vars);
            GuiDefinition def = plugin.getGuiRegistry().getGui(id);
            if (def == null) {
                plugin.getLogger().warning("OPEN_BEDROCK_DIALOGUE: GUI not found: " + id);
                return;
            }
            GuiOpener.open(plugin, player, def, OpenOptions.uncheckedNoBedrockConvert());
        }
    }

    private static final class BackAction implements Action {
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            UUID playerId = player.getUniqueId();

            // Drop the current menu, then reopen the previous one from the navigation history.
            NavigationManager.pop(playerId);
            String previous = NavigationManager.pop(playerId);
            if (previous == null) {
                player.closeInventory();
                return;
            }

            GuiDefinition def = plugin.getGuiRegistry().getGui(previous);
            if (def == null) {
                player.closeInventory();
                return;
            }
            // Reopening re-pushes the target, so the history stays consistent.
            GuiOpener.open(plugin, player, def, OpenOptions.unchecked());
        }
    }

    private static final class ServerConnectAction implements Action {
        private final String server;
        ServerConnectAction(String server) { this.server = server; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            String srv = replaceVars(server, player, vars);
            try {
                java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
                java.io.DataOutputStream out = new java.io.DataOutputStream(bout);
                out.writeUTF("Connect");
                out.writeUTF(srv);
                player.sendPluginMessage(plugin, "BungeeCord", bout.toByteArray());
            } catch (Exception e) {
                plugin.getLogger().warning("SERVER_CONNECT failed: " + e.getMessage());
            }
        }
    }

    private static final class ScriptAction implements Action {
        private final String script;
        ScriptAction(String script) { this.script = script; }
        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            plugin.getScriptEngine().evalAction(script, player);
        }
    }

    public static final class ChanceRewardAction implements Action {
        private final List<Map<String, Object>> rewards;

        public ChanceRewardAction(List<Map<String, Object>> rewards) {
            this.rewards = rewards;
        }

        @Override
        public void execute(SmartMenus plugin, Player player, Map<String, String> vars) {
            if (rewards.isEmpty()) return;
            double roll = RANDOM.nextDouble() * 100.0;
            double cumulative = 0.0;
            for (Map<String, Object> reward : rewards) {
                cumulative += doubleVal(reward, "chance", 0.0);
                if (roll < cumulative) {
                    for (String cmd : strList(reward, "commands")) {
                        String resolved = replaceVars(cmd, player, vars);
                        if (resolved.startsWith("/")) resolved = resolved.substring(1);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
                    }
                    for (String msg : strList(reward, "messages")) {
                        Component component =
                                ColorUtil.colorComponent(replaceVars(msg, player, vars));
                        ((Audience) player).sendMessage(component);
                    }
                    return;
                }
            }

        }
    }
}
