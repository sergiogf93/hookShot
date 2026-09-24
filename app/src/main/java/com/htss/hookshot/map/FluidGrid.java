package com.htss.hookshot.map;

import java.util.Arrays;

/**
 * How water and lava flow, on a grid of cells, each holding some of one fluid, a whole cell's worth being 1. A few
 * times every update fluid falls into the cell below, then evens out with the cells beside it, and what's more than a
 * cell holds pushes up into the cell above. Cells with a bit less than a whole count as part full, so surfaces don't
 * jump a cell at a time.
 *
 * That alone evens out a wide pool slowly, leaves a draining pool sloping down to where it drains, piles up water where
 * a stream lands, and never raises water in the other arm of a bend. So water that's held up, by rock or by full water
 * under it, is levelled too, even while water falls into it or out of it, which the flowing does. Where it would settle
 * is worked out: its water poured back into the space it's in from the bottom up, which gives it the flat surface it
 * would have, the same in both arms of a bend. Then every update a share of the water that isn't there yet goes, from
 * the top of where there's too much to the bottom of where there's too little, so a high pool's surface sinks and a
 * low one's rises, as water pushed through between them would, slowing as they come level. The space it levels into
 * is over the water, and hollows with a floor, but only a cell further to the side than the water reaches, so water
 * spreading over a floor still flows there rather than appearing. Not lava, which is slow anyway, nor water at the
 * grid's side, which may be flowing across.
 *
 * Only cells where something changed are worked on, so still fluid costs nothing. Changing which cells are rock wakes
 * the cells around them. Lava flows too, but only every so many passes, so it creeps. Different fluids don't flow into
 * each other.
 *
 * Past its sides it can flow into other grids, which neighbours find, if it has any. Without them its sides are walls.
 *
 * It knows nothing of the game, so it can be tried out on its own.
 */
public class FluidGrid {

    public static final int NONE = 0, WATER = 1, LAVA = 2;

    // How much a cell holds, how much more a cell under a full one can, and less than what's as good as nothing. Water
    // isn't squeezed: its pressure is left to the levelling, which fills cells to exactly full, and water squeezed
    // below that would keep flowing after every levelling
    static final float FULL = 1, SQUEEZE = 0, NOTHING = 0.0001f;
    // The most that moves between two cells in a pass, and how many passes an update has, which is how fast it flows
    private static final float FASTEST = 1, DAMPED = 0.005f;
    private static final int PASSES = 3;
    // Changes smaller than this let a cell rest, and cells with less than this aren't drawn
    static final float STILL = 0.0005f, SHOWN = 0.02f;
    // Lava only flows every so many passes
    private static final int LAVA_EVERY = 16;
    // How often, in updates, the water that came to rest is levelled, how full a cell with water over it has to be for
    // it not to be falling, and how much more room than water a body can have around it and still be levelled, as
    // more means it's pouring out somewhere
    private static final int LEVELLING_EVERY = 1;
    private static final float FALLING = 0.9f, MOST_ROOM = 1.5f;
    // How much of the water that's out of place goes where it settles every update, at least how much, how little is left
    // before it's put there all at once, and how many cells a surface can sink or rise by in an update, however far out
    // of place the water is
    private static final float EASING = 0.15f, LEAST_EASING = 0.05f, EASED = 0.02f, SURFACE_SPEED = 0.2f;

    // Finds the grid and cell past this grid's sides, if any
    public interface Neighbours {
        // The grid with the cell at the given place, counted from this grid's corner, with that cell's index put in
        // the given array, or none
        FluidGrid find(int i, int j, int[] cell);
    }

    final int columns, rows;
    final float[] mass, next;
    final byte[] kind;
    final boolean[] solid;
    // The cells to work on next update, without repeats
    private int[] active = new int[64], waking = new int[64];
    private int activeCount = 0, wakingCount = 0, steps = 0;
    private final boolean[] awake;
    // The cells that changed since water was last levelled, and what levelling it goes through: the body found, the room
    // it has, and which cells were already looked at, marked with a number for each search
    private final boolean[] dirty;
    private int[] dirtyCells = new int[64];
    private int dirtyCount = 0, updates = 0, levelling = 0;
    private final int[] body, room, seen;
    // Where levelling puts water, and how much is taken from and given to each row, from the top down
    private final float[] goal, rowSurplus, rowShortfall;
    private final int[] goalMark;
    private int goals = 0;
    // What's past its sides, once it has any, and whether its fluid has changed since then, so it has to be remembered
    private Neighbours neighbours = null;
    private boolean changed = false;
    // The cell a flow goes to, which may be in the next grid, as found by target
    private FluidGrid targetGrid;
    private int targetCell;
    private final int[] found = new int[1];

