package com.oreo.animation;

import com.oreo.gui.GuiItem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single frame of a {@link GuiAnimation}. A frame maps one or more inventory slots to the
 * {@link GuiItem} that should be shown there while the frame is active.
 *
 * <p>Frames reuse the regular {@link GuiItem} model, so animated slots support the exact same
 * item definitions (materials, names, lore, placeholders, custom items, glow, …) as static
 * menu items — there is no separate item format.</p>
 *
 * <p>A frame may optionally override the animation's global interval with its own
 * {@link #getDurationTicks() duration}, allowing individual frames to linger longer or pass
 * quicker than the rest.</p>
 */
public final class AnimationFrame {

    /** Sentinel meaning "use the animation's global interval for this frame". */
    public static final int USE_ANIMATION_INTERVAL = -1;

    private final Map<Integer, GuiItem> slotItems;
    private final int durationTicks;

    private AnimationFrame(Map<Integer, GuiItem> slotItems, int durationTicks) {
        this.slotItems = slotItems;
        this.durationTicks = durationTicks;
    }

    /** @return an immutable view of the slot index → item mapping for this frame */
    public Map<Integer, GuiItem> getSlotItems() {
        return Collections.unmodifiableMap(slotItems);
    }

    /**
     * @return this frame's own display duration in ticks, or {@link #USE_ANIMATION_INTERVAL}
     *         to inherit the animation's global interval
     */
    public int getDurationTicks() {
        return durationTicks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Integer, GuiItem> slotItems = new LinkedHashMap<>();
        private int durationTicks = USE_ANIMATION_INTERVAL;

        private Builder() {
        }

        /**
         * Assigns an item to a slot for this frame. A {@code null} item clears the slot while
         * the frame is active.
         *
         * @param slot the raw slot index
         * @param item the item to display, or {@code null} to leave the slot empty
         */
        public Builder set(int slot, GuiItem item) {
            slotItems.put(slot, item);
            return this;
        }

        /** Overrides the animation interval for this specific frame. */
        public Builder duration(int ticks) {
            this.durationTicks = ticks > 0 ? ticks : USE_ANIMATION_INTERVAL;
            return this;
        }

        public AnimationFrame build() {
            return new AnimationFrame(new LinkedHashMap<>(slotItems), durationTicks);
        }
    }
}
