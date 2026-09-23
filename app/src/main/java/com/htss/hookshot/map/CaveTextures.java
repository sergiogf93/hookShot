package com.htss.hookshot.map;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.NoiseUtil;

import java.util.HashMap;

/**
 * The cave's textures, made in code once per palette. They tile seamlessly.
 */
public class CaveTextures {

    public static final int SIZE = 256;

    private static final HashMap<CavePalette, Bitmap> rocks = new HashMap<CavePalette, Bitmap>();
    private static final HashMap<CavePalette, Bitmap> backdrops = new HashMap<CavePalette, Bitmap>();

    // The rock's colour with darker and lighter patches, faint wavy layers, and fine grain
    public static synchronized Bitmap getRock(CavePalette palette) {
        Bitmap rock = rocks.get(palette);
        if (rock == null) {
            float[] patches = NoiseUtil.tileableNoise(SIZE, new int[]{4, 8}, 1);
            float[] grain = NoiseUtil.tileableNoise(SIZE, new int[]{32, 64}, 2);
            int[] pixels = new int[SIZE * SIZE];
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int i = y * SIZE + x;
                    // A whole number of layers per tile, so it still tiles
                    float layers = (float) Math.sin(2 * Math.PI * (5f * y / SIZE + 0.8f * patches[i]));
                    pixels[i] = scale(palette.rock, 0.65f + 0.35f * patches[i] + 0.35f * grain[i] + 0.07f * layers);
                }
            }
            rock = Bitmap.createBitmap(pixels, SIZE, SIZE, Bitmap.Config.ARGB_8888);
            rocks.put(palette, rock);
        }
        return rock;
    }

    // Soft blobs of distant rock, and nothing around them
    public static synchronized Bitmap getBackdrop(CavePalette palette) {
        Bitmap backdrop = backdrops.get(palette);
        if (backdrop == null) {
            float[] noise = NoiseUtil.tileableNoise(SIZE, new int[]{2, 4, 8}, 3);
            int[] pixels = new int[SIZE * SIZE];
            for (int i = 0; i < pixels.length; i++) {
                int alpha = (int) (190 * NoiseUtil.smoothstep(0.45f, 0.65f, noise[i]));
                pixels[i] = DrawUtil.withAlpha(palette.backdrop, alpha);
            }
            backdrop = Bitmap.createBitmap(pixels, SIZE, SIZE, Bitmap.Config.ARGB_8888);
            backdrops.put(palette, backdrop);
        }
        return backdrop;
    }

    private static int scale(int color, float factor) {
        return Color.rgb(Math.min(255, (int) (Color.red(color) * factor)),
                Math.min(255, (int) (Color.green(color) * factor)),
                Math.min(255, (int) (Color.blue(color) * factor)));
    }
}
