package com.htss.hookshot.map;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;

import java.util.ArrayList;
import java.util.Vector;

/**
 * A cave in the world: its map, where it is, what's solid in it, and its picture.
 *
 * What's solid is kept as a bit for every pixel, which is what everything that touches rock asks about. That's a
 * thirtieth of the picture the whole cave used to be drawn into, so several caves can exist at once. The picture is
 * drawn a tile at a time, as tiles come into view, from the cave's shapes, which are sorted by tile so that drawing
 * one only goes through what's in it. Tiles are kept while they're seen, so the GPU keeps them too, and the ones
 * unseen for longest are dropped. Holes dug into the rock are remembered, as they're not among the cave's shapes, and
 * dug again into any tile drawn afresh.
 *
 * Building one takes a while, and can be done away from the main thread. From then on it belongs to the main thread.
 */
public class Cave {

    public static final int TILE = 256;
    // Tiles kept at most, more than cover the biggest screen and the ring around it, and how many of that ring are
    // drawn each frame before they're seen
    private static final int MAX_KEPT = 160, AHEAD_PER_FRAME = 2;

    public final Map map;
    public final int level;
    // Where its top left corner is in the world, and its size, in pixels
    public final int x, y, width, height;
    private final CavePalette palette;
    private final int columns, rows;
    private final long[] solid;
    private final Path[] rock, edges;
    private final ArrayList<ArrayList<float[]>> digs;
    private final Bitmap shading;
    private final RectF shadingArea;
    private final Paint rockPaint = new Paint(Paint.FILTER_BITMAP_FLAG), shadingPaint = new Paint(Paint.FILTER_BITMAP_FLAG),
            edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG), clearPaint = new Paint(), digEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            tilePaint = new Paint();
    private final Bitmap[] tiles;
    private final int[] lastSeen;
    private int kept = 0, frame = 0;
    // What came into the game with the cave, like its enemies and gates, in the order it was made, which goes when the
    // cave does
    public final ArrayList<Object> objects = new ArrayList<Object>();
    // Every hole dug in it
    private final ArrayList<float[]> allDigs = new ArrayList<float[]>();
    // Its water and lava
    private final CaveFluid fluid;

    public Cave(Map map, int level, int x, int y) {
        this.map = map;
        this.level = level;
        this.x = x;
        this.y = y;
        this.width = map.getWidth();
        this.height = map.getHeight();
        this.palette = CavePalette.forLevel(level);
        columns = (width + TILE - 1) / TILE;
        rows = (height + TILE - 1) / TILE;
        rock = new Path[columns * rows];
        edges = new Path[columns * rows];
        tiles = new Bitmap[columns * rows];
        lastSeen = new int[columns * rows];
        digs = new ArrayList<ArrayList<float[]>>(columns * rows);
        for (int i = 0; i < columns * rows; i++) {
            digs.add(null);
        }
        solid = new long[(int) (((long) width * height + 63) / 64)];

        float edgeWidth = (float) (Map.SQUARE_SIZE / 4);
        sortRock(map.getVertices(), map.getTriangles());
        sortEdges(map.getVertices(), map.getOutlines(), edgeWidth);

        // The rock's texture runs on from cave to cave, as it's laid out from the world's corner rather than the cave's
        BitmapShader texture = new BitmapShader(CaveTextures.getRock(palette), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix scale = new Matrix();
        float texel = (float) Map.SQUARE_SIZE / Map.ROCK_TEXELS_PER_SQUARE;
        scale.setScale(texel, texel);
        scale.postTranslate(-x, -y);
        texture.setLocalMatrix(scale);
        rockPaint.setShader(texture);
        // Only on the rock already drawn, and keeping it opaque, as what's solid is what's opaque
        shadingPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        shading = CaveShading.create(map.getMap(), palette);
        float half = (float) Map.SQUARE_SIZE / 2;
        shadingArea = new RectF(-half, -half, shading.getWidth() * (float) Map.SQUARE_SIZE - half, shading.getHeight() * (float) Map.SQUARE_SIZE - half);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(edgeWidth);
        edgePaint.setStrokeJoin(Paint.Join.ROUND);
        edgePaint.setStrokeCap(Paint.Cap.ROUND);
        edgePaint.setColor(palette.outline);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        // Dug tunnels get the same dark edge as the rest of the cave, only over rock, keeping it opaque
        digEdgePaint.setStyle(Paint.Style.STROKE);
        digEdgePaint.setStrokeWidth(edgeWidth);
        digEdgePaint.setColor(palette.outline);
        digEdgePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        findSolid();
        fluid = new CaveFluid(this, map);
    }

    // Every triangle of rock goes to the tiles it touches. Each tile's are one path, as filling them apart leaves
    // thin seams between them
    private void sortRock(Vector<Point> vertices, Vector<Integer> triangles) {
        for (int i = 0; i + 2 < triangles.size(); i += 3) {
            Point a = vertices.get(triangles.get(i)), b = vertices.get(triangles.get(i + 1)), c = vertices.get(triangles.get(i + 2));
            int fromColumn = clampColumn(Math.min(a.x, Math.min(b.x, c.x)) - 1), toColumn = clampColumn(Math.max(a.x, Math.max(b.x, c.x)) + 1);
            int fromRow = clampRow(Math.min(a.y, Math.min(b.y, c.y)) - 1), toRow = clampRow(Math.max(a.y, Math.max(b.y, c.y)) + 1);
            for (int column = fromColumn; column <= toColumn; column++) {
                for (int row = fromRow; row <= toRow; row++) {
                    int tile = row * columns + column;
                    if (rock[tile] == null) {
                        rock[tile] = new Path();
                    }
                    rock[tile].moveTo(a.x, a.y);
                    rock[tile].lineTo(b.x, b.y);
                    rock[tile].lineTo(c.x, c.y);
                    rock[tile].close();
                }
            }
        }
    }

    // The dark line along the rock's edges, a stretch at a time, to the tiles each stretch touches. Not along the
    // cave's own borders, which are where the next cave carries on. It does go right up to them, where the cave's walls
    // cross them, so it meets the line the next cave draws on its side
    private void sortEdges(Vector<Point> vertices, Vector<Vector<Integer>> outlines, float edgeWidth) {
        int margin = (int) Math.ceil(edgeWidth / 2) + 1;
        // The last stretch each tile got, so one that follows it carries the line on, with a proper joint, rather than
        // starting a new one. Two strokes that only meet blend a touch less than solid where they do
        int[] lastStretch = new int[columns * rows];
        int stretch = 1;
        for (Vector<Integer> outline : outlines) {
            Point previous = null;
            for (Integer index : outline) {
                Point point = vertices.get(index);
                stretch++;
                // A jump is where the outline doesn't carry on, so the line starts again from the new point
                if (previous != null && !isAlongBorder(previous, point) && Math.hypot(point.x - previous.x, point.y - previous.y) <= Map.OUTLINE_JUMP) {
                    int fromColumn = clampColumn(Math.min(previous.x, point.x) - margin), toColumn = clampColumn(Math.max(previous.x, point.x) + margin);
                    int fromRow = clampRow(Math.min(previous.y, point.y) - margin), toRow = clampRow(Math.max(previous.y, point.y) + margin);
                    for (int column = fromColumn; column <= toColumn; column++) {
                        for (int row = fromRow; row <= toRow; row++) {
                            int tile = row * columns + column;
                            if (edges[tile] == null) {
                                edges[tile] = new Path();
                            }
                            if (lastStretch[tile] != stretch - 1) {
                                edges[tile].moveTo(previous.x, previous.y);
                            }
                            edges[tile].lineTo(point.x, point.y);
                            lastStretch[tile] = stretch;
                        }
                    }
                }
                previous = point;
            }
        }
    }

    // Whether a stretch runs along one of the cave's borders, rather than across or away from it
    private boolean isAlongBorder(Point a, Point b) {
        return (a.x == 0 && b.x == 0) || (a.x == width && b.x == width) || (a.y == 0 && b.y == 0) || (a.y == height && b.y == height);
    }

    private int clampColumn(int pixel) {
        return Math.max(0, Math.min(columns - 1, pixel / TILE));
    }

    private int clampRow(int pixel) {
        return Math.max(0, Math.min(rows - 1, pixel / TILE));
    }

    // What's solid is what's fully opaque in the picture: the rock and the line along its edges. So every tile is drawn
    // once, plainly, which leaves the same pixels opaque as the picture will
    private void findSolid() {
        Bitmap tile = Bitmap.createBitmap(TILE, TILE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tile);
        int[] pixels = new int[TILE * TILE];
        Paint plain = new Paint();
        plain.setColor(0xFF000000);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (rock[index] == null && edges[index] == null) {
                    continue;
                }
                tile.eraseColor(0);
                canvas.save();
                canvas.translate(-column * TILE, -row * TILE);
                if (rock[index] != null) {
                    canvas.drawPath(rock[index], plain);
                }
                if (edges[index] != null) {
                    canvas.drawPath(edges[index], edgePaint);
                }
                canvas.restore();
                tile.getPixels(pixels, 0, TILE, 0, 0, TILE, TILE);
                int tileWidth = Math.min(TILE, width - column * TILE), tileHeight = Math.min(TILE, height - row * TILE);
                for (int ty = 0; ty < tileHeight; ty++) {
                    int bit = (row * TILE + ty) * width + column * TILE;
                    for (int tx = 0; tx < tileWidth; tx++, bit++) {
                        if ((pixels[ty * TILE + tx] >>> 24) == 255) {
                            solid[bit >> 6] |= 1L << (bit & 63);
                        }
                    }
                }
            }
        }
        tile.recycle();
    }

    // Whether any of it is in a part of the world
    public boolean shows(int viewLeft, int viewTop, int viewWidth, int viewHeight) {
        return viewLeft < x + width && viewLeft + viewWidth > x && viewTop < y + height && viewTop + viewHeight > y;
    }

    public boolean contains(double worldX, double worldY) {
        return worldX >= x && worldX < x + width && worldY >= y && worldY < y + height;
    }

    // Water, lava or neither at a point of the cave, counted from its own corner
    public int getFluid(double localX, double localY) {
        return fluid.getFluid(localX, localY);
    }

    // Where the fluid at a point comes up to, counted from the cave's corner, or not a number if there's none there
    public float getFluidSurface(double localX, double localY) {
        return fluid.getSurface(localX, localY);
    }

    CaveFluid getFluidGrid() {
        return fluid;
    }

    // It's in the world now, so its fluid and the fluid of the caves next to it can flow into each other
    void joinFluid() {
        fluid.join();
    }

    // The fluid flows on a step, where it's moving
    public void updateFluid() {
        fluid.step();
    }

    // Draws the fluid that shows in a part of the world, which starts at the canvas' top left corner. Before the rock,
    // which covers its edges
    public void drawFluid(Canvas canvas, int left, int top, int viewWidth, int viewHeight, int frame) {
        if (shows(left, top, viewWidth, viewHeight)) {
            fluid.draw(canvas, left - x, top - y, viewWidth, viewHeight, frame);
        }
    }

    // Whether there's rock at a point of the cave, counted from its own corner
    public boolean isSolid(int localX, int localY) {
        int bit = localY * width + localX;
        return (solid[bit >> 6] >>> (bit & 63) & 1) != 0;
    }

    // Digs a round hole, in the rock and in the picture, given from the cave's own corner
    public void dig(float centerX, float centerY, float radius) {
        int left = Math.max(0, (int) Math.floor(centerX - radius)), right = Math.min(width - 1, (int) Math.ceil(centerX + radius));
        int top = Math.max(0, (int) Math.floor(centerY - radius)), bottom = Math.min(height - 1, (int) Math.ceil(centerY + radius));
        if (left > right || top > bottom) {
            return;
        }
        boolean dug = false;
        for (int py = top; py <= bottom; py++) {
            for (int px = left; px <= right; px++) {
                double dx = px + 0.5 - centerX, dy = py + 0.5 - centerY;
                if (dx * dx + dy * dy <= radius * radius) {
                    int bit = py * width + px;
                    dug |= (solid[bit >> 6] >>> (bit & 63) & 1) != 0;
                    solid[bit >> 6] &= ~(1L << (bit & 63));
                }
            }
        }
        // A hole where there's no rock left changes nothing. Worms keep digging in their own tunnels, and keeping those
        // would only make the cave slower to bring back and its memory bigger
        if (!dug) {
            return;
        }
        // Fluid around the hole can flow into it now
        if (fluid != null) {
            fluid.dug(centerX, centerY, radius);
        }
        float[] dig = {centerX, centerY, radius};
        allDigs.add(dig);
        float reach = radius + digEdgePaint.getStrokeWidth();
        for (int column = clampColumn((int) (centerX - reach)); column <= clampColumn((int) (centerX + reach)); column++) {
            for (int row = clampRow((int) (centerY - reach)); row <= clampRow((int) (centerY + reach)); row++) {
                int index = row * columns + column;
                if (digs.get(index) == null) {
                    digs.set(index, new ArrayList<float[]>());
                }
                digs.get(index).add(dig);
                // Into the tile's picture too, if it's drawn. If it isn't, it's dug when it is
                if (tiles[index] != null) {
                    Canvas canvas = new Canvas(tiles[index]);
                    canvas.translate(-column * TILE, -row * TILE);
                    drawDig(canvas, dig);
                    tiles[index].prepareToDraw();
                }
            }
        }
    }

    public ArrayList<float[]> getDigs() {
        return allDigs;
    }

    private void drawDig(Canvas canvas, float[] dig) {
        canvas.drawCircle(dig[0], dig[1], dig[2], clearPaint);
        canvas.drawCircle(dig[0], dig[1], dig[2], digEdgePaint);
    }

    // Draws the part of the cave that shows in a part of the world, which starts at the canvas' top left corner
    public void draw(Canvas canvas, int left, int top, int viewWidth, int viewHeight) {
        frame++;
        int localLeft = left - x, localTop = top - y;
        if (localLeft >= width || localTop >= height || localLeft + viewWidth <= 0 || localTop + viewHeight <= 0) {
            // Out of sight, so its picture isn't needed for now
            if (kept > 0) {
                dropAll();
            }
            return;
        }
        int firstColumn = clampColumn(localLeft), lastColumn = clampColumn(localLeft + viewWidth - 1);
        int firstRow = clampRow(localTop), lastRow = clampRow(localTop + viewHeight - 1);
        for (int column = firstColumn; column <= lastColumn; column++) {
            for (int row = firstRow; row <= lastRow; row++) {
                int index = row * columns + column;
                if (tiles[index] == null) {
                    cut(column, row);
                }
                lastSeen[index] = frame;
                canvas.drawBitmap(tiles[index], column * TILE - localLeft, row * TILE - localTop, tilePaint);
            }
        }
        // A few of the tiles around the screen, so moving the camera rarely has to draw one right as it's seen
        int ahead = 0;
        for (int column = Math.max(0, firstColumn - 1); column <= Math.min(columns - 1, lastColumn + 1) && ahead < AHEAD_PER_FRAME; column++) {
            for (int row = Math.max(0, firstRow - 1); row <= Math.min(rows - 1, lastRow + 1) && ahead < AHEAD_PER_FRAME; row++) {
                if (tiles[row * columns + column] == null) {
                    cut(column, row);
                    lastSeen[row * columns + column] = frame;
                    ahead++;
                }
            }
        }
        if (kept > MAX_KEPT) {
            dropUnseen();
        }
    }

    // Draws a tile's picture: the rock, lit by the caves and dark deep in, the line along its edges, and the holes dug
    private void cut(int column, int row) {
        int index = row * columns + column;
        Bitmap tile = Bitmap.createBitmap(TILE, TILE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tile);
        canvas.translate(-column * TILE, -row * TILE);
        if (rock[index] != null) {
            canvas.drawPath(rock[index], rockPaint);
            canvas.drawBitmap(shading, null, shadingArea, shadingPaint);
        }
        if (edges[index] != null) {
            canvas.drawPath(edges[index], edgePaint);
        }
        if (digs.get(index) != null) {
            for (float[] dig : digs.get(index)) {
                drawDig(canvas, dig);
            }
        }
        // Starts sending it to the GPU now, rather than when it's first drawn
        tile.prepareToDraw();
        tiles[index] = tile;
        kept++;
    }

    private void drop(int index) {
        if (tiles[index] == null) {
            return;
        }
        // Freed now, with its copy on the GPU, rather than whenever it's collected. Before Android 8 a bitmap freed
        // while the last frame still draws it breaks that frame, so there it's left to be collected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tiles[index].recycle();
        }
        tiles[index] = null;
        kept--;
    }

    // Drops the tiles unseen for longest, down to three quarters of the most kept, so this doesn't run every frame
    private void dropUnseen() {
        while (kept > MAX_KEPT * 3 / 4) {
            int oldest = -1;
            for (int index = 0; index < tiles.length; index++) {
                if (tiles[index] != null && lastSeen[index] < frame && (oldest < 0 || lastSeen[index] < lastSeen[oldest])) {
                    oldest = index;
                }
            }
            if (oldest < 0) {
                return;
            }
            drop(oldest);
        }
    }

    // Gives up its picture, when it's out of sight or leaves the world
    public void dropAll() {
        for (int index = 0; index < tiles.length; index++) {
            drop(index);
        }
    }
}
