package com.oreo.listener;

import com.oreo.SmartMenus;
import com.oreo.action.Action;
import com.oreo.gui.ButtonType;
import com.oreo.gui.GuiDefinition;
import com.oreo.gui.GuiInventoryProvider;
import com.oreo.gui.GuiItem;
import fr.minuskube.inv.ClickableItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputSlotListener implements Listener {

    private final SmartMenus plugin;

    public InputSlotListener(SmartMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        GuiDefinition def = GuiInventoryProvider.getActiveGui(player.getUniqueId());
        if (def == null) return;

        int slot = event.getSlot();
        GuiItem guiItem = def.getItems().get(slot);
        if (guiItem == null || guiItem.getButtonType() != ButtonType.INPUT) return;

        InventoryAction action = event.getAction();
        ItemStack cursor = event.getCursor();
        ItemStack currentInSlot = event.getCurrentItem();

        if (isPickupAction(action)) {
            if (currentInSlot != null
                    && currentInSlot.getType() != Material.AIR
                    && !GuiInventoryProvider.isGuiItem(currentInSlot)) {
                event.setCancelled(false);

                Bukkit.getScheduler().runTask(plugin, () ->
                        restorePlaceholder(player, slot));
            }
            return;
        }

        if (!isPlacementAction(action)) return;
        if (cursor == null || cursor.getType() == Material.AIR) return;

        int placedAmount = (action == InventoryAction.PLACE_ONE) ? 1 : cursor.getAmount();
        ItemStack placedItem = cursor.clone();
        placedItem.setAmount(placedAmount);

        com.oreo.util.CooldownConfig cooldown = guiItem.getCooldown();
        if (cooldown != null && cooldown.isEnabled()
                && plugin.getCooldownManager() != null
                && !plugin.getCooldownManager().tryUse(player, cooldown)) {
            event.setCancelled(true);
            return;
        }

        if (guiItem.isRemoveOnPlace()) {

            event.setCancelled(true);
            consumeCursor(player, action, cursor.clone());
            fireOnPlace(player, guiItem, placedItem);
        } else {

            event.setCancelled(false);
            final ItemStack snapshot = placedItem;
            Bukkit.getScheduler().runTask(plugin, () -> {
                ItemStack inSlot = player.getOpenInventory().getTopInventory().getItem(slot);
                if (inSlot != null && inSlot.getType() != Material.AIR) {
                    updateContents(player, slot, ClickableItem.empty(inSlot));
                }
                fireOnPlace(player, guiItem, snapshot);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        GuiDefinition def = GuiInventoryProvider.getActiveGui(player.getUniqueId());
        if (def == null) return;

        int topSize = event.getView().getTopInventory().getSize();

        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            int rawSlot = entry.getKey();
            if (rawSlot >= topSize) continue;

            GuiItem guiItem = def.getItems().get(rawSlot);
            if (guiItem == null || guiItem.getButtonType() != ButtonType.INPUT) continue;

            ItemStack placed = entry.getValue().clone();

            com.oreo.util.CooldownConfig cooldown = guiItem.getCooldown();
            if (cooldown != null && cooldown.isEnabled()
                    && plugin.getCooldownManager() != null
                    && !plugin.getCooldownManager().tryUse(player, cooldown)) {
                event.setCancelled(true);
                return;
            }

            if (guiItem.isRemoveOnPlace()) {
                event.setCancelled(true);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack cursor = player.getItemOnCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        int remaining = cursor.getAmount() - placed.getAmount();
                        if (remaining <= 0) {
                            player.setItemOnCursor(new ItemStack(Material.AIR));
                        } else {
                            cursor.setAmount(remaining);
                            player.setItemOnCursor(cursor);
                        }
                    }
                    fireOnPlace(player, guiItem, placed);
                });
            } else {

                final int finalSlot = rawSlot;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack inSlot = player.getOpenInventory().getTopInventory().getItem(finalSlot);
                    if (inSlot != null && inSlot.getType() != Material.AIR) {
                        updateContents(player, finalSlot, ClickableItem.empty(inSlot));
                    }
                    fireOnPlace(player, guiItem, placed);
                });
            }
            return;
        }
    }

    private boolean isPickupAction(InventoryAction action) {
        return action == InventoryAction.PICKUP_ALL
                || action == InventoryAction.PICKUP_ONE
                || action == InventoryAction.PICKUP_SOME
                || action == InventoryAction.PICKUP_HALF;
    }

    private boolean isPlacementAction(InventoryAction action) {
        return action == InventoryAction.PLACE_ALL
                || action == InventoryAction.PLACE_ONE
                || action == InventoryAction.PLACE_SOME
                || action == InventoryAction.SWAP_WITH_CURSOR;
    }

    private void consumeCursor(Player player, InventoryAction action, ItemStack originalCursor) {
        if (action == InventoryAction.PLACE_ONE) {
            int remaining = originalCursor.getAmount() - 1;
            if (remaining <= 0) {
                player.setItemOnCursor(new ItemStack(Material.AIR));
            } else {
                originalCursor.setAmount(remaining);
                player.setItemOnCursor(originalCursor);
            }
        } else {
            player.setItemOnCursor(new ItemStack(Material.AIR));
        }
    }

    private void updateContents(Player player, int slot, ClickableItem item) {
        plugin.getInventoryManager().getContents(player).ifPresent(contents ->
                contents.set(slot / 9, slot % 9, item));
    }

    private void restorePlaceholder(Player player, int slot) {
        ItemStack placeholder = GuiInventoryProvider.getInputSlotPlaceholder(player.getUniqueId(), slot);
        if (placeholder != null) {
            updateContents(player, slot, ClickableItem.empty(placeholder));
        }
    }

    private void fireOnPlace(Player player, GuiItem guiItem, ItemStack item) {
        List<Action> actions = guiItem.getOnPlaceActions();
        if (actions.isEmpty()) return;

        String itemName = item.getType().name();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                itemName = meta.getDisplayName();
            }
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("input_item_material", item.getType().name());
        vars.put("input_item_name", itemName);
        vars.put("input_item_amount", String.valueOf(item.getAmount()));

        for (Action action : actions) {
            action.execute(plugin, player, vars);
        }
    }
}
