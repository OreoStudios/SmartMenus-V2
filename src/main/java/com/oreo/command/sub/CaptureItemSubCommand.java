package com.oreo.command.sub;

import com.oreo.SmartMenus;
import com.oreo.gui.GuiDefinition;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CaptureItemSubCommand extends SubCommand {

    public CaptureItemSubCommand(SmartMenus plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "captureitem";
    }

    @Override
    public String primaryPermission() {
        return "smartmenus.captureitem";
    }

    @Override
    public String legacyPermission() {
        return "ogui.captureitem";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg(sender, "&cThis command can only be used by a player.");
            return;
        }
        if (args.length < 3) {
            msg(sender, "&cUsage: /smartmenus captureitem <gui_id> <slot> [item_key]");
            return;
        }

        String guiId = args[1];
        int slot;
        try {
            slot = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            msg(sender, "&cSlot must be a number.");
            return;
        }

        GuiDefinition definition = plugin.getGuiRegistry().getGui(guiId);
        if (definition == null) {
            msg(sender, "&cUnknown GUI: &f" + guiId);
            return;
        }

        File guiFile = plugin.getGuiRegistry().getGuiFile(guiId);
        if (guiFile == null) {
            msg(sender, "&cCannot find the file for GUI: &f" + guiId);
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            msg(sender, "&cHold the item you want to capture in your main hand.");
            return;
        }

        String itemKey = args.length >= 4 ? args[3] : "captured_slot_" + slot;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);

        String basePath = config.isConfigurationSection("guis")
                ? "guis." + guiId + ".items." + itemKey
                : guiId + ".items." + itemKey;

        config.set(basePath + ".slot", slot);
        config.set(basePath + ".material", held.getType().name());

        ItemMeta meta = held.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                config.set(basePath + ".name", meta.getDisplayName().replace('§', '&'));
            }

            if (meta.hasLore() && meta.getLore() != null) {
                List<String> lore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    lore.add(line.replace('§', '&'));
                }
                config.set(basePath + ".lore", lore);
            }

            if (meta.hasCustomModelData()) {
                config.set(basePath + ".custom_model_data", meta.getCustomModelData());
            }

            if (!meta.getItemFlags().isEmpty()) {
                List<String> flags = new ArrayList<>();
                for (ItemFlag flag : meta.getItemFlags()) {
                    flags.add(flag.name());
                }
                config.set(basePath + ".item_flags", flags);
            }

            Map<Enchantment, Integer> enchants = meta.getEnchants();
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                config.set(basePath + ".enchantments." + entry.getKey().getKey().getKey(), entry.getValue());
            }
            config.set(basePath + ".glow", false);

            if (meta instanceof EnchantmentStorageMeta esm) {
                for (Map.Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet()) {
                    config.set(basePath + ".enchantments." + entry.getKey().getKey().getKey(), entry.getValue());
                }
            }
        }

        try {
            config.save(guiFile);
            plugin.reloadGuis();
            msg(sender, "&a✔ Item captured as &f'" + itemKey + "'&a in GUI &f'" + guiId + "'&a at slot &f" + slot + "&a.");
            msg(sender, "&7File: &f" + guiFile.getName());
        } catch (IOException e) {
            msg(sender, "&cFailed to save the GUI file: " + e.getMessage());
            plugin.getLogger().severe("captureitem: failed to save " + guiFile.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return filter(new ArrayList<>(plugin.getGuiRegistry().getGuiIds()), args[1]);
        }
        if (args.length == 3) {
            List<String> slots = new ArrayList<>();
            for (int i = 0; i <= 53; i++) slots.add(String.valueOf(i));
            return filter(slots, args[2]);
        }
        return List.of();
    }
}
