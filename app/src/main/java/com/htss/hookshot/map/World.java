package com.htss.hookshot.map;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Build;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.game.object.hook.Chain;
import com.htss.hookshot.game.object.miscellaneous.CompassObject;
import com.htss.hookshot.game.object.miscellaneous.PortalObject;
import com.htss.hookshot.game.object.miscellaneous.TimerObject;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * The caves that exist right now, side by side in one space, and what everything in the game asks of them: whether
 * there's rock at a point, where the caves end, digging, and drawing what's in view.
 *
 * Caves follow one another without a break: each one's exit leads straight into the next one's entrance. The next cave
 * is made in the background as soon as the character enters one, long before it's reached, and joins the world when
 * it's ready. The cave left behind goes once it's out of sight, with everything that was in it. So there are three
 * caves at most: the one the character is in, the one before and the one after. Beyond them it's all rock.
 *
 * The open world is the same space filled another way: pieces of one endless cave all around the character, which
 * OpenWorld sees to.
 *
 * Only the main thread touches the world. The thread that makes a cave only hands it over when it's done.
 */
public class World {

    // As many caves as exist at once: the one the character is in, the one before and the one after
    private static final int MOST_CAVES = 3;
    // How dark the rock beyond the caves is, like the rock deep inside them
    private static final int DEEP_ROCK = Color.argb(170, 0, 0, 0);

    // Ordered by level
    private static final ArrayList<Cave> caves = new ArrayList<Cave>();
    private static Cave current, lastFound;
    // Whether caves follow this one. The playground is a cave on its own
    private static boolean endless = false;
    // Whether this is the open world
    private static boolean open = false;
    private static int left, top, right, bottom;
    // The cave made in the background, waiting to join the world, and the level being made, if any. Starting over
    // changes the run, so a cave that was being made for the last one is thrown away when it's done
    private static volatile Cave made;
    private static int makingLevel = -1, run = 0;
    private static final Paint rockPaint = new Paint(), deepPaint = new Paint();
    // Counts the frames the pools are drawn in, which ripple and flicker with it
    private static int fluidFrame = 0;
    private static CavePalette rockPalette;

    // Starts over with a cave, with everything already in the game taken as being in it
    public static void start(Map map, int level, boolean endless) {
        clear();
        World.endless = endless;
        open = false;
        Cave cave = new Cave(map, level, 0, 0);
        cave.objects.addAll(MyActivity.canvas.gameObjects);
        cave.objects.addAll(MyActivity.dynamicObjects);
        cave.objects.addAll(MyActivity.enemies);
        add(cave);
        current = cave;
        makeNext();
    }

    // Starts the open world over, from its seed, with the piece at the given place there to stand in. Gives that piece
    public static Cave startOpen(long seed, boolean forget, double x, double y) {
        clear();
        endless = false;
        open = true;
        current = OpenWorld.start(seed, forget, x, y);
        MyActivity.currentMap = current.map;
        MyActivity.canvas.myActivity.level = current.level;
        return current;
    }

    public static void clear() {
        lastFound = null;
        run++;
        made = null;
        makingLevel = -1;
        for (Cave cave : caves) {
            cave.dropAll();
        }
        caves.clear();
        current = null;
        findBounds();
    }

    // Once every update, from the main thread, with where the character is and what's in view
    public static void update(double characterX, double characterY, int viewLeft, int viewTop, int viewWidth, int viewHeight) {
        if (current == null) {
            return;
        }
        for (int i = 0; i < caves.size(); i++) {
            caves.get(i).updateFluid();
        }
        if (open) {
            current = OpenWorld.update(characterX, characterY, current);
            return;
        }
        Cave made = World.made;
        if (made != null && !caves.isEmpty() && made.level == caves.get(caves.size() - 1).level + 1) {
            World.made = null;
            makingLevel = -1;
            if (caves.size() >= MOST_CAVES && caves.get(0) != current) {
                remove(caves.get(0));
            }
            if (caves.size() < MOST_CAVES) {
                join(made);
            }
        }
        Cave at = getCaveAt(characterX, characterY);
        if (at != null && at != current) {
            enter(at);
        }
        // The cave left behind goes once it's out of sight, unless the chain is still hooked in it
        Cave oldest = caves.get(0);
        if (oldest != current && oldest.level < current.level && !oldest.shows(viewLeft, viewTop, viewWidth, viewHeight) && !isHookedIn(oldest)) {
            remove(oldest);
        }
    }

