package com.htss.hookshot.util;

import java.util.Random;

/**
 * Smooth random patterns for the textures drawn in code.
 */
public class NoiseUtil {

    // Values in [0, 1] that tile seamlessly, adding octaves of value noise from coarse to fine. The size has to be a
    // multiple of every cell count
    public static float[] tileableNoise(int size, int[] cellCounts, long seed) {
        Random random = new Random(seed);
        float[] noise = new float[size * size];
        float weight = 1, totalWeight = 0;
        for (int cells : cellCounts) {
            float[] grid = new float[cells * cells];
            for (int i = 0; i < grid.length; i++) {
                grid[i] = random.nextFloat();
            }
            for (int y = 0; y < size; y++) {
                float gridY = (float) y * cells / size;
                int y0 = (int) gridY, y1 = (y0 + 1) % cells;
                float ty = smooth(gridY - y0);
                for (int x = 0; x < size; x++) {
                    float gridX = (float) x * cells / size;
                    int x0 = (int) gridX, x1 = (x0 + 1) % cells;
                    float tx = smooth(gridX - x0);
                    float top = lerp(grid[y0 * cells + x0], grid[y0 * cells + x1], tx);
                    float bottom = lerp(grid[y1 * cells + x0], grid[y1 * cells + x1], tx);
                    noise[y * size + x] += lerp(top, bottom, ty) * weight;
                }
            }
            totalWeight += weight;
            weight /= 2;
        }
        for (int i = 0; i < noise.length; i++) {
            noise[i] /= totalWeight;
        }
        return noise;
    }

    public static float smoothstep(float edge0, float edge1, float x) {
        return smooth(Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0))));
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
