package com.oreo.command;

import com.oreo.SmartMenus;
import com.oreo.command.sub.CaptureItemSubCommand;
import com.oreo.command.sub.ConvertSubCommand;
import com.oreo.command.sub.EditorSubCommand;
import com.oreo.command.sub.ItemLevelSubCommand;
import com.oreo.command.sub.OpenSubCommand;
import com.oreo.command.sub.ReloadSubCommand;
import com.oreo.command.sub.SubCommand;
import com.oreo.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SmartMenusCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public SmartMenusCommand(SmartMenus plugin) {
        register(new OpenSubCommand(plugin));
        register(new ReloadSubCommand(plugin));
        register(new CaptureItemSubCommand(plugin));
        register(new EditorSubCommand(plugin));
        register(new ConvertSubCommand(plugin));
        register(new ItemLevelSubCommand(plugin));
    }

    private void register(SubCommand sub) {
        subCommands.put(sub.name(), sub);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            printHelp(sender);
            return true;
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ENGLISH));
        if (sub == null) {
            sender.sendMessage(ColorUtil.color("&cUnknown subcommand: &f" + args[0]));
            sender.sendMessage(ColorUtil.color("&7Use &f/smartmenus &7for help"));
            return true;
        }

        if (!sub.hasPermission(sender)) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission to use that subcommand."));
            return true;
        }

        sub.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (SubCommand sub : subCommands.values()) {
                if (sub.hasPermission(sender)) names.add(sub.name());
            }
            return SubCommand.filter(names, args[0]);
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ENGLISH));
        if (sub == null || !sub.hasPermission(sender)) {
            return Collections.emptyList();
        }
        return sub.tabComplete(sender, args);
    }

    private void printHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&e&lSmart Menus"));
        sender.sendMessage(ColorUtil.color("&7Usage: &f/smartmenus <open|reload|captureitem|editor|convert|itemlevel> [args]"));
        sender.sendMessage(ColorUtil.color("&7Examples:"));
        sender.sendMessage(ColorUtil.color("  &f/smartmenus open shop"));
        sender.sendMessage(ColorUtil.color("  &f/smartmenus open shop PlayerName"));
        sender.sendMessage(ColorUtil.color("  &f/smartmenus reload"));
        sender.sendMessage(ColorUtil.color("  &f/smartmenus captureitem shop 13 &7— capture held item into slot 13 of 'shop'"));
        sender.sendMessage(ColorUtil.color("  &f/smartmenus itemlevel give <player> <item_id> <level>"));
        sender.sendMessage(ColorUtil.color("  &f/smartmenus itemlevel set  <player> <item_id> <level>"));
        sender.sendMessage(ColorUtil.color("  &f/smartmenus itemlevel check [player]"));
        sender.sendMessage(ColorUtil.color("  &f/smartmenus itemlevel list"));
    }
}
