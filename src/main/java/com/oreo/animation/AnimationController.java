package com.oreo.animation;

import com.oreo.gui.GuiItem;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Manages every named animation of a single open menu (i.e. one player's {@link InventoryContents}).
 *
 * <p>The controller is deliberately lightweight: it creates <b>no</b> Bukkit scheduler tasks. All
 * playback is driven by {@link #tick(InventoryContents, FrameRenderer)}, which SmartMenus calls from
 * the existing per-player SmartInvs update lifecycle
 * ({@code InventoryProvider#update}). One update lifecycle per open inventory advances every
 * animation, so the "one task per animation / per slot / per player" trap is avoided entirely.</p>
 *
 * <h2>Slot ownership &amp; overlap</h2>
 * While running, an animation owns the union of all slots referenced across its frames. When a frame
 * becomes active, every owned slot that the active frame does not define is cleared. If two
 * animations own the same slot, whichever is applied last in a given tick wins (registration order),
 * so overlapping animated regions should be avoided.
 *
 * <p>The controller holds no player reference and no scheduler handle, so it leaks nothing: dropping
 * it (which SmartMenus does on inventory close/quit) is sufficient cleanup.</p>
 */
public final class AnimationController {

    /** Property key under which the controller is stored on a SmartInvs {@code InventoryContents}. */
    public static final String CONTENTS_PROPERTY = "smartmenus:animations";

    private final Map<String, AnimationInstance> instances = new LinkedHashMap<>();
    /** Names whose current frame must be (re)painted on the next tick even without an advance. */
    private final Set<String> dirty = new java.util.LinkedHashSet<>();

    /**
     * Registers an animation under a name without changing its autoplay-driven running state.
     * If {@link GuiAnimation#isAutoplay()} is {@code true} the animation starts running immediately
     * and its first frame is painted on the next tick.
     */
    public void register(String name, GuiAnimation animation) {
        if (name == null || animation == null || animation.isEmpty()) return;
        AnimationInstance instance = animation.newInstance();
        if (!animation.isAutoplay()) {
            instance.pause();
        }
        instances.put(name, instance);
        dirty.add(name);
    }

    /**
     * Registers (if needed) and (re)starts an animation from the beginning.
     * Equivalent to the illustrative {@code contents.animations().play(name, animation)} call.
     */
    public void play(String name, GuiAnimation animation) {
        register(name, animation);
        play(name);
    }

    /** Restarts the named animation from its first frame and resumes playback. */
    public void play(String name) {
        AnimationInstance instance = instances.get(name);
        if (instance == null) return;
        instance.restart();
        dirty.add(name);
    }

    public void pause(String name) {
        AnimationInstance instance = instances.get(name);
        if (instance != null) instance.pause();
    }

    public void resume(String name) {
        AnimationInstance instance = instances.get(name);
        if (instance != null) instance.resume();
    }

    public void restart(String name) {
        play(name);
    }

    public void stop(String name) {
        AnimationInstance instance = instances.get(name);
        if (instance != null) instance.stop();
    }

    public void pauseAll() {
        instances.values().forEach(AnimationInstance::pause);
    }

    public void resumeAll() {
        instances.values().forEach(AnimationInstance::resume);
    }

    public void stopAll() {
        instances.values().forEach(AnimationInstance::stop);
    }

    public boolean isRunning(String name) {
        AnimationInstance instance = instances.get(name);
        return instance != null && instance.isRunning();
    }

    public boolean hasAny() {
        return !instances.isEmpty();
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(instances.keySet());
    }

    /** @return the current frame index of the named animation, or empty if it is unknown */
    public Optional<Integer> currentFrame(String name) {
        AnimationInstance instance = instances.get(name);
        return instance == null ? Optional.empty() : Optional.of(instance.getCurrentFrameIndex());
    }

    /** @return the runtime instance for the named animation, primarily for tests/inspection */
    public AnimationInstance instance(String name) {
        return instances.get(name);
    }

    /**
     * Advances every animation by one tick and paints any frame that changed (or was just started
     * / restarted) into the given contents.
     *
     * @param contents the live inventory contents to mutate
     * @param renderer builds the {@link ClickableItem} for each animated slot
     */
    public void tick(InventoryContents contents, FrameRenderer renderer) {
        if (instances.isEmpty()) return;
        for (Map.Entry<String, AnimationInstance> entry : instances.entrySet()) {
            String name = entry.getKey();
            AnimationInstance instance = entry.getValue();

            boolean changed = instance.tick();
            boolean forceRender = dirty.remove(name);

            if (changed || forceRender) {
                applyFrame(instance, contents, renderer);
            }
        }
    }

    /**
     * Immediately paints the current frame of every registered animation without advancing playback.
     * Used once when the menu opens so animated slots are never briefly empty.
     */
    public void renderCurrent(InventoryContents contents, FrameRenderer renderer) {
        for (AnimationInstance instance : instances.values()) {
            applyFrame(instance, contents, renderer);
        }
        dirty.clear();
    }

    /** Paints the instance's active frame, clearing owned slots the active frame does not define. */
    private void applyFrame(AnimationInstance instance, InventoryContents contents, FrameRenderer renderer) {
        GuiAnimation animation = instance.getAnimation();
        AnimationFrame frame = animation.getFrame(instance.getCurrentFrameIndex());
        Map<Integer, GuiItem> frameItems = frame.getSlotItems();

        for (int slot : animation.getOwnedSlots()) {
            if (frameItems.containsKey(slot)) {
                GuiItem item = frameItems.get(slot);
                ClickableItem rendered = renderer.render(slot, item);
                contents.set(slot, rendered);
            } else {
                // owned by the animation but not part of this frame → clear it
                contents.set(slot, null);
            }
        }
    }
}
