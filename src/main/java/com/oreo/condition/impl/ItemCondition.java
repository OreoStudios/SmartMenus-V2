package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import com.oreo.gui.BottomInventoryManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class ItemCondition implements Condition {
    private final SmartMenus plugin;
    private final Material material;
    private final int amount;
    private final Integer customModelData;

    public ItemCondition(SmartMenus plugin, Material material, int amount) {
        this(plugin, material, amount, null);
    }

    public ItemCondition(SmartMenus plugin, Material material, int amount, Integer customModelData) {
        this.plugin = plugin;
        this.material = material;
        this.amount = amount;
        this.customModelData = customModelData;
    }

    @Override
    public boolean check(Player player) {
        return countItems(player) >= amount;
    }

    @Override
    public boolean take(Player player) {
        if (!check(player)) return false;
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            if (customModelData != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.hasCustomModelData() ||
                        meta.getCustomModelData() != customModelData) continue;
            }

            int itemAmount = item.getAmount();
            if (itemAmount > remaining) {
                item.setAmount(itemAmount - remaining);
                player.getInventory().setItem(i, item);

                BottomInventoryManager.updateSnapshotSlot(player, i, item);
                remaining = 0;
                break;
            } else {
                remaining -= itemAmount;
                player.getInventory().setItem(i, null);

                BottomInventoryManager.updateSnapshotSlot(player, i, null);
                if (remaining == 0) break;
            }
        }

        player.updateInventory();
        return remaining <= 0;
    }

    @Override
    public String getErrorMessage(Player player) {
        int has = countItems(player);
        String itemName = material.name().toLowerCase().replace("_", " ");
        if (customModelData != null) {
            itemName += " (CMD: " + customModelData + ")";
        }

        Map<String, String> replacements = new HashMap<>();
        replacements.put("amount", String.valueOf(amount));
        replacements.put("item", itemName);
        replacements.put("current", String.valueOf(has));

        return plugin.getMessageManager().getMessage("conditions.item.insufficient", player, replacements);
    }

    @Override
    public ConditionType getType() {
        return customModelData != null ? ConditionType.ITEM_CUSTOM_MODEL : ConditionType.ITEM;
    }

    private int countItems(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            if (customModelData != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.hasCustomModelData() ||
                        meta.getCustomModelData() != customModelData) continue;
            }
            count += item.getAmount();
        }
        return count;
    }
}
