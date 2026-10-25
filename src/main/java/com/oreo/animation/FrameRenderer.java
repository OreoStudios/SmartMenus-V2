package com.oreo.animation;

import com.oreo.gui.GuiItem;
import fr.minuskube.inv.ClickableItem;

/**
 * Turns an animation frame's {@link GuiItem} into a live {@link ClickableItem} for a specific slot.
 *
 * <p>This is the single seam between the (Bukkit-agnostic) animation engine and SmartMenus' item
 * pipeline: the controller decides <i>which</i> item belongs in <i>which</i> slot, and the renderer
 * builds the actual {@link org.bukkit.inventory.ItemStack} — applying materials, placeholders,
 * custom items, glow, etc. — exactly like a normal menu item.</p>
 */
@FunctionalInterface
public interface FrameRenderer {

    /**
     * @param slot the slot the item is being placed in
     * @param item the frame's item definition, or {@code null} if the slot should be cleared
     * @return the clickable item to place, or {@code null} to leave the slot empty
     */
    ClickableItem render(int slot, GuiItem item);
}
