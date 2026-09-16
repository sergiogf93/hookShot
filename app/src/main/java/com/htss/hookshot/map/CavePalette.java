package com.htss.hookshot.map;

import android.graphics.Color;

/**
 * The cave's colours. They change every few levels, so going deeper shows.
 */
public class CavePalette {

    private static final int LEVELS_PER_PALETTE = 10;
    private static final CavePalette[] PALETTES = {
            // Earth
            new CavePalette(Color.rgb(96, 64, 40), Color.rgb(170, 128, 86), Color.rgb(32, 20, 12),
                    Color.rgb(24, 19, 16), Color.rgb(56, 45, 37), Color.rgb(210, 180, 140)),
            // Crimson, close to the original look
            new CavePalette(Color.rgb(118, 10, 10), Color.rgb(205, 72, 56), Color.rgb(42, 2, 4),
                    Color.rgb(24, 12, 22), Color.rgb(54, 24, 40), Color.rgb(235, 150, 130)),
            // Ice
            new CavePalette(Color.rgb(54, 86, 126), Color.rgb(170, 215, 245), Color.rgb(12, 24, 42),
                    Color.rgb(12, 18, 34), Color.rgb(28, 44, 72), Color.rgb(205, 232, 255)),
            // Amethyst
            new CavePalette(Color.rgb(82, 42, 110), Color.rgb(186, 132, 228), Color.rgb(26, 8, 40),
                    Color.rgb(18, 12, 32), Color.rgb(44, 28, 64), Color.rgb(222, 184, 255)),
            // Emerald
            new CavePalette(Color.rgb(22, 82, 60), Color.rgb(100, 208, 152), Color.rgb(4, 28, 20),
                    Color.rgb(8, 20, 20), Color.rgb(18, 48, 42), Color.rgb(172, 242, 204)),
    };

    // The rock, the light on rock next to the caves, the rock's edges, the caves' gradient and the floating dust
    public final int rock, rim, outline, backgroundTop, backgroundBottom, dust;
    // The distant rock behind the caves, and the darkness away from the character
    public final int backdrop, darkness;

    private CavePalette(int rock, int rim, int outline, int backgroundTop, int backgroundBottom, int dust) {
        this.rock = rock;
        this.rim = rim;
        this.outline = outline;
        this.backgroundTop = backgroundTop;
        this.backgroundBottom = backgroundBottom;
        this.dust = dust;
        this.backdrop = blend(backgroundBottom, rock, 0.5f);
        this.darkness = blend(Color.BLACK, backgroundTop, 0.5f);
    }

    public static CavePalette forLevel(int level) {
        return PALETTES[(Math.max(level, 0) / LEVELS_PER_PALETTE) % PALETTES.length];
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int blend(int from, int to, float amount) {
        return Color.rgb((int) (Color.red(from) + (Color.red(to) - Color.red(from)) * amount),
                (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * amount),
                (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount));
    }
}
