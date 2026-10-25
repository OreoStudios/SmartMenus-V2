package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class NexoCondition implements Condition {
    private final SmartMenus plugin;
    private final String itemId;
    private final int amount;

    public NexoCondition(SmartMenus plugin, String itemId, int amount) {
        this.plugin = plugin;
        this.itemId = itemId;
        this.amount = amount;
    }

    @Override
    public boolean check(Player player) {
        if (!isAvailable()) return false;
        return countItems(player) >= amount;
    }

    @Override
    public boolean take(Player player) {
        if (!check(player)) return false;
        int remaining = amount;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            try {
                Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
                String id = (String) nexoItemsClass.getMethod("idFromItem", ItemStack.class).invoke(null, item);

                if (id == null || !id.equals(itemId)) continue;

                int itemAmount = item.getAmount();
                if (itemAmount >= remaining) {
                    item.setAmount(itemAmount - remaining);
                    return true;
                } else {
                    remaining -= itemAmount;
                    item.setAmount(0);
                }
            } catch (Exception ignored) {
                // Not a Nexo item (or the API is absent); skip this stack.
            }
        }
        return remaining <= 0;
    }

    @Override
    public String getErrorMessage(Player player) {
        if (!isAvailable()) {
            return plugin.getMessageManager().getMessage("conditions.nexo.unavailable", player);
        }

        Map<String, String> replacements = new HashMap<>();
        replacements.put("amount", String.valueOf(amount));
        replacements.put("item", itemId);
        replacements.put("current", String.valueOf(countItems(player)));

        return plugin.getMessageManager().getMessage("conditions.nexo.insufficient", player, replacements);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.NEXO;
    }

    private boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("Nexo") != null;
    }

    private int countItems(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            try {
                Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
                String id = (String) nexoItemsClass.getMethod("idFromItem", ItemStack.class).invoke(null, item);

                if (id != null && id.equals(itemId)) {
                    count += item.getAmount();
                }
            } catch (Exception ignored) {
                // Not a Nexo item (or the API is absent); skip this stack.
            }
        }
        return count;
    }
}
