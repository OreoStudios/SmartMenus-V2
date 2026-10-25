package com.oreo.command.sub;

import com.oreo.SmartMenus;
import com.oreo.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * One {@code /smartmenus <name> …} subcommand. Each concrete subcommand owns its own execution and
 * tab-completion logic; {@code SmartMenusCommand} only registers and dispatches to them.
 */
public abstract class SubCommand {

    protected final SmartMenus plugin;

    protected SubCommand(SmartMenus plugin) {
        this.plugin = plugin;
    }

    public abstract String name();

    public abstract String primaryPermission();

    /** Legacy {@code ogui.*} permission kept for backwards compatibility, or {@code null} if none. */
    public String legacyPermission() {
        return null;
    }

    public boolean hasPermission(CommandSender sender) {
        if (sender.hasPermission(primaryPermission())) return true;
        return legacyPermission() != null && sender.hasPermission(legacyPermission());
    }

    /** Runs the subcommand. {@code args[0]} is the subcommand name itself. Permission is already checked. */
    public abstract void execute(CommandSender sender, String[] args);

    /** Tab-completions for this subcommand. {@code args[0]} is the subcommand name. */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    protected static void msg(CommandSender sender, String text) {
        sender.sendMessage(ColorUtil.color(text));
    }

    public static List<String> filter(List<String> options, String input) {
        if (input.isEmpty()) return options;
        String lower = input.toLowerCase(Locale.ENGLISH);
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ENGLISH).startsWith(lower)) filtered.add(option);
        }
        return filtered;
    }

    protected static List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        return names;
    }
}