    // The cave joins the world, with what's in it, which is kept in the order it's found in the game. That's the same
    // every time the same cave is made, so it tells one of its objects from another
    static void join(Cave cave) {
        HashSet<Object> known = new HashSet<Object>(MyActivity.canvas.gameObjects);
        known.addAll(MyActivity.dynamicObjects);
        known.addAll(MyActivity.enemies);
        cave.map.addObjects(cave.x, cave.y);
        ArrayList<Object> after = new ArrayList<Object>(MyActivity.canvas.gameObjects);
        after.addAll(MyActivity.dynamicObjects);
        after.addAll(MyActivity.enemies);
        for (Object object : after) {
            if (known.add(object)) {
                cave.objects.add(object);
            }
        }
        add(cave);
    }

    // The character crossed into another cave, which is now the level being played
    private static void enter(Cave cave) {
        current = cave;
        MyActivity.currentMap = cave.map;
        MyActivity.canvas.myActivity.level = cave.level;
        MyActivity.canvas.myActivity.save();
        if (MyActivity.character.getCompass() != null) {
            MyActivity.character.getCompass().findInterests();
        }
        makeNext();
    }

    // Makes the cave after the last one, away from the main thread, unless it's made or being made already
    private static void makeNext() {
        if (!endless || caves.isEmpty()) {
            return;
        }
        final Cave last = caves.get(caves.size() - 1);
        if (last != current || makingLevel == last.level + 1 || made != null) {
            return;
        }
        makingLevel = last.level + 1;
        final int thisRun = run;
        Thread maker = new Thread(new Runnable() {
            @Override
            public void run() {
                int[] origin = last.map.getNextOrigin(last.x, last.y);
                Cave cave = new Cave(last.map.next(), last.level + 1, origin[0], origin[1]);
                if (thisRun == run) {
                    made = cave;
                }
            }
        }, "cave maker");
        // Below the game's own thread, but not so low that the cave takes many seconds while the game runs
        maker.setPriority(Thread.NORM_PRIORITY - 2);
        maker.start();
    }

    private static boolean isHookedIn(Cave cave) {
        if (MyActivity.character == null || MyActivity.character.getHook() == null || MyActivity.character.getHook().getHookedPoint() == null) {
            return false;
        }
        return cave.contains(MyActivity.character.getHook().getHookedPoint().x, MyActivity.character.getHook().getHookedPoint().y);
    }

    private static void add(Cave cave) {
        int index = 0;
        while (index < caves.size() && caves.get(index).level < cave.level) {
            index++;
        }
        caves.add(index, cave);
        findBounds();
        cave.joinFluid();
    }

    // The cave goes, with what it came with and what was left in it, like loot. Portals only work in pairs, so if one
    // goes they all do, as they did on leaving a cave
    static void remove(Cave cave) {
        cave.dropAll();
        caves.remove(cave);
        lastFound = null;
        findBounds();
        MyActivity.canvas.gameObjects.removeAll(cave.objects);
        MyActivity.dynamicObjects.removeAll(cave.objects);
        MyActivity.enemies.removeAll(cave.objects);
        boolean portalGone = false;
        for (GameObject object : new ArrayList<GameObject>(MyActivity.canvas.gameObjects)) {
            // Not what the character carries with it, wherever that was made
            boolean carried = object == MyActivity.character || object instanceof Chain || object instanceof CompassObject || object instanceof TimerObject;
            if (!carried && cave.contains(object.getxPosInRoom(), object.getyPosInRoom())) {
                portalGone = portalGone || object instanceof PortalObject;
                MyActivity.canvas.gameObjects.remove(object);
                MyActivity.dynamicObjects.remove(object);
            }
        }
        if (portalGone && MyActivity.character != null) {
            MyActivity.character.losePortals();
        }
        if (MyActivity.character != null && MyActivity.character.getCompass() != null) {
            MyActivity.character.getCompass().findInterests();
        }
    }

    private static void findBounds() {
        left = top = right = bottom = 0;
        for (int i = 0; i < caves.size(); i++) {
            Cave cave = caves.get(i);
            left = (i == 0) ? cave.x : Math.min(left, cave.x);
            top = (i == 0) ? cave.y : Math.min(top, cave.y);
            right = (i == 0) ? cave.x + cave.width : Math.max(right, cave.x + cave.width);
            bottom = (i == 0) ? cave.y + cave.height : Math.max(bottom, cave.y + cave.height);
        }
    }

    // The box around all the caves, which the camera stays inside
    public static int getLeft() {
        return left;
    }

    public static int getTop() {
        return top;
    }

    public static int getRight() {
        return right;
    }

    public static int getBottom() {
        return bottom;
    }

