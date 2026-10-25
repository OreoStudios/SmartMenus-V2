package com.oreo.command.sub;

import com.oreo.SmartMenus;
import com.oreo.gui.GuiDefinition;
import com.oreo.gui.GuiOpener;
import com.oreo.gui.GuiOpener.OpenOptions;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenSubCommand extends SubCommand {

    public OpenSubCommand(SmartMenus plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "open";
    }

    @Override
    public String primaryPermission() {
        return "smartmenus.open";
    }

    @Override
    public String legacyPermission() {
        return "ogui.open";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "&cUsage: /smartmenus open <id> [player]");
            msg(sender, "&7Available GUIs: &f" + String.join(", ", plugin.getGuiRegistry().getGuiIds()));
            return;
        }

        String id = args[1];
        GuiDefinition definition = plugin.getGuiRegistry().getGui(id);
        if (definition == null) {
            msg(sender, "&cUnknown GUI id: &f" + id);
            msg(sender, "&7Available GUIs: &f" + String.join(", ", plugin.getGuiRegistry().getGuiIds()));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                msg(sender, "&cPlayer not found: &f" + args[2]);
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            msg(sender, "&cYou must specify a player from console.");
            return;
        }

        GuiOpener.open(plugin, target, definition, OpenOptions.unchecked());

        if (!target.equals(sender)) {
            msg(sender, "&aOpened GUI &f" + id + " &afor &f" + target.getName());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return filter(new ArrayList<>(plugin.getGuiRegistry().getGuiIds()), args[1]);
        }
        if (args.length == 3) {
            return filter(onlinePlayerNames(), args[2]);
        }
        return Collections.emptyList();
    }
}
