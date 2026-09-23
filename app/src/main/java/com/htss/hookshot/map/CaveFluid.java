package com.htss.hookshot.map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.htss.hookshot.util.DrawUtil;

import java.util.Arrays;

/**
 * The water and lava in a cave, and how they flow. The cave is split into cells half a tile wide, each holding some
 * of one fluid, a whole cell's worth being 1. Every update fluid falls into the cell below, then evens out with the
 * cells beside it, and what's squeezed in below pushes up into the cell above, a little more of it fitting in the
 * lower cells, which is what lets it rise in both arms of a bend. Cells with a bit less than a whole count as part
 * full, so surfaces don't jump a cell at a time.
 *
 * Only cells where something changed are worked on, so still fluid costs nothing. Digging wakes the cells around the
 * hole, which is how bombs and worms let a pool run out. Lava flows too, but only every so many updates, so it creeps.
 * Different fluids don't flow into each other.
 *
 * Caves are a whole number of squares wide and tall, and cells are half a square, so the cells of caves side by side
 * line up. A cell at a cave's side flows into the cell next to it in the next cave, as it would into one of its own,
 * and waking a cell at the side wakes the one across it. Where there's no cave, the side is a wall.
 *
 * It starts as the cave's pools, left to settle when the cave is made, away from the main thread, when its sides are
 * all walls, as the world isn't touched from there. Pools are made away from the sides, so that doesn't matter. From
 * the moment it joins the world only the main thread touches it.
 */
public class CaveFluid {

    // How much a cell holds, how much more a cell under a full one can, and less than what's as good as nothing
    private static final float FULL = 1, SQUEEZE = 0.02f, NOTHING = 0.0001f;
    // Flows bigger than this are halved, which keeps it from sloshing forever, and the most that moves in an update
    private static final float SMOOTH = 0.005f, FASTEST = 1;
    // Changes smaller than this let a cell rest, and cells with less than this aren't drawn
    private static final float STILL = 0.0005f, SHOWN = 0.02f;
    // Lava only flows every so many updates, and how many updates settling a new cave's pools may take at most
    private static final int LAVA_EVERY = 6, SETTLING = 600;
    private static final int WATER = Color.argb(150, 50, 120, 200), WATER_SURFACE = Color.argb(170, 190, 230, 255),
            LAVA = Color.argb(245, 238, 92, 24), LAVA_SURFACE = Color.rgb(255, 214, 110), LAVA_GLOW = Color.argb(90, 255, 120, 30);

    private final Cave cave;
    private final float cell;
    private final int columns, rows;
    private final float[] mass, next;
    private final byte[] kind;
    private final boolean[] solid;
    // The cells to work on next update, without repeats
    private int[] active = new int[64], waking = new int[64];
    private int activeCount = 0, wakingCount = 0, steps = 0;
    private final boolean[] awake;
    private final Path waterPath = new Path(), lavaPath = new Path(), waterTop = new Path(), lavaTop = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Whether it's in the world, so it can flow into the caves beside it
    private boolean joined = false;
    // The cell a flow goes to, which may be in the next cave, as found by target
    private CaveFluid targetFluid;
    private int targetCell;

    CaveFluid(Cave cave, Map map) {
        this.cave = cave;
        cell = (float) (Map.SQUARE_SIZE / 2);
        columns = (int) Math.ceil(cave.width / cell);
        rows = (int) Math.ceil(cave.height / cell);
        mass = new float[columns * rows];
        next = new float[columns * rows];
        kind = new byte[columns * rows];
        solid = new boolean[columns * rows];
        awake = new boolean[columns * rows];
        for (int i = 0; i < columns; i++) {
            for (int j = 0; j < rows; j++) {
                solid[j * columns + i] = findSolid(i, j);
            }
        }
        // A pool's tile reaches half a tile either side of its middle, which is two cells each way. Its surface is on
        // a cell's top
        for (FluidPool pool : map.getPools()) {
            for (Coord tile : pool.tiles) {
                for (int i = 2 * tile.tileX - 1; i <= 2 * tile.tileX; i++) {
                    for (int j = 2 * tile.tileY - 1; j <= 2 * tile.tileY; j++) {
                        if (isInside(i, j) && !solid[j * columns + i]) {
                            mass[j * columns + i] = FULL;
                            kind[j * columns + i] = (byte) pool.type;
                            wake(j * columns + i);
                        }
                    }
                }
            }
        }
        System.arraycopy(mass, 0, next, 0, mass.length);
        for (int step = 0; step < SETTLING && wakingCount > 0; step++) {
            step();
        }
    }

