package com.htss.hookshot.map;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.htss.hookshot.util.NoiseUtil;

import java.util.LinkedList;

/**
 * How a cave's rock is shaded: lit next to the caves and darker deeper in. Worked out per tile and blurred, a pixel for
 * every tile, to be drawn over the rock scaled up smoothly, with its first pixel's middle on the cave's corner.
 *
 * It touches nothing but the tiles it's given, so it can be made away from the main thread, with its cave.
 */
public class CaveShading {

    // How strong the lit edges and the deep shadows are
    private static final int RIM_ALPHA = 80, SHADOW_ALPHA = 170;
    // How many tiles from its borders a cave's shading fades into the shading the border alone gives
    private static final float BORDER_FADE = 5;

    private final int[][] map;
    private final int xTiles, yTiles;

    private CaveShading(int[][] map) {
        this.map = map;
        this.xTiles = map.length;
        this.yTiles = map[0].length;
    }

    // From the tiles, 1 for rock and 0 for open, by column, lit in the palette's colour
    public static Bitmap create(int[][] map, CavePalette palette) {
        return new CaveShading(map).create(palette);
    }

    private Bitmap create(CavePalette palette) {
        int[][] depth = getDepthInRock();
        float[] light = new float[xTiles * yTiles], shadow = new float[xTiles * yTiles];
        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles; y++) {
                light[y * xTiles + x] = (depth[x][y] == 1) ? 1 : 0;
                shadow[y * xTiles + x] = Math.min(1, Math.max(0, (depth[x][y] - 2) / 4f));
            }
        }
        light = blur(light);
        shadow = blur(shadow);
        blendIntoBorders(light, shadow);
        int[] pixels = new int[xTiles * yTiles];
        for (int i = 0; i < pixels.length; i++) {
            int lightAlpha = (int) (RIM_ALPHA * light[i]);
            int alpha = Math.min(255, lightAlpha + (int) (SHADOW_ALPHA * shadow[i]));
            float rimShare = (alpha == 0) ? 0 : (float) lightAlpha / alpha;
            pixels[i] = Color.argb(alpha, (int) (Color.red(palette.rim) * rimShare), (int) (Color.green(palette.rim) * rimShare), (int) (Color.blue(palette.rim) * rimShare));
        }
        return Bitmap.createBitmap(pixels, xTiles, yTiles, Bitmap.Config.ARGB_8888);
    }

    // The cave only knows its own rock, so on its own the shading would change sharply where it meets the next one,
    // which sees other caves close to the border. So towards each border it fades into shading made from nothing but
    // the border itself, how far along it the nearest cave is, which the cave on the other side makes the same way from
    // the same border. The rock beyond all caves is dark like the deepest rock, and so is a border with no cave near it
    private void blendIntoBorders(float[] light, float[] shadow) {
        // Top, bottom, left and right
        float[][][] borders = {getBorderShading(0, 0, 1, 0, xTiles), getBorderShading(0, yTiles - 1, 1, 0, xTiles),
                getBorderShading(0, 0, 0, 1, yTiles), getBorderShading(xTiles - 1, 0, 0, 1, yTiles)};
        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles; y++) {
                int i = y * xTiles + x;
                int[] along = {x, x, y, y}, distance = {y, yTiles - 1 - y, x, xTiles - 1 - x};
                // The furthest border first and the nearest last, so right on a border it's all there is
                boolean[] done = new boolean[4];
                for (int n = 0; n < 4; n++) {
                    int border = -1;
                    for (int b = 0; b < 4; b++) {
                        if (!done[b] && (border < 0 || distance[b] > distance[border])) {
                            border = b;
                        }
                    }
                    done[border] = true;
                    float keep = NoiseUtil.smoothstep(0, BORDER_FADE, distance[border]);
                    float borderLight = borders[border][0][along[border]], borderShadow = borders[border][1][along[border]];
                    light[i] = borderLight + (light[i] - borderLight) * keep;
                    shadow[i] = borderShadow + (shadow[i] - borderShadow) * keep;
                }
            }
        }
    }

    // The light and shadow along a border, from how far along it each of its tiles is from the nearest cave on it
    private float[][] getBorderShading(int startX, int startY, int stepX, int stepY, int length) {
        int[] along = new int[length];
        int last = -length;
        for (int i = 0; i < length; i++) {
            if (map[startX + i * stepX][startY + i * stepY] == 0) {
                last = i;
            }
            along[i] = i - last;
        }
        last = 2 * length;
        for (int i = length - 1; i >= 0; i--) {
            if (map[startX + i * stepX][startY + i * stepY] == 0) {
                last = i;
            }
            along[i] = Math.min(along[i], last - i);
        }
        float[] light = new float[length], shadow = new float[length];
        for (int i = 0; i < length; i++) {
            light[i] = (along[i] == 1) ? 1 : 0;
            shadow[i] = Math.min(1, Math.max(0, (along[i] - 2) / 4f));
        }
        return new float[][]{blurAlong(light), blurAlong(shadow)};
    }

    private static float[] blurAlong(float[] values) {
        float[] blurred = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            blurred[i] = (values[Math.max(0, i - 1)] + values[i] + values[Math.min(values.length - 1, i + 1)]) / 3;
        }
        return blurred;
    }

    // How many tiles each tile is from the nearest cave, 0 for the caves themselves
    private int[][] getDepthInRock() {
        int[][] depth = new int[xTiles][yTiles];
        LinkedList<Coord> queue = new LinkedList<Coord>();
        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles; y++) {
                if (map[x][y] == 0) {
                    queue.add(new Coord(x, y));
                } else {
                    depth[x][y] = Integer.MAX_VALUE;
                }
            }
        }
        int[][] neighbours = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            Coord tile = queue.poll();
            int next = depth[tile.tileX][tile.tileY] + 1;
            for (int[] neighbour : neighbours) {
                int x = tile.tileX + neighbour[0];
                int y = tile.tileY + neighbour[1];
                if (isInMapRange(x, y) && depth[x][y] > next) {
                    depth[x][y] = next;
                    queue.add(new Coord(x, y));
                }
            }
        }
        return depth;
    }

    private float[] blur(float[] values) {
        float[] blurred = new float[values.length];
        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles; y++) {
                float sum = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = Math.max(0, Math.min(xTiles - 1, x + dx));
                        int ny = Math.max(0, Math.min(yTiles - 1, y + dy));
                        sum += values[ny * xTiles + nx];
                    }
                }
                blurred[y * xTiles + x] = sum / 9;
            }
        }
        return blurred;
    }

    private boolean isInMapRange(int x, int y) {
        return x >= 0 && x < xTiles && y >= 0 && y < yTiles;
    }
}
