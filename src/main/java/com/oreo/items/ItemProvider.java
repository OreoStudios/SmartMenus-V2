package com.oreo.items;

import org.bukkit.inventory.ItemStack;

public interface ItemProvider {

    ItemStack getItem(String material, String itemType, Integer customModelData);

    boolean isAvailable(String itemType);
}