    // Rock over most of the cell, going by its middle and four points around it
    private boolean findSolid(int i, int j) {
        float x = (i + 0.5f) * cell, y = (j + 0.5f) * cell, d = cell / 4;
        int rock = 0;
        rock += isRock(x, y) ? 1 : 0;
        rock += isRock(x - d, y - d) ? 1 : 0;
        rock += isRock(x + d, y - d) ? 1 : 0;
        rock += isRock(x - d, y + d) ? 1 : 0;
        rock += isRock(x + d, y + d) ? 1 : 0;
        return rock >= 3;
    }

    private boolean isRock(float x, float y) {
        int px = Math.min(cave.width - 1, Math.max(0, (int) x)), py = Math.min(cave.height - 1, Math.max(0, (int) y));
        return cave.isSolid(px, py);
    }

    private boolean isInside(int i, int j) {
        return i >= 0 && j >= 0 && i < columns && j < rows;
    }

    // A hole was dug, so the cells it reached are looked at again, and the fluid around them may flow into it
    void dug(float centerX, float centerY, float radius) {
        int left = Math.max(0, (int) ((centerX - radius) / cell) - 1), right = Math.min(columns - 1, (int) ((centerX + radius) / cell) + 1);
        int top = Math.max(0, (int) ((centerY - radius) / cell) - 1), bottom = Math.min(rows - 1, (int) ((centerY + radius) / cell) + 1);
        for (int i = left; i <= right; i++) {
            for (int j = top; j <= bottom; j++) {
                int c = j * columns + i;
                boolean was = solid[c];
                solid[c] = findSolid(i, j);
                if (was != solid[c]) {
                    wakeAround(i, j);
                }
            }
        }
    }

