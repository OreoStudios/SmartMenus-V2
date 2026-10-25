package com.oreo.command.sub;

import com.oreo.SmartMenus;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemLevelSubCommand extends SubCommand {

    public ItemLevelSubCommand(SmartMenus plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "itemlevel";
    }

    @Override
    public String primaryPermission() {
        return "smartmenus.itemlevel";
    }

    @Override
    public String legacyPermission() {
        return "ogui.itemlevel";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "&e&lItem Level Commands");
            msg(sender, "&7/smartmenus itemlevel give <player> <item_id> <level>");
            msg(sender, "&7/smartmenus itemlevel set  <player> <item_id> <level>");
            msg(sender, "&7/smartmenus itemlevel check [player]");
            msg(sender, "&7/smartmenus itemlevel list");
            return;
        }

        String subop = args[1].toLowerCase(Locale.ENGLISH);
        switch (subop) {
            case "list" -> executeList(sender);
            case "check" -> executeCheck(sender, args);
            case "give", "set" -> executeGiveOrSet(sender, args, subop);
            default -> {
                msg(sender, "&cUnknown itemlevel subop: &f" + subop);
                msg(sender, "&7Use: give, set, check, list");
            }
        }
    }

    private void executeList(CommandSender sender) {
        msg(sender, "&e&lRegistered Item Upgrades:");
        for (String id : plugin.getItemLevelManager().getItemIds()) {
            int min = plugin.getItemLevelManager().getMinLevel(id);
            int max = plugin.getItemLevelManager().getMaxLevel(id);
            msg(sender, "  &f" + id + " &8— levels &f" + min + "&8-&f" + max);
        }
    }

    private void executeCheck(CommandSender sender, String[] args) {
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

        ItemStack held = target.getInventory().getItemInMainHand();
        String heldId = plugin.getItemLevelManager().getItemId(held);
        if (heldId == null) {
            msg(sender, "&cThis item is not a SmartMenus leveled item.");
            return;
        }
        int heldLevel = plugin.getItemLevelManager().getItemLevel(held);
        int heldMax = plugin.getItemLevelManager().getMaxLevel(heldId);
