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
        msg(sender, "&e&lItem Level Info &8[" + target.getName() + "]");
        msg(sender, "  &7Item ID: &f" + heldId);
        msg(sender, "  &7Level:   &f" + heldLevel + " &8/ &f" + heldMax);
    }

    private void executeGiveOrSet(CommandSender sender, String[] args, String subop) {
        if (args.length < 5) {
            msg(sender, "&cUsage: /smartmenus itemlevel " + subop + " <player> <item_id> <level>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            msg(sender, "&cPlayer not found: &f" + args[2]);
            return;
        }
        String itemId = args[3];
        int level;
        try {
            level = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            msg(sender, "&cLevel must be a number.");
            return;
        }
        int max = plugin.getItemLevelManager().getMaxLevel(itemId);

        if (subop.equals("give")) {
            boolean ok = plugin.getItemLevelManager().give(target, itemId, level);
            if (!ok) {
                msg(sender, "&cUnknown item id '" + itemId + "' or level " + level + ".");
                return;
            }
            msg(sender, "&a✔ Gave &f" + target.getName() + " &a→ &f" + itemId + " &8[Level " + level + "/" + max + "]");
        } else {
            if (plugin.getItemLevelManager().getItem(itemId, level) == null) {
                msg(sender, "&cUnknown item id '" + itemId + "' or level " + level + ".");
                return;
            }
            boolean replaced = plugin.getItemLevelManager().set(target, itemId, level);
            String action = replaced ? "Upgraded" : "Gave (not found in inventory)";
            msg(sender, "&a✔ " + action + ": &f" + target.getName() + " &a→ &f" + itemId + " &8[Level " + level + "/" + max + "]");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return filter(List.of("give", "set", "check", "list"), args[1]);
        }

        String subop = args[1].toLowerCase(Locale.ENGLISH);

        if (args.length == 3 && (subop.equals("give") || subop.equals("set") || subop.equals("check"))) {
            return filter(onlinePlayerNames(), args[2]);
        }

        if (args.length == 4 && (subop.equals("give") || subop.equals("set"))) {
            return filter(new ArrayList<>(plugin.getItemLevelManager().getItemIds()), args[3]);
        }

        if (args.length == 5 && (subop.equals("give") || subop.equals("set"))) {
            String itemId = args[3];
            List<String> levels = new ArrayList<>();
            for (int lvl : plugin.getItemLevelManager().getLevels(itemId)) {
                levels.add(String.valueOf(lvl));
            }
            return filter(levels, args[4]);
        }

        return List.of();
    }
}