    private void wakeAround(int i, int j) {
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                if (target(i + di, j + dj)) {
                    targetFluid.wake(targetCell);
                }
            }
        }
    }

    // It's in the world now, so the fluid along its sides, and along the sides of the caves next to it, can flow
    // across. That's water that had reached a side where there was no cave yet, or that's in the new cave
    void join() {
        joined = true;
        for (int i = 0; i < columns; i++) {
            wakeAround(i, 0);
            wakeAround(i, rows - 1);
        }
        for (int j = 0; j < rows; j++) {
            wakeAround(0, j);
            wakeAround(columns - 1, j);
        }
    }

    // Finds the cell at the given place, counted from this cave's corner, which past its sides is in the cave next to
    // it, if there's one. Tells whether there's a cell there
    private boolean target(int i, int j) {
        if (isInside(i, j)) {
            targetFluid = this;
            targetCell = j * columns + i;
            return true;
        }
        if (!joined) {
            return false;
        }
        double x = cave.x + (i + 0.5) * cell, y = cave.y + (j + 0.5) * cell;
        Cave next = World.getCaveAt(x, y);
        if (next == null || next == cave) {
            return false;
        }
        CaveFluid other = next.getFluidGrid();
        int oi = (int) Math.floor((x - next.x) / other.cell), oj = (int) Math.floor((y - next.y) / other.cell);
        if (!other.isInside(oi, oj)) {
            return false;
        }
        targetFluid = other;
        targetCell = oj * other.columns + oi;
        return true;
    }

    private void wake(int c) {
        if (awake[c]) {
            return;
        }
        awake[c] = true;
        if (wakingCount == waking.length) {
            waking = Arrays.copyOf(waking, waking.length * 2);
        }
        waking[wakingCount++] = c;
    }

    // One update of flowing, of the cells that are awake
    void step() {
        steps++;
        // The cells woken last update are the ones worked on now, and they wake whatever changes this time
        int[] swap = active;
        active = waking;
        activeCount = wakingCount;
        waking = swap;
        wakingCount = 0;
        for (int n = 0; n < activeCount; n++) {
            awake[active[n]] = false;
        }
        for (int n = 0; n < activeCount; n++) {
            flowFrom(active[n]);
        }
        for (int n = 0; n < activeCount; n++) {
            settle(active[n]);
        }
    }

    private void flowFrom(int c) {
        float remaining = mass[c];
        if (remaining <= 0 || solid[c]) {
            return;
        }
        byte type = kind[c];
        if (type == FluidPool.LAVA && steps % LAVA_EVERY != 0) {
            // Still there, so it's looked at again when lava next flows
            wake(c);
            return;
        }
        int i = c % columns, j = c / columns;
        if (target(i, j + 1) && targetFluid.canTake(targetCell, type)) {
            float below = targetFluid.mass[targetCell];
            float flow = limit(stable(remaining + below) - below, Math.min(FASTEST, remaining));
            move(c, flow, type);
            remaining -= flow;
        }
        if (remaining <= 0) {
            return;
        }
        if (target(i - 1, j) && targetFluid.canTake(targetCell, type)) {
            float flow = limit((mass[c] - targetFluid.mass[targetCell]) / 4, remaining);
            move(c, flow, type);
            remaining -= flow;
        }
        if (remaining <= 0) {
            return;
        }
        if (target(i + 1, j) && targetFluid.canTake(targetCell, type)) {
            float flow = limit((mass[c] - targetFluid.mass[targetCell]) / 4, remaining);
            move(c, flow, type);
            remaining -= flow;
        }
        if (remaining <= 0) {
            return;
        }
        if (target(i, j - 1) && targetFluid.canTake(targetCell, type)) {
            float above = targetFluid.mass[targetCell];
            float flow = limit(remaining - stable(remaining + above), Math.min(FASTEST, remaining));
            move(c, flow, type);
        }
    }

    // Open, and empty or holding the same fluid
    private boolean canTake(int c, byte type) {
        return !solid[c] && (mass[c] < NOTHING || kind[c] == type);
    }

    // How much of the two cells' fluid the lower one keeps: all of it up to a whole cell, and a little more than the
    // upper one once both are full
    private static float stable(float total) {
        if (total <= FULL) {
            return FULL;
        } else if (total < 2 * FULL + SQUEEZE) {
            return (FULL * FULL + total * SQUEEZE) / (FULL + SQUEEZE);
        }
        return (total + SQUEEZE) / 2;
    }

    private static float limit(float flow, float most) {
        if (flow > SMOOTH) {
            flow *= 0.5f;
        }
        return Math.max(0, Math.min(flow, most));
    }

    // To the cell target last found. In another cave, that cave takes it in when it next flows, so it's woken
    private void move(int from, float flow, byte type) {
        if (flow <= 0) {
            return;
        }
        next[from] -= flow;
        targetFluid.next[targetCell] += flow;
        targetFluid.kind[targetCell] = type;
        if (targetFluid != this) {
            targetFluid.wake(targetCell);
        }
    }

    // What flowed is taken in, and the cells that changed, and those around them, are woken for the next update
    private void settle(int c) {
        settleCell(c);
        int i = c % columns, j = c / columns;
        if (i > 0) settleCell(c - 1);
        if (i + 1 < columns) settleCell(c + 1);
        if (j > 0) settleCell(c - columns);
        if (j + 1 < rows) settleCell(c + columns);
    }

    private void settleCell(int c) {
        float change = next[c] - mass[c];
        if (change == 0) {
            return;
        }
        mass[c] = next[c];
        if (mass[c] < NOTHING) {
            mass[c] = 0;
            next[c] = 0;
            kind[c] = FluidPool.NONE;
        }
        if (Math.abs(change) > STILL) {
            wakeAround(c % columns, c / columns);
        }
    }

    // Water, lava or neither at a point of the cave, counted from its own corner. A part full cell is filled from its
    // bottom up
    int getFluid(double localX, double localY) {
        int i = (int) Math.floor(localX / cell), j = (int) Math.floor(localY / cell);
        if (!isInside(i, j)) {
            return FluidPool.NONE;
        }
        int c = j * columns + i;
        if (mass[c] < SHOWN) {
            return FluidPool.NONE;
        }
        float surface = (j + 1 - Math.min(FULL, mass[c])) * cell;
        return (localY >= surface) ? kind[c] : FluidPool.NONE;
    }

    // Where the fluid a point is in comes up to, in the cave's own space: the top of the column of fluid over it
    float getSurface(double localX, double localY) {
        int i = (int) Math.floor(localX / cell), j = (int) Math.floor(localY / cell);
        if (!isInside(i, j) || getFluid(localX, localY) == FluidPool.NONE) {
            return Float.NaN;
        }
        while (j > 0 && mass[(j - 1) * columns + i] >= SHOWN && !solid[(j - 1) * columns + i]) {
            j--;
        }
        return (j + 1 - Math.min(FULL, mass[j * columns + i])) * cell;
    }

    /**
     * Draws the fluid that shows in a part of the world, which starts at the canvas' top left corner. It's drawn before
     * the rock, which covers its sides and bottom, so its edges follow the rock's smooth outline. Each column's fluid is
     * a run of cells from where it comes up to down to the rock, and neighbouring runs meet half way, so a surface
     * that slopes while it flows looks like one.
     */
    void draw(Canvas canvas, int localLeft, int localTop, int viewWidth, int viewHeight, int frame) {
        int firstColumn = Math.max(0, (int) (localLeft / cell) - 1), lastColumn = Math.min(columns - 1, (int) ((localLeft + viewWidth) / cell) + 1);
        int firstRow = Math.max(0, (int) (localTop / cell) - 1), lastRow = Math.min(rows - 1, (int) ((localTop + viewHeight) / cell) + 1);
        waterPath.rewind();
        lavaPath.rewind();
        waterTop.rewind();
        lavaTop.rewind();
        boolean any = false;
        for (int i = firstColumn; i <= lastColumn; i++) {
            int j = firstRow;
            while (j <= lastRow) {
                int c = j * columns + i;
                if (mass[c] < SHOWN || solid[c]) {
                    j++;
                    continue;
                }
                // A run of the same fluid down the column
                byte type = kind[c];
                int start = j;
                while (j + 1 <= lastRow && mass[(j + 1) * columns + i] >= SHOWN && kind[(j + 1) * columns + i] == type && !solid[(j + 1) * columns + i]) {
                    j++;
                }
                // It reaches half a cell into rock beside and below it, which covers it, so no gap is left between the
                // two where the rock's edge cuts across a cell. Not into open cells, like beside a falling stream
                float top = (start + 1 - Math.min(FULL, mass[c])) * cell;
                float bottom = (j + 1) * cell + ((j + 1 < rows && solid[(j + 1) * columns + i]) ? cell / 2 : 0);
                boolean open = start == 0 || mass[(start - 1) * columns + i] < SHOWN;
                float left = i * cell - (reaches(i - 1, start) ? cell / 2 : 0), right = (i + 1) * cell + (reaches(i + 1, start) ? cell / 2 : 0);
                float leftTop = open ? meet(i - 1, start, top, type) : top, rightTop = open ? meet(i + 1, start, top, type) : top;
                // One path for each fluid, so where columns overlap it isn't drawn twice
                Path body = (type == FluidPool.LAVA) ? lavaPath : waterPath;
                body.moveTo(left, leftTop);
                body.lineTo(i * cell + cell / 2, top);
                body.lineTo(right, rightTop);
                body.lineTo(right, bottom);
                body.lineTo(left, bottom);
                body.close();
                if (open) {
                    Path line = (type == FluidPool.LAVA) ? lavaTop : waterTop;
                    line.moveTo(left + cell / 2, (leftTop + top) / 2);
                    line.lineTo(i * cell + cell / 2, top);
                    line.lineTo(right - cell / 2, (rightTop + top) / 2);
                    if (type == FluidPool.LAVA && i % 4 == 0) {
                        float flicker = 1 + 0.1f * (float) Math.sin(frame * 0.13 + i);
                        DrawUtil.drawGlow(canvas, glowPaint, i * cell + cell / 2 - localLeft, top - localTop, cell * 4 * flicker, LAVA_GLOW);
                    }
                }
                any = true;
                j++;
            }
        }
        if (!any) {
            return;
        }
        canvas.save();
        canvas.translate(-localLeft, -localTop);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(WATER);
        canvas.drawPath(waterPath, paint);
        paint.setColor(LAVA);
        canvas.drawPath(lavaPath, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(cell * 0.18f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int pulse = (int) (40 * Math.sin(frame * 0.05));
        paint.setColor(DrawUtil.withAlpha(WATER_SURFACE, 130 + pulse));
        canvas.drawPath(waterTop, paint);
        paint.setColor(DrawUtil.withAlpha(LAVA_SURFACE, 215 + pulse / 2));
        canvas.drawPath(lavaTop, paint);
        canvas.restore();
    }

    // Whether a column's fluid can reach into the cell beside it: rock, or more fluid
    private boolean reaches(int i, int j) {
        return i >= 0 && i < columns && (solid[j * columns + i] || mass[j * columns + i] >= SHOWN);
    }

    // Where a surface meets the one next to it: half way, if that column's fluid comes up near the same height
    private float meet(int i, int row, float top, byte type) {
        if (i < 0 || i >= columns) {
            return top;
        }
        for (int j = Math.max(0, row - 1); j <= Math.min(rows - 1, row + 1); j++) {
            int c = j * columns + i;
            boolean open = j == 0 || mass[(j - 1) * columns + i] < SHOWN;
            if (mass[c] >= SHOWN && kind[c] == type && !solid[c] && open) {
                float other = (j + 1 - Math.min(FULL, mass[c])) * cell;
                if (Math.abs(other - top) < cell * 1.5f) {
                    return (top + other) / 2;
                }
            }
        }
        return top;
    }
}