    public FluidGrid(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        mass = new float[columns * rows];
        next = new float[columns * rows];
        kind = new byte[columns * rows];
        solid = new boolean[columns * rows];
        awake = new boolean[columns * rows];
        dirty = new boolean[columns * rows];
        body = new int[columns * rows];
        room = new int[columns * rows];
        seen = new int[columns * rows];
        goal = new float[columns * rows];
        goalMark = new int[columns * rows];
        rowSurplus = new float[rows];
        rowShortfall = new float[rows];
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public boolean isInside(int i, int j) {
        return i >= 0 && j >= 0 && i < columns && j < rows;
    }

    public float getMass(int i, int j) {
        return mass[j * columns + i];
    }

    public int getKind(int i, int j) {
        return kind[j * columns + i];
    }

    public boolean isSolid(int i, int j) {
        return solid[j * columns + i];
    }

    // Before it flows, as it's made: rock, and fluid, which is woken to settle
    public void setSolidAtFirst(int i, int j, boolean rock) {
        solid[j * columns + i] = rock;
    }

    public void fill(int i, int j, int type, float amount) {
        int c = j * columns + i;
        if (solid[c]) {
            return;
        }
        mass[c] = amount;
        next[c] = amount;
        kind[c] = (byte) type;
        wake(c);
    }

    // Rock appears or goes, so the fluid around it may flow
    public void setSolid(int i, int j, boolean rock) {
        int c = j * columns + i;
        if (solid[c] != rock) {
            solid[c] = rock;
            wakeAround(i, j);
        }
    }

    // Settles what it was filled with, before it's in the world
    public void settleAtFirst(int mostUpdates) {
        for (int update = 0; update < mostUpdates && wakingCount > 0; update++) {
            step();
        }
    }

    // Whether any of it is still flowing, or being levelled
    public boolean isFlowing() {
        return wakingCount > 0 || dirtyCount > 0;
    }

    // It's in the world now, so the fluid along its sides, and along the sides of the grids next to it, can flow
    // across. That's water that had reached a side where there was no grid yet, or that's in the new grid
    public void join(Neighbours neighbours) {
        this.neighbours = neighbours;
        for (int i = 0; i < columns; i++) {
            wakeAround(i, 0);
            wakeAround(i, rows - 1);
        }
        for (int j = 0; j < rows; j++) {
            wakeAround(0, j);
            wakeAround(columns - 1, j);
        }
    }

    public void wakeAround(int i, int j) {
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                if (target(i + di, j + dj)) {
                    targetGrid.wake(targetCell);
                }
            }
        }
    }

    // Finds the cell at the given place, counted from this grid's corner, which past its sides is in the grid next to
    // it, if there's one. Tells whether there's a cell there
    private boolean target(int i, int j) {
        if (isInside(i, j)) {
            targetGrid = this;
            targetCell = j * columns + i;
            return true;
        }
        if (neighbours == null) {
            return false;
        }
        FluidGrid other = neighbours.find(i, j, found);
        if (other == null || other == this) {
            return false;
        }
        targetGrid = other;
        targetCell = found[0];
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

    // One update of flowing, of the cells that are awake, and now and then levelling what has come to rest
    public void step() {
        for (int pass = 0; pass < PASSES; pass++) {
            flowOnce();
        }
        updates++;
        if (updates % LEVELLING_EVERY == 0 && dirtyCount > 0) {
            level();
        }
    }

    private void flowOnce() {
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
        if (type == LAVA && steps % LAVA_EVERY != 0) {
            // Still there, so it's looked at again when lava next flows
            wake(c);
            return;
        }
        int i = c % columns, j = c / columns;
        if (target(i, j + 1) && targetGrid.canTake(targetCell, type)) {
            float below = targetGrid.mass[targetCell];
            float flow = limit(damp(stable(remaining + below) - below, below), Math.min(FASTEST, remaining));
            move(c, flow, type);
            remaining -= flow;
        }
        if (remaining <= 0) {
            return;
        }
        if (target(i - 1, j) && targetGrid.canTake(targetCell, type)) {
            float flow = limit((mass[c] - targetGrid.mass[targetCell]) / 4, remaining);
            move(c, flow, type);
            remaining -= flow;
        }
        if (remaining <= 0) {
            return;
        }
        if (target(i + 1, j) && targetGrid.canTake(targetCell, type)) {
            float flow = limit((mass[c] - targetGrid.mass[targetCell]) / 4, remaining);
            move(c, flow, type);
            remaining -= flow;
        }
        if (remaining <= 0) {
            return;
        }
        if (target(i, j - 1) && targetGrid.canTake(targetCell, type)) {
            float above = targetGrid.mass[targetCell];
            float flow = limit(damp(remaining - stable(remaining + above), above), Math.min(FASTEST, remaining));
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

    // Between two cells that both hold some, each pushes on the other at once, which overshoots and sloshes up and down
    // for ever, so bigger flows between them are halved. Falling into an empty cell, or sideways, it can't overshoot
    private static float damp(float flow, float other) {
        return (other >= NOTHING && flow > DAMPED) ? flow * 0.5f : flow;
    }

    private static float limit(float flow, float most) {
        return Math.max(0, Math.min(flow, most));
    }

    // To the cell target last found. In another grid, that grid takes it in when it next flows, so it's woken
    private void move(int from, float flow, byte type) {
        if (flow <= 0) {
            return;
        }
        next[from] -= flow;
        targetGrid.next[targetCell] += flow;
        targetGrid.kind[targetCell] = type;
        if (targetGrid != this) {
            targetGrid.wake(targetCell);
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
            kind[c] = NONE;
        }
        if (Math.abs(change) > STILL) {
            wakeAround(c % columns, c / columns);
        }
        markDirty(c);
        changed = changed || neighbours != null;
    }

    // What's in it, as it is: the cells with fluid, how much and which. Kept with the size of the grid, as that comes
    // from the screen's and could be another the next time the game is opened
    public static class Saved {
        final int columns, rows;
        final int[] cells;
        final float[] masses;
        final byte[] kinds;

        Saved(int columns, int rows, int[] cells, float[] masses, byte[] kinds) {
            this.columns = columns;
            this.rows = rows;
            this.cells = cells;
            this.masses = masses;
            this.kinds = kinds;
        }
    }

    // Its fluid if it changed since it joined the world, or none, as then it's as it was made anyway
    public Saved save() {
        if (!changed) {
            return null;
        }
        int count = 0;
        for (float m : mass) {
            if (m >= NOTHING) {
                count++;
            }
        }
        int[] cells = new int[count];
        float[] masses = new float[count];
        byte[] kinds = new byte[count];
        int n = 0;
        for (int c = 0; c < mass.length; c++) {
            if (mass[c] >= NOTHING) {
                cells[n] = c;
                masses[n] = mass[c];
                kinds[n] = kind[c];
                n++;
            }
        }
        return new Saved(columns, rows, cells, masses, kinds);
    }

    // Puts back the fluid as it was remembered, in place of what it was made with. Woken, in case it was still
    // flowing. Not if the grid is another size now
    public void load(Saved saved) {
        if (saved.columns != columns || saved.rows != rows) {
            return;
        }
        Arrays.fill(mass, 0);
        Arrays.fill(next, 0);
        Arrays.fill(kind, (byte) NONE);
        for (int n = 0; n < saved.cells.length; n++) {
            int c = saved.cells[n];
            if (c >= 0 && c < mass.length && !solid[c]) {
                mass[c] = saved.masses[n];
                next[c] = saved.masses[n];
                kind[c] = saved.kinds[n];
                wake(c);
            }
        }
        changed = true;
    }

    private void markDirty(int c) {
        if (dirty[c]) {
            return;
        }
        dirty[c] = true;
        if (dirtyCount == dirtyCells.length) {
            dirtyCells = Arrays.copyOf(dirtyCells, dirtyCells.length * 2);
        }
        dirtyCells[dirtyCount++] = c;
    }

    // Every body of water that changed since the last time, and has come to rest, is levelled
    private void level() {
        int count = dirtyCount;
        for (int n = 0; n < count; n++) {
            dirty[dirtyCells[n]] = false;
        }
        for (int n = 0; n < count; n++) {
            int c = dirtyCells[n];
            if (mass[c] >= NOTHING && kind[c] == WATER && seen[c] != levelling + 1 && isHeldUp(c)) {
                levelBody(c);
            }
        }
        // What levelling changed is kept, so it carries on next time
        System.arraycopy(dirtyCells, count, dirtyCells, 0, dirtyCount - count);
        dirtyCount -= count;
        levelling++;
    }

    // The body of water a cell is in: its cells, found by going from cell to cell of water
    private int findBody(int start, int mark) {
        int count = 0;
        body[count++] = start;
        seen[start] = mark;
        for (int n = 0; n < count; n++) {
            int c = body[n], i = c % columns, j = c / columns;
            int[] around = {j > 0 ? c - columns : -1, j + 1 < rows ? c + columns : -1, i > 0 ? c - 1 : -1, i + 1 < columns ? c + 1 : -1};
            for (int d : around) {
                if (d >= 0 && seen[d] != mark && mass[d] >= NOTHING && kind[d] == WATER && isHeldUp(d)) {
                    seen[d] = mark;
                    body[count++] = d;
                }
            }
        }
        return count;
    }

    // Water with rock or full water under it, which rests rather than falls
    private boolean isHeldUp(int c) {
        int below = c + columns;
        return below < mass.length && (solid[below] || mass[below] >= FALLING);
    }

    // An empty cell water could lie in: with rock or water under it
    private boolean hasFloor(int c) {
        int below = c + columns;
        return below < mass.length && (solid[below] || mass[below] >= NOTHING);
    }

    // Levels a body of water that's held up, unless it's at a side, or the space it would settle in is much bigger than
    // it
    private void levelBody(int start) {
        int size = findBody(start, levelling + 1);
        float volume = 0;
        int top = rows, bottom = -1;
        for (int n = 0; n < size; n++) {
            int c = body[n], i = c % columns, j = c / columns;
            if (i == 0 || j == 0 || i == columns - 1 || j == rows - 1) {
                return;
            }
            volume += mass[c];
            top = Math.min(top, j);
            bottom = Math.max(bottom, j);
        }
        // Raised a row at a time from its bottom, until there's more room from that row down than water. Then the
        // water fills the room under that row, and what's left lies evenly along it. Raising a row can reach another
        // hollow further down, and if that takes more water than there is, the water isn't at rest: it's left to flow
        int most = (int) (size * MOST_ROOM) + 8;
        for (int level = bottom; level >= top; level--) {
            int roomCount = findRoom(size, level, most);
            if (roomCount < 0) {
                return;
            }
            if (roomCount > volume) {
                int lower = 0, along = 0;
                for (int n = 0; n < roomCount; n++) {
                    if (room[n] / columns > level) {
                        lower++;
                    } else {
                        along++;
                    }
                }
                if (lower > volume || along == 0) {
                    return;
                }
                pour(size, level, roomCount, (volume - lower) / along);
                return;
            }
        }
        // More water than room up to its top: it's squeezed, and left as it is
    }

    // The cells joined to the body's cells from the given row down, without going above it, that its water could lie
    // in, counted into room, or -1 if there are more than the most. Water that's held up, and empty cells over it or
    // with a floor, but only a cell to the side of where there's water now
    private int findRoom(int size, int level, int most) {
        int mark = -(levelling * rows + level) - 2;
        int count = 0;
        for (int n = 0; n < size; n++) {
            int c = body[n];
            if (c / columns >= level && seen[c] != mark) {
                seen[c] = mark;
                room[count++] = c;
            }
        }
        for (int n = 0; n < count; n++) {
            int c = room[n], i = c % columns, j = c / columns;
            int[] around = {j > level ? c - columns : -1, j + 1 < rows ? c + columns : -1, i > 0 ? c - 1 : -1, i + 1 < columns ? c + 1 : -1};
            for (int k = 0; k < 4; k++) {
                int d = around[k];
                if (d < 0 || seen[d] == mark || solid[d]) {
                    continue;
                }
                boolean water = mass[d] >= NOTHING;
                if (water && (kind[d] != WATER || !isHeldUp(d))) {
                    continue;
                }
                // Empty: straight up is fine, down or across it needs a floor, and across only from water
                if (!water && (k == 1 && !hasFloor(d) || k >= 2 && (!hasFloor(d) || mass[c] < SHOWN))) {
                    continue;
                }
                seen[d] = mark;
                if (count == most) {
                    return -1;
                }
                room[count++] = d;
            }
        }
        return count;
    }

    // Moves the body's water towards the room found from the given row down: full below it, and the rest of the water
    // evenly along it. A share of what's out of place goes each time, taken from the top of where there's too much and
    // given to the bottom of where there's too little, so surfaces sink and rise smoothly. The last of it goes at once
    private void pour(int size, int level, int roomCount, float share) {
        goals++;
        for (int n = 0; n < roomCount; n++) {
            int c = room[n];
            goal[c] = (c / columns == level) ? share : FULL;
            goalMark[c] = goals;
        }
        Arrays.fill(rowSurplus, 0);
        Arrays.fill(rowShortfall, 0);
        float surplus = 0, shortfall = 0;
        for (int n = 0; n < size; n++) {
            int c = body[n];
            if (goalMark[c] != goals) {
                rowSurplus[c / columns] += mass[c];
                surplus += mass[c];
            }
        }
        for (int n = 0; n < roomCount; n++) {
            int c = room[n];
            float off = mass[c] - goal[c];
            if (off > 0) {
                rowSurplus[c / columns] += off;
                surplus += off;
            } else {
                rowShortfall[c / columns] -= off;
                shortfall -= off;
            }
        }
        // The room can take in water that isn't the body's, which it counts as too much, so no more goes than both
        // sides have, or water would be lost
        surplus = Math.min(surplus, shortfall);
        float moving = (surplus < EASED) ? surplus : Math.min(surplus, Math.max(LEAST_EASING, surplus * EASING));
        if (moving <= 0) {
            return;
        }
        // No faster than the surfaces can move: the top row with too much, and the bottom row with too little, each by
        // so many cells, times how wide they are
        int topRow = -1, bottomRow = -1;
        for (int r = 0; r < rows && topRow < 0; r++) {
            if (rowSurplus[r] > 0) topRow = r;
        }
        for (int r = rows - 1; r >= 0 && bottomRow < 0; r--) {
            if (rowShortfall[r] > 0) bottomRow = r;
        }
        int topWidth = 0, bottomWidth = 0;
        for (int n = 0; n < size; n++) {
            int c = body[n];
            if (c / columns == topRow && goalMark[c] != goals) topWidth++;
        }
        for (int n = 0; n < roomCount; n++) {
            int c = room[n];
            if (c / columns == topRow && mass[c] > goal[c]) topWidth++;
            if (c / columns == bottomRow && mass[c] < goal[c]) bottomWidth++;
        }
        if (surplus >= EASED) {
            moving = Math.min(moving, SURFACE_SPEED * Math.max(1, Math.min(topWidth, bottomWidth)));
        }
        // The share of each row's surplus that goes, from the top row down, and of each row's shortfall that's made
        // up, from the bottom row up
        float left = moving;
        for (int r = 0; r < rows; r++) {
            float taken = Math.min(left, rowSurplus[r]);
            left -= taken;
            rowSurplus[r] = (rowSurplus[r] > 0) ? taken / rowSurplus[r] : 0;
        }
        left = moving;
        for (int r = rows - 1; r >= 0; r--) {
            float given = Math.min(left, rowShortfall[r]);
            left -= given;
            rowShortfall[r] = (rowShortfall[r] > 0) ? given / rowShortfall[r] : 0;
        }
        for (int n = 0; n < size; n++) {
            int c = body[n];
            if (goalMark[c] != goals) {
                setMass(c, mass[c] * (1 - rowSurplus[c / columns]));
            }
        }
        for (int n = 0; n < roomCount; n++) {
            int c = room[n];
            float off = mass[c] - goal[c];
            if (off > 0) {
                setMass(c, mass[c] - off * rowSurplus[c / columns]);
            } else if (off < 0) {
                setMass(c, mass[c] - off * rowShortfall[c / columns]);
            }
        }
        changed = changed || neighbours != null;
        // Its cells are marked seen, so the body isn't levelled twice in one go
        for (int n = 0; n < roomCount; n++) {
            seen[room[n]] = levelling + 1;
        }
    }

    // A cell's water, changed by levelling, which carries on levelling next time
    private void setMass(int c, float amount) {
        if (amount == mass[c]) {
            return;
        }
        if (amount < NOTHING) {
            amount = 0;
        }
        mass[c] = amount;
        next[c] = amount;
        kind[c] = (byte) ((amount > 0) ? WATER : NONE);
        markDirty(c);
    }
}
