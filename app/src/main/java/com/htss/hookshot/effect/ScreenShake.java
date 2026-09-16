package com.htss.hookshot.effect;

import com.htss.hookshot.game.MyActivity;

import java.util.Random;

/**
 * Shakes the cave and everything in it for a moment, but not the HUD.
 */
public class ScreenShake {

    // How much of the shake is left after each frame
    private static final float DECAY = 0.85f;

    private static float intensity = 0, offsetX = 0, offsetY = 0;
    private static Random random = new Random();

    // In tiles. A weaker shake doesn't cut a stronger one short
    public static void shake(float tiles) {
        intensity = Math.max(intensity, tiles * MyActivity.TILE_WIDTH);
    }

    // Once per frame
    public static void update() {
        if (intensity < 0.5f) {
            clear();
            return;
        }
        offsetX = (random.nextFloat() * 2 - 1) * intensity;
        offsetY = (random.nextFloat() * 2 - 1) * intensity;
        intensity *= DECAY;
    }

    public static float getOffsetX() {
        return offsetX;
    }

    public static float getOffsetY() {
        return offsetY;
    }

    public static void clear() {
        intensity = 0;
        offsetX = 0;
        offsetY = 0;
    }
}
