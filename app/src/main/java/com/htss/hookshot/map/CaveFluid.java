package com.htss.hookshot.map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.htss.hookshot.util.DrawUtil;

/**
 * The water and lava in a cave: a fluid grid of cells half a tile wide over it, which does the flowing, filled with the
 * cave's pools, and kept up with the rock as it's dug. It also answers what's where in the cave's own space, and
 * draws it.
 *
 * Caves are a whole number of squares wide and tall, and cells are half a square, so the cells of caves side by side
 * line up, and the grids flow into each other. Where there's no cave, the side is a wall.
 *
 * It starts as the cave's pools, left to settle when the cave is made, away from the main thread, when its sides are
 * all walls, as the world isn't touched from there. Pools are made away from the sides, so that doesn't matter. From
 * the moment it joins the world only the main thread touches it.
 */
public class CaveFluid {

    // How many updates settling a new cave's pools may take at most
    private static final int SETTLING = 600;
    private static final int WATER = Color.argb(150, 50, 120, 200), WATER_SURFACE = Color.argb(170, 190, 230, 255),
            LAVA = Color.argb(245, 238, 92, 24), LAVA_SURFACE = Color.rgb(255, 214, 110), LAVA_GLOW = Color.argb(90, 255, 120, 30);

    private final Cave cave;
    private final float cell;
    final FluidGrid grid;
    private final Path waterPath = new Path(), lavaPath = new Path(), waterTop = new Path(), lavaTop = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    CaveFluid(Cave cave, Map map) {
        this.cave = cave;
        cell = (float) (Map.SQUARE_SIZE / 2);
        grid = new FluidGrid((int) Math.ceil(cave.width / cell), (int) Math.ceil(cave.height / cell));
        for (int i = 0; i < grid.columns; i++) {
            for (int j = 0; j < grid.rows; j++) {
                grid.setSolidAtFirst(i, j, findSolid(i, j));
            }
        }
        // A pool's tile reaches half a tile either side of its middle, which is two cells each way. Its surface is on
        // a cell's top
        for (FluidPool pool : map.getPools()) {
            for (Coord tile : pool.tiles) {
                for (int i = 2 * tile.tileX - 1; i <= 2 * tile.tileX; i++) {
                    for (int j = 2 * tile.tileY - 1; j <= 2 * tile.tileY; j++) {
                        if (grid.isInside(i, j)) {
                            grid.fill(i, j, pool.type, FluidGrid.FULL);
                        }
                    }
                }
            }
        }
        grid.settleAtFirst(SETTLING);
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

    // A hole was dug, so the cells it reached are looked at again, and the fluid around them may flow into it
    void dug(float centerX, float centerY, float radius) {
        int left = Math.max(0, (int) ((centerX - radius) / cell) - 1), right = Math.min(grid.columns - 1, (int) ((centerX + radius) / cell) + 1);
        int top = Math.max(0, (int) ((centerY - radius) / cell) - 1), bottom = Math.min(grid.rows - 1, (int) ((centerY + radius) / cell) + 1);
        for (int i = left; i <= right; i++) {
            for (int j = top; j <= bottom; j++) {
                grid.setSolid(i, j, findSolid(i, j));
            }
        }
    }

    // It's in the world now, so it flows into the caves beside it, which are found by where their cells are
    void join() {
        grid.join(new FluidGrid.Neighbours() {
            @Override
            public FluidGrid find(int i, int j, int[] found) {
                double x = cave.x + (i + 0.5) * cell, y = cave.y + (j + 0.5) * cell;
                Cave next = World.getCaveAt(x, y);
                if (next == null || next == cave) {
                    return null;
                }
                CaveFluid other = next.getFluidGrid();
                int oi = (int) Math.floor((x - next.x) / other.cell), oj = (int) Math.floor((y - next.y) / other.cell);
                if (!other.grid.isInside(oi, oj)) {
                    return null;
                }
                found[0] = oj * other.grid.columns + oi;
                return other.grid;
            }
        });
    }

    void step() {
        grid.step();
    }

    FluidGrid.Saved save() {
        return grid.save();
    }

    void load(FluidGrid.Saved saved) {
        grid.load(saved);
    }

    // Water, lava or neither at a point of the cave, counted from its own corner. A part full cell is filled from its
    // bottom up
    int getFluid(double localX, double localY) {
        int i = (int) Math.floor(localX / cell), j = (int) Math.floor(localY / cell);
        if (!grid.isInside(i, j)) {
            return FluidGrid.NONE;
        }
        float mass = grid.getMass(i, j);
        if (mass < FluidGrid.SHOWN) {
            return FluidGrid.NONE;
        }
        float surface = (j + 1 - Math.min(FluidGrid.FULL, mass)) * cell;
        return (localY >= surface) ? grid.getKind(i, j) : FluidGrid.NONE;
    }

    // Where the fluid a point is in comes up to, in the cave's own space: the top of the column of fluid over it
    float getSurface(double localX, double localY) {
        int i = (int) Math.floor(localX / cell), j = (int) Math.floor(localY / cell);
        if (!grid.isInside(i, j) || getFluid(localX, localY) == FluidGrid.NONE) {
            return Float.NaN;
        }
        while (j > 0 && grid.getMass(i, j - 1) >= FluidGrid.SHOWN && !grid.isSolid(i, j - 1)) {
            j--;
        }
        return (j + 1 - Math.min(FluidGrid.FULL, grid.getMass(i, j))) * cell;
    }

    /**
     * Draws the fluid that shows in a part of the world, which starts at the canvas' top left corner. It's drawn before
     * the rock, which covers its sides and bottom, so its edges follow the rock's smooth outline. Each column's fluid is
     * a run of cells from where it comes up to down to the rock, and neighbouring runs meet half way, so a surface
     * that slopes while it flows looks like one.
     */
    void draw(Canvas canvas, int localLeft, int localTop, int viewWidth, int viewHeight, int frame) {
        int firstColumn = Math.max(0, (int) (localLeft / cell) - 1), lastColumn = Math.min(grid.columns - 1, (int) ((localLeft + viewWidth) / cell) + 1);
        int firstRow = Math.max(0, (int) (localTop / cell) - 1), lastRow = Math.min(grid.rows - 1, (int) ((localTop + viewHeight) / cell) + 1);
        waterPath.rewind();
        lavaPath.rewind();
        waterTop.rewind();
        lavaTop.rewind();
        boolean any = false;
        for (int i = firstColumn; i <= lastColumn; i++) {
            int j = firstRow;
            while (j <= lastRow) {
                int c = j * grid.columns + i;
                if (grid.mass[c] < FluidGrid.SHOWN || grid.solid[c]) {
                    j++;
                    continue;
                }
                // A run of the same fluid down the column
                byte type = grid.kind[c];
                int start = j;
                while (j + 1 <= lastRow && grid.mass[(j + 1) * grid.columns + i] >= FluidGrid.SHOWN && grid.kind[(j + 1) * grid.columns + i] == type && !grid.solid[(j + 1) * grid.columns + i]) {
                    j++;
                }
                // It reaches half a cell into rock beside and below it, which covers it, so no gap is left between the
                // two where the rock's edge cuts across a cell. Not into open cells, like beside a falling stream
                float top = (start + 1 - Math.min(FluidGrid.FULL, grid.mass[c])) * cell;
                float bottom = (j + 1) * cell + ((j + 1 < grid.rows && grid.solid[(j + 1) * grid.columns + i]) ? cell / 2 : 0);
                boolean open = start == 0 || grid.mass[(start - 1) * grid.columns + i] < FluidGrid.SHOWN;
                float left = i * cell - (reaches(i - 1, start) ? cell / 2 : 0), right = (i + 1) * cell + (reaches(i + 1, start) ? cell / 2 : 0);
                float leftTop = open ? meet(i - 1, start, top, type) : top, rightTop = open ? meet(i + 1, start, top, type) : top;
                // One path for each fluid, so where grid.columns overlap it isn't drawn twice
                Path body = (type == FluidGrid.LAVA) ? lavaPath : waterPath;
                body.moveTo(left, leftTop);
                body.lineTo(i * cell + cell / 2, top);
                body.lineTo(right, rightTop);
                body.lineTo(right, bottom);
                body.lineTo(left, bottom);
                body.close();
                if (open) {
                    Path line = (type == FluidGrid.LAVA) ? lavaTop : waterTop;
                    line.moveTo(left + cell / 2, (leftTop + top) / 2);
                    line.lineTo(i * cell + cell / 2, top);
                    line.lineTo(right - cell / 2, (rightTop + top) / 2);
                    if (type == FluidGrid.LAVA && i % 4 == 0) {
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
        return i >= 0 && i < grid.columns && (grid.solid[j * grid.columns + i] || grid.mass[j * grid.columns + i] >= FluidGrid.SHOWN);
    }

    // Where a surface meets the one next to it: half way, if that column's fluid comes up near the same height
    private float meet(int i, int row, float top, byte type) {
        if (i < 0 || i >= grid.columns) {
            return top;
        }
        for (int j = Math.max(0, row - 1); j <= Math.min(grid.rows - 1, row + 1); j++) {
            int c = j * grid.columns + i;
            boolean open = j == 0 || grid.mass[(j - 1) * grid.columns + i] < FluidGrid.SHOWN;
            if (grid.mass[c] >= FluidGrid.SHOWN && grid.kind[c] == type && !grid.solid[c] && open) {
                float other = (j + 1 - Math.min(FluidGrid.FULL, grid.mass[c])) * cell;
                if (Math.abs(other - top) < cell * 1.5f) {
                    return (top + other) / 2;
                }
            }
        }
        return top;
    }
}
