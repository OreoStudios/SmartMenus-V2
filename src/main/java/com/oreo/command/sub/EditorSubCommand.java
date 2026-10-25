package com.oreo.command.sub;

import com.oreo.SmartMenus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EditorSubCommand extends SubCommand {

    public EditorSubCommand(SmartMenus plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "editor";
    }

    @Override
    public String primaryPermission() {
        return "smartmenus.editor";
    }

    @Override
    public String legacyPermission() {
        return "ogui.editor";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg(sender, "&cThe editor can only be used by players.");
            return;
        }
        plugin.getEditorManager().openEditor(player);
    }
}
