package com.oreo.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pure playback state machine ({@link AnimationInstance}) with no Bukkit involved.
 * Frames carry no items here — only the index progression matters for these assertions.
 */
class AnimationInstanceTest {

    private static AnimationFrame emptyFrame() {
        return AnimationFrame.builder().set(0, null).build();
    }

    private static GuiAnimation animation(int frames, int interval, AnimationPlaybackMode mode) {
        GuiAnimation.Builder b = GuiAnimation.builder().interval(interval).playback(mode);
        for (int i = 0; i < frames; i++) b.frame(emptyFrame());
        return b.build();
    }

    /** Advances {@code ticks} times and returns how many of those ticks reported a frame change. */
    private static int tickTimes(AnimationInstance instance, int ticks) {
        int changes = 0;
        for (int i = 0; i < ticks; i++) {
            if (instance.tick()) changes++;
        }
        return changes;
    }

    @Test
    void intervalControlsFrameAdvance() {
        AnimationInstance inst = animation(3, 3, AnimationPlaybackMode.LOOP).newInstance();
        assertEquals(0, inst.getCurrentFrameIndex());
        assertFalse(inst.tick()); // tick 1
        assertFalse(inst.tick()); // tick 2
        assertTrue(inst.tick());  // tick 3 -> advance
        assertEquals(1, inst.getCurrentFrameIndex());
    }

    @Test
    void loopWrapsAround() {
        AnimationInstance inst = animation(3, 1, AnimationPlaybackMode.LOOP).newInstance();
        assertTrue(inst.tick()); assertEquals(1, inst.getCurrentFrameIndex());
        assertTrue(inst.tick()); assertEquals(2, inst.getCurrentFrameIndex());
        assertTrue(inst.tick()); assertEquals(0, inst.getCurrentFrameIndex()); // wrapped
    }

    @Test
    void onceStopsOnLastFrame() {
        AnimationInstance inst = animation(3, 1, AnimationPlaybackMode.ONCE).newInstance();
        assertTrue(inst.tick());  assertEquals(1, inst.getCurrentFrameIndex());
        assertTrue(inst.tick());  assertEquals(2, inst.getCurrentFrameIndex());
        assertFalse(inst.tick()); // no more frames
        assertEquals(2, inst.getCurrentFrameIndex());
        assertTrue(inst.isFinished());
        assertFalse(inst.isRunning());
    }

    @Test
    void reverseStartsAtLastAndCountsDown() {
        AnimationInstance inst = animation(3, 1, AnimationPlaybackMode.REVERSE).newInstance();
        assertEquals(2, inst.getCurrentFrameIndex());
        assertTrue(inst.tick()); assertEquals(1, inst.getCurrentFrameIndex());
        assertTrue(inst.tick()); assertEquals(0, inst.getCurrentFrameIndex());
        assertTrue(inst.tick()); assertEquals(2, inst.getCurrentFrameIndex()); // wrapped back to top
    }

    @Test
    void pingPongBouncesBetweenEnds() {
        AnimationInstance inst = animation(3, 1, AnimationPlaybackMode.PING_PONG).newInstance();
        assertEquals(0, inst.getCurrentFrameIndex());
        assertTrue(inst.tick()); assertEquals(1, inst.getCurrentFrameIndex());
        assertTrue(inst.tick()); assertEquals(2, inst.getCurrentFrameIndex());
        assertTrue(inst.tick()); assertEquals(1, inst.getCurrentFrameIndex()); // bounce down
        assertTrue(inst.tick()); assertEquals(0, inst.getCurrentFrameIndex()); // bottom
        assertTrue(inst.tick()); assertEquals(1, inst.getCurrentFrameIndex()); // bounce up again
    }

    @Test
    void startDelayPostponesFirstAdvance() {
        GuiAnimation anim = GuiAnimation.builder()
                .interval(1).startDelay(3).playback(AnimationPlaybackMode.LOOP)
                .frame(emptyFrame()).frame(emptyFrame()).build();
        AnimationInstance inst = anim.newInstance();
        assertFalse(inst.tick()); // delay 1
        assertFalse(inst.tick()); // delay 2
        assertFalse(inst.tick()); // delay 3
        assertTrue(inst.tick());  // now the interval elapses
        assertEquals(1, inst.getCurrentFrameIndex());
    }

    @Test
    void pauseFreezesAndResumeContinues() {
        AnimationInstance inst = animation(3, 1, AnimationPlaybackMode.LOOP).newInstance();
        assertTrue(inst.tick()); assertEquals(1, inst.getCurrentFrameIndex());
        inst.pause();
        assertFalse(inst.isRunning());
        assertFalse(inst.tick());
        assertEquals(1, inst.getCurrentFrameIndex()); // frozen
        inst.resume();
        assertTrue(inst.tick());
        assertEquals(2, inst.getCurrentFrameIndex());
    }

    @Test
    void stopHaltsPlaybackPermanently() {
        AnimationInstance inst = animation(3, 1, AnimationPlaybackMode.LOOP).newInstance();
        inst.tick();
        inst.stop();
        assertTrue(inst.isFinished());
        assertFalse(inst.tick());
        assertFalse(inst.isRunning());
    }

    @Test
    void restartReturnsToBeginning() {
        AnimationInstance inst = animation(3, 1, AnimationPlaybackMode.LOOP).newInstance();
        inst.tick(); inst.tick();
        assertEquals(2, inst.getCurrentFrameIndex());
        inst.restart();
        assertEquals(0, inst.getCurrentFrameIndex());
        assertFalse(inst.isFinished());
        assertTrue(inst.isRunning());
    }

    @Test
    void finiteLoopCountFinishes() {
        GuiAnimation anim = GuiAnimation.builder()
                .interval(1).loops(2).playback(AnimationPlaybackMode.LOOP)
                .frame(emptyFrame()).frame(emptyFrame()).build();
        AnimationInstance inst = anim.newInstance();
        // loop 1: 0->1->0
        assertTrue(inst.tick());  assertEquals(1, inst.getCurrentFrameIndex());
        assertTrue(inst.tick());  assertEquals(0, inst.getCurrentFrameIndex());
        // loop 2: 0->1->(finish)
        assertTrue(inst.tick());  assertEquals(1, inst.getCurrentFrameIndex());
        assertFalse(inst.tick()); // second wrap hits loop limit
        assertTrue(inst.isFinished());
    }

    @Test
    void singleFrameLoopNeverChanges() {
        AnimationInstance inst = animation(1, 1, AnimationPlaybackMode.LOOP).newInstance();
        assertEquals(0, tickTimes(inst, 10));
        assertEquals(0, inst.getCurrentFrameIndex());
    }

    @Test
    void emptyAnimationIsInert() {
        GuiAnimation anim = GuiAnimation.builder().build();
        assertTrue(anim.isEmpty());
        AnimationInstance inst = anim.newInstance();
        assertFalse(inst.tick());
        assertFalse(inst.isRunning());
    }
}
