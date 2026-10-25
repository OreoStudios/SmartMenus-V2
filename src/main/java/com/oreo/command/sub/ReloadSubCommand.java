package com.oreo.command.sub;

import com.oreo.SmartMenus;
import org.bukkit.command.CommandSender;

public class ReloadSubCommand extends SubCommand {

    public ReloadSubCommand(SmartMenus plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String primaryPermission() {
        return "smartmenus.reload";
    }

    @Override
    public String legacyPermission() {
        return "ogui.reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reloadGuis();
        msg(sender, "&a✔ Smart Menus reloaded successfully!");
        msg(sender, "&7Loaded &f" + plugin.getGuiRegistry().getGuiIds().size() + " &7GUI(s)");
    }
}
