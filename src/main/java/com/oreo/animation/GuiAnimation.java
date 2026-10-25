package com.oreo.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An immutable, reusable animation definition: an ordered list of {@link AnimationFrame frames}
 * plus playback settings (interval, start delay, {@link AnimationPlaybackMode mode}, loop count
 * and autoplay).
 *
 * <p>A {@code GuiAnimation} is stateless and can be shared across players — the mutable playback
 * position lives in a per-player {@link AnimationInstance}, which is created and driven by the
 * {@link AnimationController}. This mirrors how a {@code SmartInventory} definition is shared while
 * each player gets their own {@code InventoryContents}.</p>
 *
 * <p>Build one with {@link #builder()}:</p>
 * <pre>{@code
 * GuiAnimation border = GuiAnimation.builder()
 *         .interval(2)
 *         .playback(AnimationPlaybackMode.LOOP)
 *         .frame(frameOne)
 *         .frame(frameTwo)
 *         .build();
 * }</pre>
 */
public final class GuiAnimation {

    /** Loop count meaning "run forever". */
    public static final int INFINITE_LOOPS = 0;

    private final List<AnimationFrame> frames;
    private final int interval;
    private final int startDelay;
    private final AnimationPlaybackMode playback;
    private final int loops;
    private final boolean autoplay;

    private final Set<Integer> ownedSlots;
    private final int[] frameDurations;

    private GuiAnimation(List<AnimationFrame> frames, int interval, int startDelay,
                         AnimationPlaybackMode playback, int loops, boolean autoplay) {
        this.frames = frames;
        this.interval = interval;
        this.startDelay = startDelay;
        this.playback = playback;
        this.loops = loops;
        this.autoplay = autoplay;

        Set<Integer> slots = new LinkedHashSet<>();
        this.frameDurations = new int[frames.size()];
        for (int i = 0; i < frames.size(); i++) {
            AnimationFrame frame = frames.get(i);
            slots.addAll(frame.getSlotItems().keySet());
            frameDurations[i] = frame.getDurationTicks();
        }
        this.ownedSlots = Collections.unmodifiableSet(slots);
    }

    public List<AnimationFrame> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    public int getFrameCount() {
        return frames.size();
    }

    public AnimationFrame getFrame(int index) {
        return frames.get(index);
    }

    /** @return the base number of ticks each frame is shown (may be overridden per-frame) */
    public int getInterval() {
        return interval;
    }

    /** @return ticks to wait after the animation starts before the first frame advance */
    public int getStartDelay() {
        return startDelay;
    }

    public AnimationPlaybackMode getPlayback() {
        return playback;
    }

    /** @return number of loops before finishing, or {@link #INFINITE_LOOPS} for endless playback */
    public int getLoops() {
        return loops;
    }

    /** @return whether this animation should start automatically when the menu opens */
    public boolean isAutoplay() {
        return autoplay;
    }

    /**
     * @return the union of every slot referenced by any frame. While the animation runs it owns
     *         these slots exclusively; slots not present in the active frame are cleared.
     */
    public Set<Integer> getOwnedSlots() {
        return ownedSlots;
    }

    /** @return a per-frame duration array; entries {@code <= 0} mean "use {@link #getInterval()}" */
    int[] frameDurations() {
        return frameDurations;
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    /** Creates a fresh, independent runtime instance of this animation for one player. */
    public AnimationInstance newInstance() {
        return new AnimationInstance(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<AnimationFrame> frames = new ArrayList<>();
        private int interval = 2;
        private int startDelay = 0;
        private AnimationPlaybackMode playback = AnimationPlaybackMode.LOOP;
        private int loops = INFINITE_LOOPS;
        private boolean autoplay = true;

        private Builder() {
        }

        public Builder frame(AnimationFrame frame) {
            if (frame != null) frames.add(frame);
            return this;
        }

        public Builder frames(List<AnimationFrame> frames) {
            for (AnimationFrame frame : frames) frame(frame);
            return this;
        }

        /** Base ticks per frame. Values below 1 are clamped to 1. */
        public Builder interval(int ticks) {
            this.interval = Math.max(1, ticks);
            return this;
        }

        /** Ticks to wait before the first advance. Negative values are clamped to 0. */
        public Builder startDelay(int ticks) {
            this.startDelay = Math.max(0, ticks);
            return this;
        }

        public Builder playback(AnimationPlaybackMode playback) {
            if (playback != null) this.playback = playback;
            return this;
        }

        /** Number of loops before finishing; {@code <= 0} means infinite. */
        public Builder loops(int loops) {
            this.loops = Math.max(INFINITE_LOOPS, loops);
            return this;
        }

        public Builder autoplay(boolean autoplay) {
            this.autoplay = autoplay;
            return this;
        }

        public GuiAnimation build() {
            return new GuiAnimation(new ArrayList<>(frames), interval, startDelay, playback, loops, autoplay);
        }
    }
}
