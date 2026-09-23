package com.htss.hookshot.map;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

/**
 * Water or lava lying still in a hollow of a cave: every open tile joined to the hollow's bottom that's no higher than
 * its surface, so nothing in it could run out. Pools are found when a cave is made, from its own seed, and they stay as
 * they are: nothing flows yet, and digging next to one doesn't drain it.
 */
public class FluidPool {

    public static final int NONE = 0, WATER = 1, LAVA = 2;
    // The most tiles a pool can have, and how deep it can be, in tiles
    private static final int MOST_TILES = 300, MOST_DEPTH = 7, LEAST_DEPTH = 2, LEAST_TILES = 5;

    public final int type;
    // The highest row of tiles that's filled. The surface is half a tile above that row's middles
    public final int surfaceRow;
    public final Vector<Coord> tiles;
    // The tiles it covers, from its surface down, inclusive
    public final int left, top, right, bottom;

    private FluidPool(int type, int surfaceRow, Vector<Coord> tiles) {
        this.type = type;
        this.surfaceRow = surfaceRow;
        this.tiles = tiles;
        int l = Integer.MAX_VALUE, r = Integer.MIN_VALUE, b = Integer.MIN_VALUE;
        for (Coord tile : tiles) {
            l = Math.min(l, tile.tileX);
            r = Math.max(r, tile.tileX);
            b = Math.max(b, tile.tileY);
        }
        left = l;
        top = surfaceRow;
        right = r;
        bottom = b;
    }

    // Where a pool may reach: its tiles and the rock around them have to be allowed
    public interface Allowed {
        boolean allows(int tileX, int tileY);
    }

    /**
     * Up to the wanted number of pools in the given tiles, 1 for rock and 0 for open, by column, marked in the fluid grid
     * with their type. The floors are tried in a shuffled order, and each fills the hollow it's in, up to a depth picked
     * for it, as long as the water stays where it's allowed and doesn't spill into a bigger cave. Most floors aren't in
     * a hollow, so most tries come to nothing. Pools from the lava row down are lava as often as the lava share says.
     */
    public static Vector<FluidPool> find(int[][] map, byte[][] fluid, Random random, int wanted, Allowed allowed, int lavaRow, double lavaShare) {
        int xTiles = map.length, yTiles = map[0].length;
        Vector<FluidPool> pools = new Vector<FluidPool>();
        Vector<Coord> floors = new Vector<Coord>();
        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles - 1; y++) {
                if (map[x][y] == 0 && map[x][y + 1] == 1 && allowed.allows(x, y)) {
                    floors.add(new Coord(x, y));
                }
            }
        }
        Collections.shuffle(floors, random);
        for (int attempt = 0; attempt < floors.size() && pools.size() < wanted; attempt++) {
            int x = floors.get(attempt).tileX, y = floors.get(attempt).tileY;
            if (fluid[x][y] != NONE) {
                continue;
            }
            int depth = LEAST_DEPTH + random.nextInt(MOST_DEPTH - LEAST_DEPTH + 1);
            boolean lava = random.nextDouble() < lavaShare;
            // Raised a row at a time, until it's as deep as it's meant to be, or it would spill or reach too far
            Vector<Coord> filled = null;
            int surface = y;
            for (int level = y; level > 0 && y - level < depth; level--) {
                Vector<Coord> region = fillFrom(map, fluid, x, y, level, allowed);
                if (region == null) {
                    break;
                }
                filled = region;
                surface = level;
            }
            if (filled == null || filled.size() < LEAST_TILES || y - surface + 1 < LEAST_DEPTH) {
                continue;
            }
            int type = (lava && surface >= lavaRow) ? LAVA : WATER;
            for (Coord tile : filled) {
                fluid[tile.tileX][tile.tileY] = (byte) type;
            }
            pools.add(new FluidPool(type, surface, filled));
        }
        return pools;
    }

    // Every open tile joined to the given one without going above the level, or none if that's too many, or reaches
    // somewhere it can't, or into another pool
    private static Vector<Coord> fillFrom(int[][] map, byte[][] fluid, int x, int y, int level, Allowed allowed) {
        int xTiles = map.length, yTiles = map[0].length;
        boolean[][] seen = new boolean[xTiles][yTiles];
        Vector<Coord> region = new Vector<Coord>();
        LinkedList<Coord> queue = new LinkedList<Coord>();
        queue.add(new Coord(x, y));
        seen[x][y] = true;
        int[][] neighbours = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            Coord tile = queue.poll();
            if (!allowed.allows(tile.tileX, tile.tileY) || fluid[tile.tileX][tile.tileY] != NONE) {
                return null;
            }
            region.add(tile);
            if (region.size() > MOST_TILES) {
                return null;
            }
            for (int[] neighbour : neighbours) {
                int nx = tile.tileX + neighbour[0], ny = tile.tileY + neighbour[1];
                if (nx < 0 || ny < level || nx >= xTiles || ny >= yTiles) {
                    continue;
                }
                if (map[nx][ny] == 1) {
                    // The rock holding it in has to be allowed too, or the pool would lie against a border
                    if (!allowed.allows(nx, ny)) {
                        return null;
                    }
                } else if (!seen[nx][ny]) {
                    seen[nx][ny] = true;
                    queue.add(new Coord(nx, ny));
                }
            }
        }
        return region;
    }
}
