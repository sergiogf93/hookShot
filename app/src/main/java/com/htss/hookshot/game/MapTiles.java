package com.htss.hookshot.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;

/**
 * Draws the cave from tiles cut out of the map as they come into view. They're kept between frames, so the GPU keeps
 * them too and each frame only draws them, instead of copying the visible part of the map into a screen sized bitmap
 * that has to be uploaded again every frame. Tiles around the screen are cut a few at a time ahead of being seen, and
 * the ones unseen for longest are dropped once there are too many. Where the map changes, like rock dug away, the
 * tiles are updated in place, so the GPU replaces its copy instead of keeping one for every change.
 */
public class MapTiles {

    private static final int SIZE = 256;
    // Tiles kept at most, more than cover the biggest screen and the ring around it
    private static final int MAX_KEPT = 200;
    // How many tiles around the screen are cut each frame before they're seen
    private static final int AHEAD_PER_FRAME = 2;

    private final Paint paint = new Paint(), copyPaint = new Paint();
    private final Rect from = new Rect(), to = new Rect();
    private Bitmap map;
    private Bitmap[][] tiles;
    private int[][] lastSeen;
    private int columns, rows, kept = 0, frame = 0;

    public MapTiles() {
        // Replaces the tile's pixels, so dug rock turns transparent
        copyPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
    }

    public void setMap(Bitmap map) {
        if (tiles != null) {
            for (int column = 0; column < columns; column++) {
                for (int row = 0; row < rows; row++) {
                    drop(column, row);
                }
            }
        }
        this.map = map;
        columns = (map.getWidth() + SIZE - 1) / SIZE;
        rows = (map.getHeight() + SIZE - 1) / SIZE;
        tiles = new Bitmap[columns][rows];
        lastSeen = new int[columns][rows];
        kept = 0;
    }

    // Draws the part of the map that starts at the given point and fills the given size, at the canvas' top left
    public void draw(Canvas canvas, int left, int top, int width, int height) {
        if (map == null) {
            return;
        }
        frame++;
        int firstColumn = Math.max(0, left / SIZE), lastColumn = Math.min(columns - 1, (left + width - 1) / SIZE);
        int firstRow = Math.max(0, top / SIZE), lastRow = Math.min(rows - 1, (top + height - 1) / SIZE);
        for (int column = firstColumn; column <= lastColumn; column++) {
            for (int row = firstRow; row <= lastRow; row++) {
                if (tiles[column][row] == null) {
                    cut(column, row);
                }
                lastSeen[column][row] = frame;
                canvas.drawBitmap(tiles[column][row], column * SIZE - left, row * SIZE - top, paint);
            }
        }
        cutAhead(firstColumn - 1, lastColumn + 1, firstRow - 1, lastRow + 1);
        if (kept > MAX_KEPT) {
            dropUnseen();
        }
    }

    // Cuts a few of the missing tiles around the screen, so moving the camera rarely has to cut one right as it's seen
    private void cutAhead(int firstColumn, int lastColumn, int firstRow, int lastRow) {
        int cut = 0;
        for (int column = Math.max(0, firstColumn); column <= Math.min(columns - 1, lastColumn) && cut < AHEAD_PER_FRAME; column++) {
            for (int row = Math.max(0, firstRow); row <= Math.min(rows - 1, lastRow) && cut < AHEAD_PER_FRAME; row++) {
                if (tiles[column][row] == null) {
                    cut(column, row);
                    lastSeen[column][row] = frame;
                    cut++;
                }
            }
        }
    }

    private void cut(int column, int row) {
        int x = column * SIZE, y = row * SIZE;
        Bitmap tile = Bitmap.createBitmap(Math.min(SIZE, map.getWidth() - x), Math.min(SIZE, map.getHeight() - y), Bitmap.Config.ARGB_8888);
        tiles[column][row] = tile;
        kept++;
        copy(column, row, x, y, x + tile.getWidth(), y + tile.getHeight());
    }

    // Copies a part of the map, in map coordinates, into the tile over it
    private void copy(int column, int row, int left, int top, int right, int bottom) {
        int x = column * SIZE, y = row * SIZE;
        from.set(left, top, right, bottom);
        to.set(left - x, top - y, right - x, bottom - y);
        new Canvas(tiles[column][row]).drawBitmap(map, from, to, copyPaint);
        // Starts sending it to the GPU now, rather than when it's first drawn
        tiles[column][row].prepareToDraw();
    }

    private void drop(int column, int row) {
        if (tiles[column][row] == null) {
            return;
        }
        // Freed now, with its copy on the GPU, rather than whenever it's collected. Before Android 8 a bitmap freed
        // while the last frame still draws it breaks that frame, so there it's left to be collected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tiles[column][row].recycle();
        }
        tiles[column][row] = null;
        kept--;
    }

    // Drops the tiles unseen for longest, down to three quarters of the most kept, so this doesn't run every frame
    private void dropUnseen() {
        while (kept > MAX_KEPT * 3 / 4) {
            int oldestColumn = -1, oldestRow = -1;
            for (int column = 0; column < columns; column++) {
                for (int row = 0; row < rows; row++) {
                    if (tiles[column][row] != null && lastSeen[column][row] < frame
                            && (oldestColumn < 0 || lastSeen[column][row] < lastSeen[oldestColumn][oldestRow])) {
                        oldestColumn = column;
                        oldestRow = row;
                    }
                }
            }
            if (oldestColumn < 0) {
                return;
            }
            drop(oldestColumn, oldestRow);
        }
    }

    // The map changed in a circle, like rock dug away, so the tiles over it copy that part again. Tiles not cut yet
    // are cut from the changed map when they're needed
    public void changed(float x, float y, float radius) {
        if (map == null) {
            return;
        }
        int left = Math.max(0, (int) Math.floor(x - radius)), right = Math.min(map.getWidth(), (int) Math.ceil(x + radius) + 1);
        int top = Math.max(0, (int) Math.floor(y - radius)), bottom = Math.min(map.getHeight(), (int) Math.ceil(y + radius) + 1);
        if (left >= right || top >= bottom) {
            return;
        }
        for (int column = left / SIZE; column <= (right - 1) / SIZE; column++) {
            for (int row = top / SIZE; row <= (bottom - 1) / SIZE; row++) {
                if (tiles[column][row] != null) {
                    int x0 = column * SIZE, y0 = row * SIZE;
                    copy(column, row, Math.max(left, x0), Math.max(top, y0), Math.min(right, x0 + SIZE), Math.min(bottom, y0 + SIZE));
                }
            }
        }
    }
}