    public static Cave getCurrent() {
        return current;
    }

    // The last one found is tried first, as what asks is mostly asking about points close together
    public static Cave getCaveAt(double x, double y) {
        if (lastFound != null && lastFound.contains(x, y)) {
            return lastFound;
        }
        for (int i = 0; i < caves.size(); i++) {
            if (caves.get(i).contains(x, y)) {
                lastFound = caves.get(i);
                return lastFound;
            }
        }
        return null;
    }

    public static boolean isOpen() {
        return open;
    }

    // Inside any of the caves
    public static boolean contains(double x, double y) {
        return getCaveAt(x, y) != null;
    }

    // Rock at a point. Beyond the caves it's all rock
    public static boolean isSolid(int x, int y) {
        Cave cave = getCaveAt(x, y);
        return cave == null || cave.isSolid(x - cave.x, y - cave.y);
    }

    // Water, lava or neither at a point. Beyond the caves there's none
    public static int getFluidAt(double x, double y) {
        Cave cave = getCaveAt(x, y);
        return (cave == null) ? FluidPool.NONE : cave.getFluid(x - cave.x, y - cave.y);
    }

    // Where the fluid at a point comes up to, or not a number if there's none there. A column of it can carry on up
    // into the cave above
    public static float getFluidSurfaceAt(double x, double y) {
        Cave cave = getCaveAt(x, y);
        if (cave == null) {
            return Float.NaN;
        }
        float surface = cave.getFluidSurface(x - cave.x, y - cave.y) + cave.y;
        while (!Float.isNaN(surface) && surface <= cave.y) {
            Cave above = getCaveAt(x, cave.y - 1);
            if (above == null || getFluidAt(x, cave.y - 1) == FluidPool.NONE) {
                break;
            }
            cave = above;
            surface = cave.getFluidSurface(x - cave.x, cave.y + cave.height - 1 - cave.y) + cave.y;
        }
        return surface;
    }

    // Digs a round hole in whichever caves it reaches
    public static void dig(float centerX, float centerY, float radius) {
        for (int i = 0; i < caves.size(); i++) {
            Cave cave = caves.get(i);
            cave.dig(centerX - cave.x, centerY - cave.y, radius);
        }
    }

    // Draws the part of the world that starts at a point and fills the given size, at the canvas' top left corner
    public static void draw(Canvas canvas, int viewLeft, int viewTop, int width, int height) {
        drawBeyond(canvas, viewLeft, viewTop, width, height);
        // The water and lava go behind the rock, which covers their edges
        fluidFrame++;
        for (int i = 0; i < caves.size(); i++) {
            caves.get(i).drawFluid(canvas, viewLeft, viewTop, width, height, fluidFrame);
        }
        for (int i = 0; i < caves.size(); i++) {
            caves.get(i).draw(canvas, viewLeft, viewTop, width, height);
        }
    }

    // Rock where the view reaches beyond the caves, like past a corner where three of them meet, as that's what's there
    private static void drawBeyond(Canvas canvas, int viewLeft, int viewTop, int width, int height) {
        boolean covered = false;
        for (int i = 0; i < caves.size() && !covered; i++) {
            Cave cave = caves.get(i);
            covered = cave.x <= viewLeft && cave.y <= viewTop && cave.x + cave.width >= viewLeft + width && cave.y + cave.height >= viewTop + height;
        }
        if (covered || current == null) {
            return;
        }
        CavePalette palette = CavePalette.forLevel(current.level);
        if (palette != rockPalette) {
            rockPalette = palette;
            rockPaint.setShader(new BitmapShader(CaveTextures.getRock(palette), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
            deepPaint.setColor(DEEP_ROCK);
        }
        // The texture is laid out from the world's corner, so it runs on from the caves'
        Matrix matrix = new Matrix();
        float texel = (float) Map.SQUARE_SIZE / Map.ROCK_TEXELS_PER_SQUARE;
        matrix.setScale(texel, texel);
        matrix.postTranslate(-viewLeft, -viewTop);
        rockPaint.getShader().setLocalMatrix(matrix);
        canvas.save();
        for (int i = 0; i < caves.size(); i++) {
            Cave cave = caves.get(i);
            int x = cave.x - viewLeft, y = cave.y - viewTop;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutRect(x, y, x + cave.width, y + cave.height);
            } else {
                canvas.clipRect(x, y, x + cave.width, y + cave.height, Region.Op.DIFFERENCE);
            }
        }
        canvas.drawRect(0, 0, width, height, rockPaint);
        canvas.drawRect(0, 0, width, height, deepPaint);
        canvas.restore();
    }
}
