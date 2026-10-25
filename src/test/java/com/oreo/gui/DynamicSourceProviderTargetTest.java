package com.oreo.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link DynamicSourceProvider} reads a player-scoped source from the correct player:
 * the viewer by default, or the explicit data owner (target) when supplied.
 *
 * <p>ItemStacks are mocked rather than constructed so the test needs no live Bukkit server.</p>
 */
class DynamicSourceProviderTargetTest {

    /** A mocked ItemStack with no meta, so its display name falls back to the material name. */
    private static ItemStack mockItem(Material material, int amount) {
        ItemStack stack = Mockito.mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.getItemMeta()).thenReturn(null);
        when(stack.clone()).thenReturn(stack);
        return stack;
    }

    private static Player playerWithEnderChest(ItemStack[] contents) {
        Player p = Mockito.mock(Player.class);
        Inventory ec = Mockito.mock(Inventory.class);
        when(p.getEnderChest()).thenReturn(ec);
        when(ec.getContents()).thenReturn(contents);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        return p;
    }

    @Test
    void enderChestDefaultsToViewerWhenNoTarget() {
        ItemStack diamond = mockItem(Material.DIAMOND, 3);
        Player viewer = playerWithEnderChest(new ItemStack[]{diamond});

        List<Map<String, Object>> elements =
                DynamicSourceProvider.generate(DynamicSource.PLAYER_ENDERCHEST, viewer);

        assertEquals(1, elements.size());
        assertEquals("DIAMOND", elements.get(0).get("item_material"));
        verify(viewer).getEnderChest();
    }

    @Test
    void enderChestUsesTargetWhenSupplied() {
        Player viewer = playerWithEnderChest(new ItemStack[]{});
        ItemStack gold = mockItem(Material.GOLD_INGOT, 5);
        Player target = playerWithEnderChest(new ItemStack[]{gold});

        List<Map<String, Object>> elements =
                DynamicSourceProvider.generate(DynamicSource.PLAYER_ENDERCHEST, viewer, target);

        assertEquals(1, elements.size());
        assertEquals("GOLD_INGOT", elements.get(0).get("item_material"));
        // the viewer's ender chest must NOT be read when a target is supplied
        verify(viewer, never()).getEnderChest();
        verify(target).getEnderChest();
    }
}
