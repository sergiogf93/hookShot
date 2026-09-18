package com.htss.hookshot.util;

/**
 * Tells how many updates each display frame advances the game, so it runs at a fixed number of updates per second
 * whatever the screen refresh rate is (60, 90, 120 Hz...), and keeps its speed when frames are dropped by running the
 * updates they missed. Feed it the vsync time of every frame.
 */
public class FramePacer {

    // The most updates one frame runs, so a slow device plays in slow motion below this many frames per second rather
    // than jumping ahead in big steps, and extra updates can't slow the frames down further and further
    private static final int MAX_UPDATES_PER_FRAME = 4;

    private final long updateIntervalNanos, toleranceNanos;
    private long lastFrameTimeNanos, accumulatedNanos;
    private boolean started = false;

    public FramePacer(int updatesPerSecond) {
        this.updateIntervalNanos = 1000000000L / updatesPerSecond;
        // Absorbs vsync jitter, so a screen running at the update rate never skips a frame
        this.toleranceNanos = updateIntervalNanos / 8;
    }

    public int updatesDue(long frameTimeNanos) {
        if (!started) {
            started = true;
            lastFrameTimeNanos = frameTimeNanos;
            accumulatedNanos = 0;
            return 1;
        }
        accumulatedNanos += frameTimeNanos - lastFrameTimeNanos;
        lastFrameTimeNanos = frameTimeNanos;
        int updates = 0;
        while (accumulatedNanos >= updateIntervalNanos - toleranceNanos && updates < MAX_UPDATES_PER_FRAME) {
            accumulatedNanos -= updateIntervalNanos;
            updates++;
        }
        // Carry the leftover time (a 90 Hz screen updates on 2 of every 3 frames), but don't carry a deficit, which
        // would skip frames on screens slightly faster than the update rate, nor more than one update's worth, so a
        // stall like loading a level isn't caught up with a burst of updates
        accumulatedNanos = Math.max(0, Math.min(accumulatedNanos, updateIntervalNanos));
        return updates;
    }

    public void reset() {
        started = false;
    }
}
