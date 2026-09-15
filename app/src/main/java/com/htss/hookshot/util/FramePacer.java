package com.htss.hookshot.util;

/**
 * Picks which display frames advance the game, so it runs at a fixed number of updates per second
 * whatever the screen refresh rate is (60, 90, 120 Hz...). Feed it the vsync time of every frame.
 */
public class FramePacer {

    private final long updateIntervalNanos, toleranceNanos;
    private long lastFrameTimeNanos, accumulatedNanos;
    private boolean started = false;

    public FramePacer(int updatesPerSecond) {
        this.updateIntervalNanos = 1000000000L / updatesPerSecond;
        // Absorbs vsync jitter, so a screen running at the update rate never skips a frame
        this.toleranceNanos = updateIntervalNanos / 8;
    }

    public boolean shouldUpdate(long frameTimeNanos) {
        if (!started) {
            started = true;
            lastFrameTimeNanos = frameTimeNanos;
            accumulatedNanos = 0;
            return true;
        }
        accumulatedNanos += frameTimeNanos - lastFrameTimeNanos;
        lastFrameTimeNanos = frameTimeNanos;
        if (accumulatedNanos < updateIntervalNanos - toleranceNanos) {
            return false;
        }
        // Carry the leftover time (a 90 Hz screen updates on 2 of every 3 frames), but don't carry a
        // deficit, which would skip frames on screens slightly faster than the update rate, and don't
        // try to catch up after a stall
        accumulatedNanos = Math.max(0, Math.min(accumulatedNanos - updateIntervalNanos, updateIntervalNanos));
        return true;
    }

    public void reset() {
        started = false;
    }
}
