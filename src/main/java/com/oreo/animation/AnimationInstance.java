package com.oreo.animation;

/**
 * The per-player, mutable playback state of a {@link GuiAnimation}.
 *
 * <p>This class contains <b>no Bukkit dependencies</b> — it is a pure frame-index state machine so
 * that all playback behaviour (frame progression, interval timing, looping, reverse, ping-pong,
 * start delay, pause/resume/stop/restart) can be unit-tested in isolation. The
 * {@link AnimationController} owns one instance per animation per open menu and is responsible for
 * turning frame changes into actual inventory updates.</p>
 *
 * <p>Ticking model: {@link #tick()} is expected to be called once per inventory update cycle
 * (which SmartInvs runs once per player per {@code updateFrequency} ticks — 1 by default). Each
 * call represents one elapsed tick. The instance accumulates ticks and advances a frame when the
 * active frame's effective interval elapses.</p>
 */
public final class AnimationInstance {

    private final GuiAnimation animation;
    private final int frameCount;
    private final int[] frameDurations;

    private int frameIndex;
    private int tickCounter;
    private int delayCounter;
    private boolean forward;
    private boolean paused;
    private boolean finished;
    private int loopsCompleted;

    AnimationInstance(GuiAnimation animation) {
        this.animation = animation;
        this.frameCount = animation.getFrameCount();
        this.frameDurations = animation.frameDurations();
        reset();
    }

    public GuiAnimation getAnimation() {
        return animation;
    }

    /**
     * Resets playback to the beginning, honouring the animation's start delay. For
     * {@link AnimationPlaybackMode#REVERSE} the starting frame is the last one.
     */
    public void restart() {
        reset();
    }

    private void reset() {
        this.frameIndex = animation.getPlayback() == AnimationPlaybackMode.REVERSE
                ? Math.max(0, frameCount - 1)
                : 0;
        this.tickCounter = 0;
        this.delayCounter = animation.getStartDelay();
        this.forward = true;
        this.paused = false;
        this.finished = false;
        this.loopsCompleted = 0;
    }

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        if (!finished) this.paused = false;
    }

    /** Stops the animation and marks it finished; the current frame is left in place. */
    public void stop() {
        this.finished = true;
        this.paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isFinished() {
        return finished;
    }

    /** @return {@code true} if the animation is actively advancing (not paused, not finished, non-empty) */
    public boolean isRunning() {
        return !paused && !finished && frameCount > 0;
    }

    public int getCurrentFrameIndex() {
        return frameIndex;
    }

    public int getLoopsCompleted() {
        return loopsCompleted;
    }

    private int effectiveInterval() {
        int perFrame = (frameIndex >= 0 && frameIndex < frameDurations.length) ? frameDurations[frameIndex] : -1;
        return perFrame > 0 ? perFrame : animation.getInterval();
    }

    /**
     * Advances playback by one tick.
     *
     * @return {@code true} if the active frame changed as a result of this tick (meaning the
     *         controller should re-render the owned slots), {@code false} otherwise
     */
    public boolean tick() {
        if (paused || finished || frameCount == 0) {
            return false;
        }
        if (delayCounter > 0) {
            delayCounter--;
            return false;
        }
        tickCounter++;
        if (tickCounter < effectiveInterval()) {
            return false;
        }
        tickCounter = 0;
        return advance();
    }

    /** Computes the next frame index for the current mode. Returns whether the frame changed. */
    private boolean advance() {
        if (frameCount <= 1) {
            if (animation.getPlayback() == AnimationPlaybackMode.ONCE) {
                finished = true;
            }
            return false;
        }

        switch (animation.getPlayback()) {
            case ONCE:
                if (frameIndex + 1 >= frameCount) {
                    finished = true;
                    return false;
                }
                frameIndex++;
                return true;

            case REVERSE:
                if (frameIndex - 1 < 0) {
                    if (reachedLoopLimit()) {
                        finished = true;
                        return false;
                    }
                    frameIndex = frameCount - 1;
                    return true;
                }
                frameIndex--;
                return true;

            case PING_PONG:
                return advancePingPong();

            case LOOP:
            default:
                if (frameIndex + 1 >= frameCount) {
                    if (reachedLoopLimit()) {
                        finished = true;
                        return false;
                    }
                    frameIndex = 0;
                    return true;
                }
                frameIndex++;
                return true;
        }
    }

    private boolean advancePingPong() {
        int next;
        if (forward) {
            next = frameIndex + 1;
            if (next >= frameCount) {
                // overshot the top — reflect back down
                forward = false;
                next = frameIndex - 1;
            }
        } else {
            next = frameIndex - 1;
            if (next < 0) {
                // overshot the bottom — one full there-and-back loop completed
                forward = true;
                if (reachedLoopLimit()) {
                    finished = true;
                    return false; // stay on the first frame
                }
                next = frameIndex + 1;
            }
        }
        if (next == frameIndex) {
            return false;
        }
        frameIndex = next;
        return true;
    }

    /** Increments the completed-loop counter and reports whether the configured limit is hit. */
    private boolean reachedLoopLimit() {
        loopsCompleted++;
        int max = animation.getLoops();
        return max > GuiAnimation.INFINITE_LOOPS && loopsCompleted >= max;
    }
}
