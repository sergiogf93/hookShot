package com.htss.hookshot.map;


/**
 * Created by Sergio on 25/08/2016.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.enemies.EnemyBat;
import com.htss.hookshot.game.object.enemies.EnemyBeetle;
import com.htss.hookshot.game.object.enemies.EnemyDeepWorm;
import com.htss.hookshot.game.object.enemies.EnemySnipper;
import com.htss.hookshot.game.object.enemies.EnemySpitter;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.enemies.EnemyStalker;
import com.htss.hookshot.game.object.enemies.EnemyTerraWorm;
import com.htss.hookshot.game.object.interactables.CoinBag;
import com.htss.hookshot.game.object.interactables.HealthDrop;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;
import com.htss.hookshot.game.object.obstacles.Ball;
import com.htss.hookshot.game.object.obstacles.Door;
import com.htss.hookshot.game.object.obstacles.WallButton;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

public class Map {

    private static final int MAX_BUTTONS = 7, MIN_BUTTONS = 2;
    // Every tenth level is the deep worm's, in a cavern this many tiles wide and tall from its middle. Switched off for
    // now, while the controls are being tried out: the worm can still be placed from the playground's menu
    private static final boolean BOSS_LEVELS = false;
    private static final int BOSS_EVERY = 10, BOSS_CAVERN_X = 22, BOSS_CAVERN_Y = 13;
    // The levels new enemies start appearing at, how many groups of enemies a level can have, and how many tiles away
    // from the entrance new kinds of enemy are placed
    private static final int BAT_LEVEL = 3, SPITTER_LEVEL = 6, SNIPPER_LEVEL = 8, BEETLE_LEVEL = 12, MAX_ENEMY_GROUPS = 3,
            SAFE_FROM_ENTRANCE = 18;
    private static final int SMOOTH_ITERATIONS = 2,  //5, 4, 3, 5, 5, 3
            WALL_COUNT_SMOOTH_THRESHOLD = 4,
            BORDER_SIZE = 3,
            WALL_COUNT_SIZE_THRESHOLD = 2,
            ROOM_COUNT_SIZE_THRESHOLD = 2,
            PASSAGE_RADIUS = 3;
    public static final double SQUARE_SIZE = 45 * MyActivity.TILE_WIDTH / 100;
    private static final int MAX_POWERUPS = 3;
    private static final int MAX_ENEMIES = 2;
    private static final int MAX_HEALTH = 2;
    // How the rock texture is drawn: its pixels per cave square, and how strong its lit edges and deep shadows are
    public static final int ROCK_TEXELS_PER_SQUARE = 24;
    private static final int RIM_ALPHA = 80, SHADOW_ALPHA = 170;
    // Two points of an outline further apart than this aren't joined: a border was skipped between them
    public static final double OUTLINE_JUMP = MyActivity.TILE_WIDTH;
    // Large odd number (the golden ratio in 64 bits), so consecutive levels get very different seeds
    private static final long LEVEL_SEED_SPREAD = 0x9E3779B97F4A7C15L;

    private int[][] map;
    private int xTiles, yTiles, fillPercent, maxSizeForSusceptible;
    // Which level this cave is, and the seed of the game it's in, as several caves exist at once: the one being
    // played, and the next, which is made while the last one still is
    private int level;
    private long seed;
    // Where a piece's vault was hollowed out and where along its corridor the door that bars it goes, both in tiles,
    // as the piece doesn't know where it is in the world until its objects are placed. Null if the piece has none
    private Coord vaultCenter, vaultDoorTile;
    private MathVector vaultDoorVector;
    // A piece of the open world, and where among the others
    private boolean chunk = false;
    private int chunkX, chunkY;
    private long worldSeed;
    // The size of the open world's pieces in tiles, and how much of them starts out as rock
    public static final int CHUNK_X = 48, CHUNK_Y = 32;
    private static final int CHUNK_FILL = 50;
    // How many of the worms that would come with the pieces are left out
    private static final double CHUNK_WORMS_DROPPED = 0.6;
    // Vaults: how often a piece has one, how big the chamber and its corridor are in tiles, how far out that corridor
    // will look for the cave, how far from the piece's own sides the whole thing stays, how many tries it gets at
    // finding rock to sit in, how many buttons open it and how many powers are kept inside
    private static final double CHUNK_VAULTS = 0.6;
    private static final int VAULT_RADIUS = 4, VAULT_CORRIDOR = 2, VAULT_REACH = 8, VAULT_EDGE = BORDER_SIZE + 1,
            VAULT_TRIES = 300, VAULT_WAYS = 16, VAULT_BUTTONS = 2, VAULT_POWERS = 2;
    // Seals: how many rows apart the barred rows are, and how many buttons break one
    private static final int SEAL_EVERY = 10, SEAL_BUTTONS = 3;
    // Where the cave's corner is in the world, which everything placed in it is placed from
    private double originX = 0, originY = 0;
    private Coord entrance, exit;
    private SquareGrid squareGrid;
    private Vector<Point> vertices;
    private Vector<Integer> triangles;
    private HashMap<Integer,Vector<Triangle>> triangleDictionary = new HashMap<Integer, Vector<Triangle>>();
    private Vector<Vector<Integer>> outlines = new Vector<Vector<Integer>>();
    private HashSet<Integer> checkedVertices = new HashSet<Integer>();
    private Room entranceRoom, exitRoom;
    private Vector<Point[]> cracks = new Vector<Point[]>();
    private Vector<Room> roomRegions = new Vector<Room>();
    private Vector<Room> susceptibleRooms = new Vector<Room>();
    private Vector<Room> roomsWithInterest = new Vector<Room>();

    private Vector<Passage> passages = new Vector<Passage>();
    // Where the character starts in the playground, which has no entrance to start from
    private MathVector playgroundStart = null;

    // The kinds of enemy the playground can place
    public static final int SPAWN_STALKER = 0, SPAWN_TERRA_WORM = 1, SPAWN_BAT = 2, SPAWN_SPITTER = 3, SPAWN_SNIPPER = 4,
            SPAWN_BEETLE = 5, SPAWN_DEEP_WORM = 6;
    // The playground's size in tiles, and how far from the character, in tiles, enemies are placed there
    private static final int PLAYGROUND_X = 100, PLAYGROUND_Y = 56, SPAWN_NEAREST = 6, SPAWN_FURTHEST = 14;

    // The cave a game starts or carries on in, at the world's corner, with everything in it
    public Map (int xTiles, int yTiles, int fillPercent, Coord entrance){
        this(xTiles, yTiles, fillPercent, entrance, MyActivity.canvas.myActivity.level, MyActivity.canvas.myActivity.seed, null);
        addObjects(0, 0);
    }

    // Only the cave itself. What's in it is added with addObjects, from the main thread, as that puts objects in the
    // game. The line to copy is the last cave's edge by the exit, which this one's entrance carries on from
    private Map(int xTiles, int yTiles, int fillPercent, Coord entrance, int level, long seed, int[] lineToCopy) {
        this.map = new int[xTiles][yTiles];
        this.xTiles = xTiles;
        this.yTiles = yTiles;
        this.fillPercent = fillPercent;
        this.maxSizeForSusceptible = (int) ((xTiles*yTiles*(100-fillPercent)/100) * 0.005);
        this.level = level;
        this.seed = seed;
        this.entrance = entrance;

        createMap();
        if (lineToCopy != null) {
            copyLine(lineToCopy, 3);
        }
        generateMesh();
    }

    // A piece of the open world: an endless cave made of pieces like this one, side by side and one below the other,
    // from the surface down. Each is made from the world's seed and its place alone, so it's the same whenever it's
    // made and in whatever order. What makes neighbours fit is that the way through each side is placed by that side
    // itself, which both of them share, and the rock along it is the same from either of them. Everything inside is
    // joined to those ways through, so the whole world can be reached. Deeper pieces count as higher levels, with more
    // and nastier enemies and other rock. It touches nothing but itself, so it can be made away from the main thread
    public static Map chunk(int chunkX, int chunkY, long worldSeed) {
        return new Map(chunkX, chunkY, worldSeed);
    }

    private Map(int chunkX, int chunkY, long worldSeed) {
        this.xTiles = CHUNK_X;
        this.yTiles = CHUNK_Y;
        this.map = new int[xTiles][yTiles];
        this.fillPercent = CHUNK_FILL;
        this.chunk = true;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.worldSeed = worldSeed;
        this.level = Math.max(0, chunkY);
        this.seed = mix(worldSeed, chunkX, chunkY, 7);

        Random random = new Random(seed);
        randomFillMap(fillPercent, random);
        for (int i = 0; i < SMOOTH_ITERATIONS; i++) {
            smoothMap();
        }
        // The ways through: down and to the right are this piece's own sides, up and to the left are its neighbours'.
        // There's none up from the top pieces, which are under the surface
        int up = along(worldSeed, chunkX, chunkY - 1, 0, xTiles), down = along(worldSeed, chunkX, chunkY, 0, xTiles);
        int toLeft = along(worldSeed, chunkX - 1, chunkY, 1, yTiles), toRight = along(worldSeed, chunkX, chunkY, 1, yTiles);
        int radius = (int) (PASSAGE_RADIUS * 1.5);
        if (chunkY > 0) {
            drawCircle(new Coord(up, 0), radius);
        }
        drawCircle(new Coord(down, yTiles - 1), radius);
        drawCircle(new Coord(0, toLeft), radius);
        drawCircle(new Coord(xTiles - 1, toRight), radius);
        // Nothing here leads anywhere in particular, but the rooms are sorted out with a way in and a way out in mind
        this.entrance = new Coord(0, toLeft);
        this.exit = new Coord(down, yTiles - 1);
        manageRooms();
        manageRoomConnection();
        // Rock all along the sides but for the ways through, the same seen from either side
        for (int i = 0; i < BORDER_SIZE; i++) {
            for (int x = 0; x < xTiles; x++) {
                map[x][i] = (chunkY > 0 && Math.abs(x - up) <= radius) ? 0 : 1;
                map[x][yTiles - 1 - i] = (Math.abs(x - down) <= radius) ? 0 : 1;
            }
        }
        for (int i = 0; i < BORDER_SIZE; i++) {
            for (int y = BORDER_SIZE; y < yTiles - BORDER_SIZE; y++) {
                map[i][y] = (Math.abs(y - toLeft) <= radius) ? 0 : 1;
                map[xTiles - 1 - i][y] = (Math.abs(y - toRight) <= radius) ? 0 : 1;
            }
        }
        if (!(chunkX == 0 && chunkY == 0) && new Random(seed * 11 + 5).nextDouble() < CHUNK_VAULTS) {
            carveVault(new Random(seed * 7 + 3));
        }
        generateMesh();
    }

    // A chamber hollowed out of solid rock with one short corridor to the cave, for a door to bar. It's carved with
    // the rest of the piece, before the mesh is made, so it's rock like any other and the same every time
    private void carveVault(Random random) {
        for (int tries = 0; tries < VAULT_TRIES; tries++) {
            int centerX = VAULT_EDGE + VAULT_RADIUS + random.nextInt(xTiles - 2 * (VAULT_EDGE + VAULT_RADIUS));
            int centerY = VAULT_EDGE + VAULT_RADIUS + random.nextInt(yTiles - 2 * (VAULT_EDGE + VAULT_RADIUS));
            if (!isRockAround(centerX, centerY, VAULT_RADIUS + 1)) {
                continue;
            }
            double[] way = findWayOut(centerX, centerY, random);
            if (way == null) {
                continue;
            }
            double angle = way[0];
            int distance = (int) way[1];
            drawCircle(new Coord(centerX, centerY), VAULT_RADIUS);
            for (int d = VAULT_RADIUS - 1; d <= distance; d++) {
                drawCircle(alongWay(centerX, centerY, angle, d), VAULT_CORRIDOR);
            }
            vaultCenter = new Coord(centerX, centerY);
            vaultDoorTile = alongWay(centerX, centerY, angle, (VAULT_RADIUS + distance) / 2);
            vaultDoorVector = new MathVector(Math.cos(angle), Math.sin(angle)).getNormal();
            return;
        }
    }

    // The shortest way out of the chamber that reaches the cave without leaving the piece's own ground, as the
    // direction to dig and how far along it the cave starts. The rock the corridor is dug through has to be thick
    // enough all the way to the door, or digging it would open a second way in past the door
    private double[] findWayOut(int centerX, int centerY, Random random) {
        double bestAngle = 0, offset = random.nextDouble() * Math.PI * 2;
        int bestDistance = 0;
        for (int i = 0; i < VAULT_WAYS; i++) {
            double angle = offset + i * 2 * Math.PI / VAULT_WAYS;
            for (int d = VAULT_RADIUS + 3; d <= VAULT_RADIUS + VAULT_REACH; d++) {
                Coord point = alongWay(centerX, centerY, angle, d);
                if (!isWellInside(point.tileX, point.tileY, VAULT_EDGE + VAULT_CORRIDOR)) {
                    break;
                }
                if (map[point.tileX][point.tileY] == 0) {
                    if ((bestDistance == 0 || d < bestDistance) && isRockToTheDoor(centerX, centerY, angle, d)) {
                        bestDistance = d;
                        bestAngle = angle;
                    }
                    break;
                }
            }
        }
        return (bestDistance == 0) ? null : new double[]{bestAngle, bestDistance};
    }

    // Rock round the corridor, from where it leaves the chamber to where the door will stand
    private boolean isRockToTheDoor(int centerX, int centerY, double angle, int distance) {
        for (int d = VAULT_RADIUS + 1; d <= (VAULT_RADIUS + distance) / 2; d++) {
            Coord point = alongWay(centerX, centerY, angle, d);
            if (!isRockAround(point.tileX, point.tileY, VAULT_CORRIDOR + 1)) {
                return false;
            }
        }
        return true;
    }

    private Coord alongWay(int centerX, int centerY, double angle, int distance) {
        return new Coord(centerX + (int) Math.round(Math.cos(angle) * distance), centerY + (int) Math.round(Math.sin(angle) * distance));
    }

    private boolean isRockAround(int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY) > radius * radius) {
                    continue;
                }
                if (!isInMapRange(x, y) || map[x][y] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isWellInside(int tileX, int tileY, int margin) {
        return tileX >= margin && tileY >= margin && tileX < xTiles - margin && tileY < yTiles - margin;
    }

    // How far along a side the way through it is, in tiles, clear of the corners. The side is the bottom (0) or the
    // right (1) of the piece at the given place, so that the piece on its other side finds the same
    private static int along(long worldSeed, int chunkX, int chunkY, int side, int length) {
        int margin = 9;
        return margin + (int) ((mix(worldSeed, chunkX, chunkY, side) >>> 1) % (length - 2 * margin));
    }

    // Scrambles the numbers into one, so that neighbouring places give unrelated results
    private static long mix(long seed, int a, int b, int c) {
        long h = seed + 0x9E3779B97F4A7C15L * (a * 73856093L ^ b * 19349663L ^ c * 83492791L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    public boolean isChunk() {
        return chunk;
    }

    // Where the way through the bottom (0) or the right side (1) of a piece of the open world is, in tiles along it
    public int getWayThrough(int side) {
        return along(worldSeed, chunkX, chunkY, side, (side == 0) ? xTiles : yTiles);
    }

    // The size of a piece of the open world, in pixels
    public static int getChunkWidth() {
        return (int) (CHUNK_X * SQUARE_SIZE - SQUARE_SIZE);
    }

    public static int getChunkHeight() {
        return (int) (CHUNK_Y * SQUARE_SIZE - SQUARE_SIZE);
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkY() {
        return chunkY;
    }

    // Somewhere to stand, in the world, for a cave with its corner at the given place: open, with nothing close around
    // it, and rock under it
    public MathVector getStandingPoint(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
        for (int distance = 0; distance < Math.max(xTiles, yTiles); distance++) {
            for (int x = xTiles / 2 - distance; x <= xTiles / 2 + distance; x++) {
                for (int y = yTiles / 2 - distance; y <= yTiles / 2 + distance; y++) {
                    if (isInMapRange(x, y) && isInMapRange(x, y + 2) && map[x][y] == 0 && getSurroundingCount(x, y) == 0
                            && map[x][y + 2] == 1 && !isBehindVaultDoor(x, y)) {
                        return at(x, y);
                    }
                }
            }
        }
        return at(xTiles / 2, yTiles / 2);
    }

    // The cave after this one, which its exit leads into. It touches nothing but itself, so it can be made away from
    // the main thread
    public Map next() {
        return new Map(xTiles, yTiles, fillPercent, getEntranceFromExit(exit), level + 1, seed, getLineToCopy());
    }

    public int getLevel() {
        return level;
    }

    // Where the cave after this one goes in the world, given where this one is: below it, or to the side its exit is on
    public int[] getNextOrigin(int x, int y) {
        if (exit.tileX == 0) {
            return new int[]{x - getWidth(), y};
        } else if (exit.tileX == xTiles - 1) {
            return new int[]{x + getWidth(), y};
        }
        return new int[]{x, y + getHeight()};
    }

    private MathVector at(Coord tile) {
        return at(tile.tileX, tile.tileY);
    }

    // A tile's place in the world
    private MathVector at(double tileX, double tileY) {
        return new MathVector(originX + tileX * SQUARE_SIZE, originY + tileY * SQUARE_SIZE);
    }

    // The playground: a cave laid out by hand, the same every time, to try the controls in. Closed all round, with ledges
    // at different heights on the left, rock hanging from the ceiling and an island to swing around in the middle, and
    // a tall room on the right. No enemies or powers of its own; they're placed from its menu
    private Map(int xTiles, int yTiles) {
        this.map = new int[xTiles][yTiles];
        this.xTiles = xTiles;
        this.yTiles = yTiles;
        this.level = MyActivity.canvas.myActivity.level;
        this.seed = MyActivity.canvas.myActivity.seed;
        // It's never left, but other parts of the game ask where the way in and out are
        this.entrance = new Coord(xTiles / 2, 0);
        this.exit = new Coord(xTiles / 2, yTiles - 1);
        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles; y++) {
                boolean border = x <= BORDER_SIZE || x >= xTiles - BORDER_SIZE - 1 || y <= BORDER_SIZE || y >= yTiles - BORDER_SIZE - 1;
                map[x][y] = border ? 1 : 0;
            }
        }
        // Ledges on the left, to jump between and hook under
        fillRock(8, 40, 20, 42);
        fillRock(22, 30, 32, 32);
        fillRock(8, 20, 16, 22);
        // Rock hanging from the ceiling, and an island, in the hall in the middle
        fillRock(44, 4, 47, 16);
        fillRock(56, 4, 59, 12);
        fillRock(48, 26, 56, 30);
        // A pillar on the floor
        fillRock(62, 44, 65, 52);
        // The tall room on the right, behind a wall that's open at the bottom, with ledges up its walls
        fillRock(71, 4, 74, 44);
        fillRock(86, 18, 95, 20);
        fillRock(75, 34, 82, 36);
        smoothMap();
        playgroundStart = new MathVector(12 * SQUARE_SIZE, 49 * SQUARE_SIZE);
        generateMesh();
    }

    public static Map playground() {
        return new Map(PLAYGROUND_X, PLAYGROUND_Y);
    }

    private void fillRock(int fromX, int fromY, int toX, int toY) {
        for (int x = fromX; x <= toX; x++) {
            for (int y = fromY; y <= toY; y++) {
                if (isInMapRange(x, y)) {
                    map[x][y] = 1;
                }
            }
        }
    }

    // Places an enemy of the given kind a few tiles from a point in the room, where that kind lives: bats on the
    // ceiling, spitters on a wall, beetles on the floor. Tells whether there was somewhere to put it
    public boolean spawnNear(int kind, MathVector at, Random random) {
        if (kind == SPAWN_DEEP_WORM) {
            for (GameEnemy enemy : MyActivity.enemies) {
                if (enemy instanceof EnemyDeepWorm) {
                    return false;
                }
            }
            new EnemyDeepWorm(at(xTiles / 2, yTiles / 2).x, at(xTiles / 2, yTiles / 2).y, Math.max(xTiles, yTiles) / 2 * SQUARE_SIZE);
            return true;
        }
        int[][] sides = {{-1, 0}, {1, 0}, {0, -1}};
        int[][] anyway = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int attempt = 0; attempt < 40; attempt++) {
            Coord open = getOpenTileNear(at, random);
            if (open == null) {
                continue;
            }
            switch (kind) {
                case SPAWN_STALKER:
                    new EnemyStalker(at(open).x, at(open).y, true);
                    return true;
                case SPAWN_TERRA_WORM:
                    new EnemyTerraWorm(at(open).x, at(open).y, 5, true, true);
                    return true;
                case SPAWN_BAT:
                    Coord ceiling = getLastEmptyTile(open, 0, -1, 40);
                    if (ceiling != null) {
                        new EnemyBat(at(ceiling).x, at(ceiling).y);
                        return true;
                    }
                    break;
                case SPAWN_SPITTER:
                    int[] side = sides[random.nextInt(sides.length)];
                    Coord wall = getLastEmptyTile(open, side[0], side[1], 30);
                    if (wall != null && Math.abs(wall.tileX - open.tileX) + Math.abs(wall.tileY - open.tileY) >= 3) {
                        new EnemySpitter(at(wall).x, at(wall).y, new MathVector(-side[0], -side[1]));
                        return true;
                    }
                    break;
                case SPAWN_SNIPPER:
                    int[] direction = anyway[random.nextInt(anyway.length)];
                    Coord rock = getLastEmptyTile(open, direction[0], direction[1], 30);
                    if (rock != null) {
                        new EnemySnipper(at(rock).x, at(rock).y);
                        return true;
                    }
                    break;
                case SPAWN_BEETLE:
                    Coord floor = getLastEmptyTile(open, 0, 1, 40);
                    if (floor != null) {
                        new EnemyBeetle(at(floor).x, at(floor).y);
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    // An empty tile with nothing but empty tiles around it, not too close to a point in the room and not too far
    private Coord getOpenTileNear(MathVector at, Random random) {
        int centerX = (int) ((at.x - originX) / SQUARE_SIZE), centerY = (int) ((at.y - originY) / SQUARE_SIZE);
        for (int attempt = 0; attempt < 100; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = SPAWN_NEAREST + random.nextDouble() * (SPAWN_FURTHEST - SPAWN_NEAREST);
            int x = centerX + (int) Math.round(Math.cos(angle) * distance), y = centerY + (int) Math.round(Math.sin(angle) * distance);
            if (isInMapRange(x, y) && map[x][y] == 0 && getSurroundingCount(x, y) == 0) {
                return new Coord(x, y);
            }
        }
        return null;
    }

    // What's in a piece of the open world, the same every time it's made: often a group of enemies, more often the
    // deeper it is, and now and then a power or some health. Nothing where the world starts
    private void addChunkObjects() {
        Random random = new Random(seed * 31 + 17);
        boolean start = chunkX == 0 && chunkY == 0;
        // The vault is hollowed out with the piece, apart from its rooms, so nothing else placed here lands behind its door
        addVault(random);
        if (isSealRow()) {
            addSeal(random);
        }
        if (!start && random.nextDouble() < Math.min(0.8, 0.4 + 0.04 * level)) {
            addEnemyGroup(random, level);
        }
        if (random.nextDouble() < 0.25) {
            MathVector position = getRandomPointInRooms(roomRegions, 0, random);
            addPowerUp(position, random.nextInt(4));
        }
        if (!start && random.nextDouble() < 0.15) {
            MathVector position = getRandomPointInRooms(roomRegions, 0, random);
            new HealthDrop(position.x, position.y, true, false);
        }
    }

    // What the vault holds, and the door and buttons that keep it: powers, coins and health, worth the trouble of
    // finding the buttons, which are anywhere in the piece's caves but the chamber, as that was never one of its rooms
    private void addVault(Random random) {
        if (vaultCenter == null) {
            return;
        }
        MathVector door = at(vaultDoorTile.tileX, vaultDoorTile.tileY);
        addDoor(door.x, door.y, (int) ((VAULT_CORRIDOR + 2) * SQUARE_SIZE * 2), (int) (1.5 * SQUARE_SIZE),
                vaultDoorVector, createWallButtons(roomRegions, VAULT_BUTTONS, random, false));
        int kept = VAULT_POWERS + 2;
        for (int i = 0; i < VAULT_POWERS; i++) {
            addPowerUp(inVault(i, kept), random.nextInt(4));
        }
        MathVector coins = inVault(VAULT_POWERS, kept);
        new CoinBag(coins.x, coins.y);
        MathVector health = inVault(VAULT_POWERS + 1, kept);
        new HealthDrop(health.x, health.y, true, false);
    }

    // One of a number of places spread round the middle of the chamber, well clear of its walls
    private MathVector inVault(int index, int of) {
        double angle = index * 2 * Math.PI / of;
        return at(vaultCenter.tileX + Math.cos(angle) * VAULT_RADIUS * 0.5, vaultCenter.tileY + Math.sin(angle) * VAULT_RADIUS * 0.5);
    }

    // On the vault's side of its door, where the character is never put down, as the door would have it shut in
    private boolean isBehindVaultDoor(int tileX, int tileY) {
        if (vaultCenter == null) {
            return false;
        }
        double reach = Math.hypot(vaultDoorTile.tileX - vaultCenter.tileX, vaultDoorTile.tileY - vaultCenter.tileY) + 1;
        return Math.hypot(tileX - vaultCenter.tileX, tileY - vaultCenter.tileY) <= reach;
    }

    // The same, for somewhere in the world rather than a tile of this piece
    public boolean isBehindVaultDoor(double worldX, double worldY) {
        return isBehindVaultDoor((int) Math.round((worldX - originX) / SQUARE_SIZE), (int) Math.round((worldY - originY) / SQUARE_SIZE));
    }

    // Every so many rows down, the way on down is barred. Every piece in the row is, so there's no falling past one
    // further along: a whole band has to be worked over before it can be left behind
    private boolean isSealRow() {
        return chunkY > 0 && chunkY % SEAL_EVERY == SEAL_EVERY - 1;
    }

    // The seal is on the piece's way down, with its buttons spread over the piece's caves
    private void addSeal(Random random) {
        addExitGate(createWallButtons(roomRegions, SEAL_BUTTONS, random, false), null);
    }

    // Puts what's in the cave into the game: gates and their buttons, enemies, powers and health. From the main thread,
    // once, with the cave's corner at the given place in the world
    public void addObjects(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
        if (chunk) {
            addChunkObjects();
            return;
        }
        Random addingRandom = new Random();
        // Mixed differently from the map's seed + level so the two don't share a sequence. Multiplying by the level
        // gave every first level seed 0, so the same starting power-ups. The level is spread over all the bits, as
        // Random's first number, which picks doors or enemies, barely changes between seeds that differ by 1
        addingRandom.setSeed(seed * 31 + level * LEVEL_SEED_SPREAD);
        susceptibleRooms.remove(entranceRoom);
        susceptibleRooms.remove(exitRoom);
        if (roomRegions.size() > 2) {
            roomRegions.remove(entranceRoom);
            roomRegions.remove(exitRoom);
        }
        if (isBossLevel(level)) {
            addBoss();
        } else if (level > 0) {
            double r = addingRandom.nextDouble();
            if (r < 0.4) {
                addPassageDoor(2);
                addExitDoor(addingRandom, MAX_BUTTONS);
            } else if (r < 0.8) {
                addEnemies(addingRandom);
            } else {
                addExitDoor(addingRandom, MIN_BUTTONS);
                addEnemies(addingRandom);
            }
            if (addingRandom.nextBoolean()) {
                addHealth(addingRandom);
            }
        }
        addPowerUps(addingRandom);
    }

    private void createMap() {
        Random random = new Random();
        random.setSeed(seed + level);
        randomFillMap(this.fillPercent, random);

        for (int i=0; i < SMOOTH_ITERATIONS; i++){
            smoothMap();
        }

        if (isBossLevel(level)) {
            carveBossCavern();
        }

        manageEntranceAndExit(random);

        manageRooms();

        manageRoomConnection();

        manageBorders();

    }

    private Coord getEntranceFromExit(Coord exit) {
        if (exit.tileX == 0) {
            return new Coord(xTiles - 1, exit.tileY);
        } else if (exit.tileX == xTiles - 1) {
            return new Coord(0, exit.tileY);
        } else {
            return new Coord(exit.tileX, 0);
        }
    }

    private int[] getLineToCopy() {
        if (exit.tileX == 0) {
            int[] lineToCopy = new int[yTiles];
            for (int y = 0 ; y < yTiles ; y++){
                lineToCopy[y] = map[0][y];
            }
            return lineToCopy;
        } else if (exit.tileX == xTiles - 1) {
            int[] lineToCopy = new int[yTiles];
            for (int y = 0 ; y < yTiles ; y++) {
                lineToCopy[y] = map[xTiles - 1][y];
            }
            return lineToCopy;
        } else {
            int[] lineToCopy = new int[xTiles];
            for (int x = 0 ; x < xTiles ; x++){
                lineToCopy[x] = map[x][yTiles-1];
            }
            return lineToCopy;
        }
    }

    private void copyLine(int[] lineToCopy, int copies) {
        if (entrance.tileX == 0) {
            for (int y = 0; y < yTiles; y++) {
                for (int i = 0; i < copies; i++) {
                    map[i][y] = lineToCopy[y];
                }
            }
        } else if (entrance.tileX == xTiles - 1) {
            for (int y = 0; y < yTiles; y++) {
                for (int i = 0; i < copies; i++) {
                    map[xTiles - 1 - i][y] = lineToCopy[y];
                }
            }
        } else {
            for (int x = 0; x < xTiles; x++) {
                for (int i = 0; i < copies; i++) {
                    map[x][i] = lineToCopy[x];
                }
            }
        }
    }

    private void manageBorders() {
        if (getEntrance().tileX == 0) {
            for (int y = 0; y < yTiles; y++) {
                if (Math.abs(y - getEntrance().tileY) > PASSAGE_RADIUS * 1.5) {
                    map[0][y] = 1;
                }
            }
        } else if (getEntrance().tileX == xTiles - 1) {
            for (int y = 0; y < yTiles; y++) {
                if (Math.abs(y - getEntrance().tileY) > PASSAGE_RADIUS * 1.5) {
                    map[xTiles - 1][y] = 1;
                }
            }
        }
        if (getExit().tileY == yTiles - 1) {
            for (int x = 0; x < xTiles; x++) {
                if (Math.abs(x - getExit().tileX) > PASSAGE_RADIUS * 1.5) {
                    map[x][yTiles - 1] = 1;
                }
            }
        }
        borderUp(getEntrance().tileY != 0, getExit().tileY != yTiles - 1, getEntrance().tileX != 0 && getExit().tileX != 0, getEntrance().tileX != xTiles - 1 && getExit().tileX != xTiles - 1);
    }

    private void borderUp(boolean top, boolean bottom, boolean left, boolean right) {
        if (top) {
            for (int x = 0; x < xTiles; x++) {
                map[x][0] = 1;
            }
        }
        if (bottom) {
            for (int x = 0; x < xTiles; x++) {
                map[x][yTiles - 1] = 1;
            }
        }
        if (left) {
            for (int y = 0; y < yTiles; y++) {
                map[0][y] = 1;
            }
        }
        if (right) {
            for (int y = 0; y < yTiles; y++) {
                map[xTiles - 1][y] = 1;
            }
        }
    }

    private void randomFillMap(int fillPercent, Random random){
        for (int x = 0; x < xTiles; x++){
            for (int y = 0; y < yTiles; y++){
                if (x <= BORDER_SIZE || x >= xTiles-BORDER_SIZE-1 || y <= BORDER_SIZE || y >= yTiles-BORDER_SIZE-1){
                    map[x][y] = 1;
                } else {
                    map[x][y] = (random.nextInt(100) < fillPercent) ? 1 : 0;
                }
            }
        }
    }

    private void smoothMap (){
        for (int x = 0; x < xTiles; x++) {
            for (int y = 1; y < yTiles - 1; y++) {
                int neighbourWallTiles = getSurroundingCount(x,y);
                if (neighbourWallTiles > WALL_COUNT_SMOOTH_THRESHOLD){
                    map[x][y] = 1;
                } else if (neighbourWallTiles < WALL_COUNT_SMOOTH_THRESHOLD){
                    map[x][y] = 0;
                }
            }
        }
    }

    private int getSurroundingCount (int gridX, int gridY){
        int wallCount = 0;
        for (int neighbourX = gridX - 1 ; neighbourX <= gridX + 1 ; neighbourX++){
            for (int neighbourY = gridY - 1 ; neighbourY <= gridY + 1 ; neighbourY++){
                if (isInMapRange(neighbourX,neighbourY)) {
                    if (neighbourX != gridX || neighbourY != gridY) {
                        wallCount += getMapValue(neighbourX, neighbourY);
                    }
                } else {
                    wallCount += 1;
                }
            }
        }
        return wallCount;
    }

    public void generateMesh() {
        outlines.clear();
        checkedVertices.clear();
        triangleDictionary.clear();

        this.squareGrid = new SquareGrid();
        this.cracks.clear();

        this.vertices = new Vector<Point>();
        this.triangles = new Vector<Integer>();

        for (int x = 0; x < squareGrid.squares.length; x++) {
            for (int y = 0; y < squareGrid.squares[0].length; y++) {
                triangulateSquare(squareGrid.squares[x][y]);
            }
        }

        calculateMeshOutlines();
    }

    private void triangulateSquare (Square square){
        switch (square.configuration) {
            case 0: {
                break;
            }
            // 1 points:
            case 1: {
                meshFromPoints(square.centreLeft, square.centreBottom, square.bottomLeft);
                break;
            }
            case 2: {
                meshFromPoints(square.bottomRight, square.centreBottom, square.centreRight);
                break;
            }
            case 4: {
                meshFromPoints(square.topRight, square.centreRight, square.centreTop);
                break;
            }
            case 8: {
                meshFromPoints(square.topLeft, square.centreTop, square.centreLeft);
                break;
            }
            // 2 points:
            case 3: {
                meshFromPoints(square.centreRight, square.bottomRight, square.bottomLeft, square.centreLeft);
                break;
            }
            case 6: {
                meshFromPoints(square.centreTop, square.topRight, square.bottomRight, square.centreBottom);
                break;
            }
            case 9: {
                meshFromPoints(square.topLeft, square.centreTop, square.centreBottom, square.bottomLeft);
                break;
            }
            case 12: {
                meshFromPoints(square.topLeft, square.topRight, square.centreRight, square.centreLeft);
                break;
            }
            case 5: {
                meshFromPoints(square.centreTop, square.topRight, square.centreRight, square.centreBottom, square.bottomLeft, square.centreLeft);
                break;
            }
            case 10: {
                meshFromPoints(square.topLeft, square.centreTop, square.centreRight, square.bottomRight, square.centreBottom, square.centreLeft);
                break;
            }
            // 3 point:
            case 7: {
                meshFromPoints(square.centreTop, square.topRight, square.bottomRight, square.bottomLeft, square.centreLeft);
                break;
            }
            case 11: {
                meshFromPoints(square.topLeft, square.centreTop, square.centreRight, square.bottomRight, square.bottomLeft);
                break;
            }
            case 13: {
                meshFromPoints(square.topLeft, square.topRight, square.centreRight, square.centreBottom, square.bottomLeft);
                break;
            }
            case 14: {
                meshFromPoints(square.topLeft, square.topRight, square.bottomRight, square.centreBottom, square.centreLeft);
                break;
            }
            // 4 point:
            case 15: {
                meshFromPoints(square.topLeft, square.topRight, square.bottomRight, square.bottomLeft);
//                createCrack(square.topLeft.getMovedNode(SQUARE_SIZE/2,-SQUARE_SIZE/2),square.configuration);
                checkedVertices.add(square.topLeft.vertexIndex);
                checkedVertices.add(square.bottomRight.vertexIndex);
                checkedVertices.add(square.topRight.vertexIndex);
                checkedVertices.add(square.bottomLeft.vertexIndex);
                break;
            }
        }
    }

    private void meshFromPoints(Node... points){
        assignVertices(points);

        if (points.length >= 3) {
            createTriangle(points[0], points[1], points[2]);
        }
        if (points.length >= 4){
            createTriangle(points[0],points[2],points[3]);
        }
        if (points.length >= 5){
            createTriangle(points[0],points[3],points[4]);
        }
        if (points.length >= 6){
            createTriangle(points[0],points[4],points[5]);
        }
    }

    private void assignVertices(Node[] points) {
        for (int i = 0 ; i < points.length ; i++){
            if (points[i].vertexIndex == -1){
                points[i].vertexIndex = vertices.size();
                vertices.add(points[i].position.toPoint());
//                if (vertices.get(points[i].vertexIndex).y == 0 || vertices.get(points[i].vertexIndex).y == getHeight()){
//                    checkedVertices.add(points[i].vertexIndex);
//                }
            }
        }
    }

    private void createTriangle(Node a, Node b, Node c){
        triangles.add(a.vertexIndex);
        triangles.add(b.vertexIndex);
        triangles.add(c.vertexIndex);

        Triangle triangle = new Triangle(a.vertexIndex,b.vertexIndex,c.vertexIndex);
        addTriangleToDictionary(a.vertexIndex,triangle);
        addTriangleToDictionary(b.vertexIndex,triangle);
        addTriangleToDictionary(c.vertexIndex,triangle);
    }

    private void addTriangleToDictionary(int vertexIndexKey, Triangle triangle){
        if (triangleDictionary.containsKey(vertexIndexKey)){
            triangleDictionary.get(vertexIndexKey).add(triangle);
        } else {
            Vector<Triangle> triangleList = new Vector<Triangle>();
            triangleList.add(triangle);
            triangleDictionary.put(vertexIndexKey,triangleList);
        }
    }

    private boolean isOutlineEdge(int vertexA, int vertexB){
        Vector<Triangle> trianglesContainingA = triangleDictionary.get(vertexA);
        int sharedTriangleCount = 0;

        for (int i = 0; i < trianglesContainingA.size(); i++){
            if (trianglesContainingA.get(i).contains(vertexB)){
                sharedTriangleCount++;
                if (sharedTriangleCount > 1){
                    break;
                }
            }
        }

        return sharedTriangleCount == 1;
    }

    private int getConnectedOutlineVertex (int vertexIndex){
        Vector<Triangle> trianglesContainingVertex = triangleDictionary.get(vertexIndex);
        for (int i = 0; i < trianglesContainingVertex.size(); i++){
            Triangle triangle = trianglesContainingVertex.get(i);
            for (int j = 0 ; j < 3 ; j++){
                int vertexB = triangle.get(j);
                if (vertexB != vertexIndex  && !checkedVertices.contains(vertexB)) {
                    if (isOutlineEdge(vertexIndex, vertexB)) {
                        return vertexB;
                    }
                }
            }
        }
        return -1;
    }

    public void calculateMeshOutlines() {
        for (int vertexIndex = 0 ; vertexIndex < vertices.size() ; vertexIndex++){
            if (!checkedVertices.contains(vertexIndex)){
                int newOutlineVertex = getConnectedOutlineVertex(vertexIndex);
                if (newOutlineVertex != -1){
                    checkedVertices.add(vertexIndex);
                    Vector<Integer> newOutline = new Vector<Integer>();
                    newOutline.add(vertexIndex);
                    outlines.add(newOutline);
                    followOutline(newOutlineVertex,outlines.size()-1);
                    outlines.lastElement().add(vertexIndex);
                }
            }
        }
    }

    // Along the outline a vertex at a time, in a loop: calling itself for every vertex ran out of stack on the thread
    // that makes caves in the background, which has less of it than the main one
    private void followOutline(int vertexIndex, int outlineIndex) {
        while (vertexIndex != -1) {
            outlines.get(outlineIndex).add(vertexIndex);
            checkedVertices.add(vertexIndex);
            vertexIndex = getConnectedOutlineVertex(vertexIndex);
        }
    }

    private Vector<Coord> getRegionTiles (int startX, int startY){
        Vector<Coord> tiles = new Vector<Coord>();
        int[][] mapFlags = new int[xTiles][yTiles];
        int tileType = map[startX][startY];

        LinkedList<Coord> queue = new LinkedList<Coord>();
        queue.add(new Coord(startX,startY));
        mapFlags[startX][startY] = 1;

        while (queue.size() > 0){
            Coord tile = queue.pop();
            tiles.add(tile);

            for (int x = tile.tileX - 1 ; x <= tile.tileX + 1 ; x++){
                for (int y = tile.tileY - 1 ; y <= tile.tileY + 1 ; y++){
                    if(isInMapRange(x,y) && (y == tile.tileY || x == tile.tileX)){
                        if (mapFlags[x][y] == 0 && map[x][y] == tileType){
                            mapFlags[x][y] = 1;
                            queue.add(new Coord(x,y));
                        }
                    }
                }
            }
        }

        return tiles;
    }

    private Vector<Vector<Coord>> getRegions (int tileType){
        Vector<Vector<Coord>> regions = new Vector<Vector<Coord>>();
        int[][] mapFlags = new int[xTiles][yTiles];

        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles; y++) {
                if (mapFlags[x][y] == 0 && map[x][y] == tileType){
                    Vector<Coord> newRegion = getRegionTiles(x,y);
                    regions.add(newRegion);
                    for (Coord tile : newRegion){
                        mapFlags[tile.tileX][tile.tileY] = 1;
                    }
                }
            }
        }

        return regions;
    }

    public void manageRooms() {
        passages.clear();
        roomRegions.clear();
        susceptibleRooms.clear();
        roomsWithInterest.clear();

        Vector<Vector<Coord>> wallRegions = getRegions(1);

        for (Vector<Coord> wallRegion : wallRegions){
            if (wallRegion.size() < WALL_COUNT_SIZE_THRESHOLD){
                for (Coord tile : wallRegion) {
                    map[tile.tileX][tile.tileY] = 0;
                }
            }
        }

        Vector<Vector<Coord>> roomRegionsPrev = getRegions(0);

        for (Vector<Coord> roomRegion : roomRegionsPrev){
            if (roomRegion.size() < ROOM_COUNT_SIZE_THRESHOLD){
                for (Coord tile : roomRegion){
                    map[tile.tileX][tile.tileY] = 1;
                }
            } else {
                roomRegions.add(new Room(roomRegion));
            }
        }
        Collections.sort(roomRegions);

        roomRegions.firstElement().isMainRoom = true;
        roomRegions.firstElement().isAccessibleFromMainRoom = true;

        exitRoom = exit.getRoom(roomRegions);
        if (level > 0) {
            entranceRoom = entrance.getRoom(roomRegions);
        }

        for (Room room : roomRegions){
            if (room.roomSize < maxSizeForSusceptible && !room.isUpOrDown()){
                addSusceptibleRoom(room);
            }
        }
    }

    public void manageRoomConnection() {
        connectClosestRooms(roomRegions, false);
    }

    private void connectClosestRooms (Vector<Room> allRooms, boolean forceAccessibilityFromMainRoom){
        Vector<Room> roomListA = new Vector<Room>();
        Vector<Room> roomListB = new Vector<Room>();

        if (forceAccessibilityFromMainRoom){
            for (Room room : allRooms){
                if (room.isAccessibleFromMainRoom){
                    roomListB.add(room);
                } else {
                    roomListA.add(room);
                }
            }
        } else {
            roomListA = allRooms;
            roomListB = allRooms;
        }

        int bestDistance = 0;
        Coord bestTileA = new Coord();
        Coord bestTileB = new Coord();
        Room bestRoomA = new Room();
        Room bestRoomB = new Room();
        boolean possibleConnectionFound = false;

        for (Room roomA : roomListA){
            if (!forceAccessibilityFromMainRoom) {
                possibleConnectionFound = false;
                if (roomA.connectedRooms.size() > 0){
                    continue;
                }
            }
            for (Room roomB : roomListB){
                if (roomA == roomB || roomA.isConnected(roomB)){
                    continue;
                }
                for (int tileIndexA = 0 ; tileIndexA < roomA.edgeTiles.size() ; tileIndexA++){
                    for (int tileIndexB = 0 ; tileIndexB < roomB.edgeTiles.size() ; tileIndexB++) {
                        Coord tileA = roomA.edgeTiles.get(tileIndexA);
                        Coord tileB = roomB.edgeTiles.get(tileIndexB);
                        int distanceBetweenRooms = (int) (Math.pow(tileA.tileX-tileB.tileX,2) + Math.pow(tileA.tileY-tileB.tileY,2));

                        if (distanceBetweenRooms < bestDistance || !possibleConnectionFound){
                            bestDistance = distanceBetweenRooms;
                            possibleConnectionFound = true;
                            bestTileA = tileA;
                            bestTileB = tileB;
                            bestRoomA = roomA;
                            bestRoomB = roomB;
                        }
                    }
                }
            }
            if (possibleConnectionFound && !forceAccessibilityFromMainRoom){
                createPassage(bestRoomA, bestRoomB, bestTileA, bestTileB);
            }
        }
        if (possibleConnectionFound && forceAccessibilityFromMainRoom){
            createPassage(bestRoomA,bestRoomB,bestTileA,bestTileB);
            connectClosestRooms(allRooms, true);
        }
        if (!forceAccessibilityFromMainRoom){
            connectClosestRooms(allRooms, true);
        }
    }

    private void createPassage (Room roomA, Room roomB, Coord tileA, Coord tileB) {
        Passage passage = getPassage(tileA, tileB, roomA, roomB);

        connectRooms(roomA, roomB);
        for (Coord c : passage.line) {
            drawCircle(c, PASSAGE_RADIUS);
        }
        this.passages.add(passage);
    }

    private Passage getPassage(Coord from, Coord to, Room roomA, Room roomB){
        Vector<Coord> line = new Vector<Coord>();
        MathVector vector = new MathVector(to.toRoomPoint().x - from.toRoomPoint().x,to.toRoomPoint().y - from.toRoomPoint().y);

        int x = from.tileX;
        int y = from.tileY;

        int dx = to.tileX - from.tileX;
        int dy = to.tileY - from.tileY;

        boolean inverted = false;
        int step = (int) Math.signum(dx);
        int gradientStep = (int) Math.signum(dy);

        int longest = Math.abs(dx);
        int shortest = Math.abs(dy);

        if (longest < shortest){
            inverted = true;
            longest = Math.abs(dy);
            shortest = Math.abs(dx);
            step = (int) Math.signum(dy);
            gradientStep = (int) Math.signum(dx);
        }

        int gradientAccumulation = longest / 2;
        for (int i=0 ; i < longest ; i++){
            line.add(new Coord(x,y));

            if (inverted){
                y += step;
            } else {
                x += step;
            }

            gradientAccumulation += shortest;
            if (gradientAccumulation >= longest){
                if (inverted){
                    x += gradientStep;
                } else {
                    y += gradientStep;
                }
                gradientAccumulation -= longest;
            }
        }

        return new Passage(vector, line, roomA, roomB);
    }

    private void drawCircle (Coord c, int r){
        for (int x = -r ; x <= r ; x++){
            for (int y = -r ; y <= r ; y++) {
                if (x*x + y*y <= r*r){
                    int drawX = c.tileX+x;
                    int drawY = c.tileY+y;
                    if (isInMapRange(drawX,drawY)) {
                        map[drawX][drawY] = 0;
                    }
                }
            }
        }
    }

    public MathVector startPosition () {
        if (playgroundStart != null) {
            return playgroundStart;
        }
        if (level == 0) {
            for (int yTile = 0; yTile < yTiles; yTile++) {
                for (int xTile = 0; xTile < xTiles; xTile++) {
                    if (map[xTile][yTile] == 0) {
                        if (level == 0) {
                            if (getSurroundingCount(xTile, yTile) == 0) {
                                if (isInMapRange(xTile, yTile + 2)) {
                                    if (map[xTile][yTile + 2] == 1) {
                                        return at(xTile, yTile);
                                    }
                                }
                            }
                        } else {
                            return at(xTile, yTile);
                        }
                    }
                }
            }
        } else {
            // A tile in from the way in, which is on the cave's very edge, and beyond the edge it's rock
            return at(Math.max(1, Math.min(getEntrance().tileX, xTiles - 2)), Math.max(1, Math.min(getEntrance().tileY, yTiles - 2)));
        }
        return new MathVector(0, 0);
    }

    private void manageEntranceAndExit(Random random) {
        // Decide where the exit will be
        boolean exitOnSides = random.nextBoolean();
        if (level == 0) {
            exitOnSides = false;
        }
        if (exitOnSides) {
            // Exit on the side
            if (entrance.tileX == 0) {
                exit = new Coord(xTiles - 1, Math.min(random.nextInt(yTiles) + 1, yTiles - 2));
                manageRightExit(3);
            } else if (entrance.tileX == xTiles - 1) {
                exit = new Coord(0, Math.min(random.nextInt(yTiles) + 1, yTiles - 2));
                manageLeftExit(3);
            } else {
                if (random.nextBoolean()) {
                    exit = new Coord(xTiles - 1, Math.min(random.nextInt(yTiles) + 1, yTiles - 2));
                    manageRightExit(3);
                } else {
                    exit = new Coord(0, Math.min(random.nextInt(yTiles) + 1, yTiles - 2));
                    manageLeftExit(3);
                }
            }
        } else {
            exit = new Coord(Math.min(random.nextInt(xTiles) + 1, xTiles - 2), yTiles - 1);
            manageDownExit(3);
        }
        if (level > 0) {
            drawCircle(entrance, (int) (PASSAGE_RADIUS * 1.5));
        }
    }

    private void manageDownExit(int copies) {
        drawCircle(exit, (int) (PASSAGE_RADIUS * 1.5));
        for (int x = 0; x < xTiles; x++) {
            for (int i = 1; i < copies; i++) {
                map[x][yTiles - 1 - i] = map[x][yTiles - 1];
            }
        }
    }

    private void manageLeftExit(int copies) {
        drawCircle(exit, (int) (PASSAGE_RADIUS * 1.5));
        for (int y = 0; y < yTiles; y++) {
            for (int i = 1; i < copies; i++) {
                map[i][y] = map[0][y];
            }
        }
    }

    private void manageRightExit(int copies) {
        drawCircle(exit, (int) (PASSAGE_RADIUS * 1.5));
        for (int y = 0; y < yTiles; y++) {
            for (int i = 1; i < copies; i++) {
                map[xTiles - 1 - i][y] = map[xTiles - 1][y];
            }
        }
    }

    public MathVector getRandomPointInRooms(Vector<Room> rooms, int maxWallCount, Random r) {
        Coord coord;
        int n = 0;
        Room room;
        do {
            do {
                room = rooms.get(r.nextInt(rooms.size()));
            } while (roomsWithInterest.contains(room) && rooms.size() > roomsWithInterest.size());
            coord = room.tiles.get(r.nextInt(room.roomSize));
            n++;
        } while ((map[coord.tileX][coord.tileY] == 1 || getSurroundingCount(coord.tileX,coord.tileY) > maxWallCount || isUpOrDown(coord)) && n < 1000);
        roomsWithInterest.add(room);
        return at(coord);
    }

    public MathVector getRandomPointInRoom(Room room, int maxWallCount, Random r) {
        Coord coord;
        int n = 0;
        do {
            coord = room.tiles.get(r.nextInt(room.roomSize));
            n++;
        } while ((map[coord.tileX][coord.tileY] == 1 || getSurroundingCount(coord.tileX,coord.tileY) > maxWallCount || isUpOrDown(coord)) && n < 1000);
        return at(coord);
    }

    private boolean isUpOrDown(Coord coord) {
        return coord.tileY >= yTiles - 5 || coord.tileY <= 5;
    }

    public MathVector getRandomEmptyPoint(int wallCount, Random r){
        int x,y;
        int n = 0;
        do {
            x = r.nextInt(xTiles);
            y = r.nextInt(yTiles);
            n++;
        } while ((map[x][y] == 1 || getSurroundingCount(x,y) != wallCount) && n < 1000);
        return at(x, y);
    }

    public void addBallObstacles(int maxObstacles){
        int[][] structure = {{-1,0,0,0,0,0,0,0,-1},
                {-1,0,0,0,0,0,0,0,-1},
                {-1,-1,0,0,0,0,0,-1,-1},
                {-1,-1,0,0,0,0,0,-1,-1},
                {-1,-1,0,0,0,0,0,-1,-1},
                {-1,-1,1,0,0,0,1,-1,-1},
                {1,1,1,1,1,1,1,1,1},
                {1,1,1,1,1,1,1,1,1},
                {1,1,1,1,1,1,1,1,1},
                {1,1,1,1,1,1,1,1,1},
                {1,1,1,1,1,1,1,1,1},
                {1,1,1,1,1,1,1,1,1}};
        int[][] subsStructure = {{1,1,1,1,0,1,1,1,1},
                {1,1,1,0,0,0,1,1,1},
                {1,0,0,0,0,0,0,0,1},
                {1,0,0,0,0,0,0,0,1},
                {1,0,0,0,0,0,0,0,1},
                {1,1,1,0,0,0,1,1,1}};
        int xStart = 0;
        int yStart = 0;
        int added = 0;
        while (added < maxObstacles) {
            Coord startStructure = findStructureInMap(structure,xStart,yStart);
            if (startStructure.tileX >= 0) {
                substituteStructure(startStructure.tileX,startStructure.tileY+6,subsStructure);
                int xBall = startStructure.tileX + 4;
                int yBall = startStructure.tileY + 2;
                Ball ball = new Ball(at(xBall, yBall).x, at(xBall, yBall).y, 100, 6, (float) (2*SQUARE_SIZE), true);
                added++;
                xStart = startStructure.tileX;
                yStart = startStructure.tileY;
//                Vector<Coord> coordsForRoom = new Vector<Coord>();
//                coordsForRoom.add(new Coord(xBall,yBall+7));
//                Room susceptibleRoom = new Room(coordsForRoom);
//                susceptibleRooms.add(susceptibleRoom);
            } else {
                added = maxObstacles;
            }
        }
    }

    private Coord findStructureInMap (int[][] structure, int xStart, int yStart){
        for (int x = xStart; x < xTiles - structure[0].length; x++) {
            for (int y = yStart; y < yTiles - structure.length; y++) {
                if (hasSameStructure(x,y,structure)){
                    return new Coord(x,y);
                }
            }
        }
        return new Coord(-1,-1);
    }

    private boolean hasSameStructure (int xStart, int yStart, int[][] structure){
        for (int x = 0 ; x < structure[0].length ; x++){
            for (int y = 0 ; y < structure.length ; y++){
                if (structure[y][x] >= 0) {
                    if (map[xStart + x][yStart + y] != structure[y][x]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void substituteStructure (int xStart, int yStart, int[][] structure){
        for (int x = 0 ; x < structure[0].length ; x++){
            for (int y = 0 ; y < structure.length ; y++){
                map[xStart + x][yStart + y] = structure[y][x];
            }
        }
    }

    private void addSusceptibleRoom(Room room) {
        if (!susceptibleRooms.contains(room)) {
            susceptibleRooms.add(room);
        } else {
            susceptibleRooms.remove(room);
            susceptibleRooms.add(room);
        }
    }

    // The gate covers the whole hole the exit makes in the border, and a square of rock at each end. The hole isn't
    // always centred on the exit: near a corner, the map's edge and the next border cut part of it off, and a gate
    // centred on the exit left a gap there
    private void addExitDoor(Random random, int maxButtons) {
        int nButtons = getNButtons(random, maxButtons);
//      Set the WallButtons
        addExitGate(createWallButtons(roomRegions, nButtons, random, true), null);
    }

    private Door addExitGate(Vector<WallButton> buttons, GameEnemy guardian) {
        boolean onSide = getExit().tileX == 0 || getExit().tileX == xTiles - 1;
        int length = onSide ? yTiles : xTiles;
        int start = onSide ? getExit().tileY : getExit().tileX;
        int end = start;
        while (start > 0 && isOpenAlongExit(onSide, start - 1)) {
            start--;
        }
        while (end < length - 1 && isOpenAlongExit(onSide, end + 1)) {
            end++;
        }
        double middle = (start + end) / 2.0;
        int width = (int) ((end - start + 3) * SQUARE_SIZE), thickness = (int) (1.5 * SQUARE_SIZE);
        Door gate;
        if (onSide) {
            MathVector place = at(getExit().tileX, middle);
            gate = new Door(place.x, place.y, width, thickness, new MathVector(0, 1), buttons, true);
        } else {
            MathVector place = at(middle, getExit().tileY);
            gate = new Door(place.x, place.y, width, thickness, new MathVector(1, 0), buttons, true);
        }
        gate.setGuardian(guardian);
        return gate;
    }

    // Whether a tile of the border line the exit is in, counted along that line, is open
    private boolean isOpenAlongExit(boolean onSide, int along) {
        return onSide ? map[getExit().tileX][along] == 0 : map[along][getExit().tileY] == 0;
    }

    private int getNButtons(Random random, int maxButtons) {
        int n = random.nextInt(maxButtons);
        return Math.max(MIN_BUTTONS,n);
    }

    public void addDoor(double xPos, double yPos, int width, int height, MathVector vector, Vector<WallButton> buttons) {
        new Door(xPos, yPos, width, height, vector, buttons, true);
    }

    public void addPassageDoor(int nButtons) {
        Random obstacleRandom = new Random();
        obstacleRandom.setSeed(seed + nButtons + level);

        if (entranceRoom == null || passages.size() == 0) {
            return;
        }
        Passage passage;
        do {
            passage = passages.get(obstacleRandom.nextInt(passages.size()));
        } while ((passage.roomA == entranceRoom || passage.roomA == exitRoom || passage.roomB == entranceRoom || passage.roomB == exitRoom) && passages.size() > 1);
//      Separate rooms by accessibility
        Vector<Room> roomsA = getRoomsConnectedWithException(passage.roomA, passage.roomB);
        Vector<Room> roomsB = new Vector<Room>(roomRegions);
        roomsB.removeAll(roomsA);
        Vector<Room> accessibleRegions = new Vector<Room>();
        for (Room room : roomsA) {
            if (room == entranceRoom) {
                accessibleRegions = roomsA;
                Collections.sort(roomsB);
                for (Room roomb : roomsB) {
                    addSusceptibleRoom(roomb);
                }
            }
        }
        if (accessibleRegions.size() == 0) {
            accessibleRegions = roomsB;
            Collections.sort(roomsA);
            for (Room rooma : roomsA) {
                addSusceptibleRoom(rooma);
            }
        }
//      Set the WallButtons
        Vector<WallButton> buttons = createWallButtons(accessibleRegions, nButtons, obstacleRandom, false);
        MathVector position = passage.getCenterInRoom();
        addDoor(position.x, position.y, (int) ((PASSAGE_RADIUS + 2) * SQUARE_SIZE * 2), (int) (1.5*SQUARE_SIZE), passage.vector.getNormal(), buttons);
    }

    public Vector<Room> getRoomsConnectedWithException(Room room, Room exceptionRoom) {
        Vector<Room> rooms = new Vector<Room>();
        LinkedList<Room> queue = new LinkedList<Room>();
        queue.add(room);
        while (queue.size() > 0) {
            Room currentRoom = queue.pop();
            if (!rooms.contains(currentRoom)) {
                rooms.add(currentRoom);
            }
            for (Room connectedRoom : currentRoom.connectedRooms) {
                if (connectedRoom != exceptionRoom && !rooms.contains(connectedRoom)) {
                    queue.add(connectedRoom);
                }
            }
        }
        return rooms;
    }

    public Vector<WallButton> createWallButtons(Vector<Room> rooms, int nButtons, Random random, boolean useSusceptibleRooms) {
        Vector<WallButton> buttons = new Vector<WallButton>();
        for (int i = 0; i < nButtons; i++) {
            MathVector position;
            if (useSusceptibleRooms && susceptibleRooms.size() > 0) {
                position = getPositionFromSusceptibleRooms(random);
            } else {
                position = getRandomPointInRooms(rooms, 0, random);
            }
            WallButton button = new WallButton(position.x, position.y, (float) (SQUARE_SIZE * 0.8), false, true, false);
            buttons.add(button);
        }
        return buttons;
    }

    public void addPowerUps(Random random) {
        int N = random.nextInt(MAX_POWERUPS);
        for (int i = 0; i < N; i++) {
            MathVector position;
            if (susceptibleRooms.size() > 0) {
                position = getPositionFromSusceptibleRooms(random);
            } else {
                position = getRandomPointInRooms(roomRegions, 0, random);
            }
            addPowerUp(position, random.nextInt(4));
        }
    }

    private void addPowerUp(MathVector position, int powerUpType) {
        if (powerUpType == 0) {
            new PortalPowerUp(position.x, position.y, (int) SQUARE_SIZE / 2, true, false);
        } else if (powerUpType == 1) {
            new CompassPowerUp(position.x, position.y, (int) (SQUARE_SIZE * 0.8), true, false);
        } else if (powerUpType == 2) {
            new BombPowerUp(position.x, position.y, (int) (SQUARE_SIZE * 0.8), true, false);
        } else if (powerUpType == 3) {
            new InfiniteJumpsPowerUp(position.x, position.y, (int) (SQUARE_SIZE * 0.9), (int) (SQUARE_SIZE * 0.8), true, false);
        }
    }

    public void addHealth(Random random) {
        int N = random.nextInt(MAX_HEALTH);
        for (int i = 0; i < N; i++) {
            MathVector position;
            if (susceptibleRooms.size() > 0) {
                position = getPositionFromSusceptibleRooms(random);
            } else {
                position = getRandomPointInRooms(roomRegions, 0, random);
            }
            new HealthDrop(position.x, position.y, true, false);
        }
    }

    private MathVector getPositionFromSusceptibleRooms(Random random) {
        if (susceptibleRooms.size() > roomsWithInterest.size()) {
            susceptibleRooms.removeAll(roomsWithInterest);
        }
        MathVector position = getRandomPointInRoom(susceptibleRooms.lastElement(), 0, random);
        roomsWithInterest.add(susceptibleRooms.lastElement());
        susceptibleRooms.remove(susceptibleRooms.lastElement());
        return position;
    }

    public static boolean isBossLevel(int level) {
        return BOSS_LEVELS && level > 0 && level % BOSS_EVERY == 0;
    }

    // A wide cavern in the middle of the cave, where the deep worm lives
    private void carveBossCavern() {
        int centerX = xTiles / 2, centerY = yTiles / 2;
        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles; y++) {
                double dx = (x - centerX) / (double) BOSS_CAVERN_X, dy = (y - centerY) / (double) BOSS_CAVERN_Y;
                if (dx * dx + dy * dy <= 1) {
                    map[x][y] = 0;
                }
            }
        }
    }

    // The deep worm, and no other enemies. The exit stays shut until it's beaten
    private void addBoss() {
        EnemyDeepWorm worm = new EnemyDeepWorm(at(xTiles / 2, yTiles / 2).x, at(xTiles / 2, yTiles / 2).y,
                Math.max(BOSS_CAVERN_X, BOSS_CAVERN_Y) * SQUARE_SIZE * 1.4);
        addExitGate(new Vector<WallButton>(), worm);
    }

    // More groups of enemies come as the cave gets deeper, and new kinds of enemy join them
    public void addEnemies (Random random) {
        int groups = Math.min(MAX_ENEMY_GROUPS, 1 + level / BOSS_EVERY);
        for (int i = 0; i < groups; i++) {
            addEnemyGroup(random, level);
        }
    }

    private void addEnemyGroup(Random random, int level) {
        int kinds = (level >= BEETLE_LEVEL) ? 6 : (level >= SNIPPER_LEVEL) ? 5 : (level >= SPITTER_LEVEL) ? 4 : (level >= BAT_LEVEL) ? 3 : 2;
        int kind = random.nextInt(kinds);
        // A piece of the open world is a fraction of a cave, and a worm is a match for a whole one, so most of the
        // pieces that would have one get stalkers instead, and all of those round where the world starts
        if (chunk && kind == 1 && (random.nextDouble() < CHUNK_WORMS_DROPPED || (Math.abs(chunkX) <= 1 && chunkY <= 1))) {
            kind = 0;
        }
        switch (kind) {
            case 0:
                int N = getNEnemies(random);
                for (int i = 0; i < N; i++) {
                    MathVector p = getRandomEmptyPoint(0, random);
                    new EnemyStalker(p.x, p.y, true);
                }
                break;
            case 1:
                MathVector p = getRandomEmptyPoint(0, random);
                new EnemyTerraWorm(p.x, p.y, 5, true, true);
                break;
            case 2:
                addBats(random);
                break;
            case 3:
                addSpitters(random);
                break;
            case 4:
                addSnipper(random);
                break;
            default:
                addBeetle(random);
        }
    }

    // Two or three on the same ceiling, a few tiles apart
    private void addBats(Random random) {
        Coord open = getOpenTile(random);
        if (open == null) {
            return;
        }
        int bats = 2 + random.nextInt(2), placed = 0;
        int[] offsets = {0, 3, -3, 6, -6};
        for (int offset : offsets) {
            int x = open.tileX + offset;
            if (placed < bats && isInMapRange(x, open.tileY) && map[x][open.tileY] == 0) {
                Coord ceiling = getLastEmptyTile(new Coord(x, open.tileY), 0, -1, 30);
                if (ceiling != null) {
                    new EnemyBat(at(ceiling).x, at(ceiling).y);
                    placed++;
                }
            }
        }
    }

    // One or two, on walls and ceilings with room in front of them
    private void addSpitters(Random random) {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}};
        int spitters = 1 + random.nextInt(2);
        for (int i = 0; i < spitters; i++) {
            for (int attempt = 0; attempt < 20; attempt++) {
                Coord open = getOpenTile(random);
                int[] direction = directions[random.nextInt(directions.length)];
                Coord wall = (open == null) ? null : getLastEmptyTile(open, direction[0], direction[1], 20);
                if (wall != null && Math.abs(wall.tileX - open.tileX) + Math.abs(wall.tileY - open.tileY) >= 3) {
                    new EnemySpitter(at(wall).x, at(wall).y, new MathVector(-direction[0], -direction[1]));
                    break;
                }
            }
        }
    }

    // On any rock
    private void addSnipper(Random random) {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int attempt = 0; attempt < 20; attempt++) {
            Coord open = getOpenTile(random);
            int[] direction = directions[random.nextInt(directions.length)];
            Coord rock = (open == null) ? null : getLastEmptyTile(open, direction[0], direction[1], 20);
            if (rock != null) {
                new EnemySnipper(at(rock).x, at(rock).y);
                return;
            }
        }
    }

    // On a floor
    private void addBeetle(Random random) {
        for (int attempt = 0; attempt < 20; attempt++) {
            Coord open = getOpenTile(random);
            Coord floor = (open == null) ? null : getLastEmptyTile(open, 0, 1, 30);
            if (floor != null) {
                new EnemyBeetle(at(floor).x, at(floor).y);
                return;
            }
        }
    }

    // An empty tile with nothing but empty tiles around it, away from the entrance and the top and bottom borders
    private Coord getOpenTile(Random random) {
        for (int attempt = 0; attempt < 200; attempt++) {
            Coord tile = new Coord(random.nextInt(xTiles), random.nextInt(yTiles));
            if (map[tile.tileX][tile.tileY] == 0 && getSurroundingCount(tile.tileX, tile.tileY) == 0 && !isUpOrDown(tile)
                    && (chunk || Math.hypot(tile.tileX - entrance.tileX, tile.tileY - entrance.tileY) > SAFE_FROM_ENTRANCE)) {
                return tile;
            }
        }
        return null;
    }

    // Walks from a tile in a direction, and gives the last empty tile before rock, if rock is that close
    private Coord getLastEmptyTile(Coord from, int dx, int dy, int maxTiles) {
        int x = from.tileX, y = from.tileY;
        for (int i = 0; i < maxTiles; i++) {
            if (!isInMapRange(x + dx, y + dy)) {
                return null;
            }
            if (map[x + dx][y + dy] == 1) {
                return new Coord(x, y);
            }
            x += dx;
            y += dy;
        }
        return null;
    }

    private int getNEnemies(Random random) {
        return random.nextInt(MAX_ENEMIES) + 1;
    }

    private boolean isInMapRange (int x, int y){
        return x >= 0 && x < xTiles && y >= 0 && y < yTiles;
    }

    public int[][] getMap() {
        return map;
    }

    public int getMapValue(int x, int y){
        return map[x][y];
    }

    public void setMap(int[][] map) {
        this.map = map;
    }

    public Coord getExit() {
        return exit;
    }

    public Coord getEntrance() {
        return entrance;
    }

    public int getWidth() {
        return (int) ((xTiles)*SQUARE_SIZE - SQUARE_SIZE);
    }

    public int getHeight() {
        return (int) ((yTiles)*SQUARE_SIZE - SQUARE_SIZE);
    }

    public class Passage {
        public MathVector vector;
        public Vector<Coord> line;
        public Room roomA, roomB;

        public Passage(MathVector vector, Vector<Coord> line, Room roomA, Room roomB) {
            this.vector = vector.normalized();
            this.line = line;
            this.roomA = roomA;
            this.roomB = roomB;
        }

        public MathVector getCenterInRoom (){
            return at(line.get(line.size()/2));
        }
    }

    public class SquareGrid {
        public Square[][] squares;

        public SquareGrid (){
            int nodeCountX = map.length;
            int nodeCountY = map[0].length;

            ControlNode[][] controlNodes = new ControlNode[nodeCountX][nodeCountY];

            for (int x=0 ; x < nodeCountX ; x++){
                for (int y=0 ; y < nodeCountY ; y++){
                    MathVector position = new MathVector(x*SQUARE_SIZE,y*SQUARE_SIZE);
                    controlNodes[x][y] = new ControlNode(position,getMapValue(x,y)==1);
                }
            }

            squares = new Square[nodeCountX-1][nodeCountY-1];

            for (int x=0 ; x < nodeCountX - 1 ; x++){
                for (int y=0 ; y < nodeCountY - 1 ; y++){
                    squares[x][y] = new Square(controlNodes[x][y+1],controlNodes[x+1][y+1],controlNodes[x+1][y],controlNodes[x][y]);
                }
            }
        }
    }

    public class Square {
        public ControlNode topLeft, topRight, bottomRight, bottomLeft;
        public Node centreTop, centreRight, centreBottom, centreLeft;
        public int configuration = 0;

        public Square (ControlNode topLeft,ControlNode topRight,ControlNode bottomRight,ControlNode bottomLeft){
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomRight = bottomRight;
            this.bottomLeft = bottomLeft;

            this.centreTop = topLeft.right;
            this.centreRight = bottomRight.above;
            this.centreBottom = bottomLeft.right;
            this.centreLeft = bottomLeft.above;

            if (topLeft.active)
                configuration += 8;
            if (topRight.active)
                configuration += 4;
            if (bottomRight.active)
                configuration += 2;
            if (bottomLeft.active)
                configuration += 1;
        }
    }

    public class Triangle {
        int[] vertices;

        public Triangle(int vertexIndexA, int vertexIndexB, int vertexIndexC) {
            vertices = new int[3];
            vertices[0] = vertexIndexA;
            vertices[1] = vertexIndexB;
            vertices[2] = vertexIndexC;
        }

        public boolean contains (int vertexIndex){
            return vertexIndex == vertices[0] || vertexIndex == vertices[1] || vertexIndex == vertices[2];
        }

        public int get(int i){
            return vertices[i];
        }
    }

    public class Node {
        public MathVector position;
        public int vertexIndex = -1;

        public Node (MathVector position){
            this.position = position;
        }

        public Node getMovedNode (double dx, double dy){
            return new Node(new MathVector(position.x + dx, position.y + dy));
        }
    }

    public class ControlNode extends Node {

        public boolean active;
        public Node above, right;

        public ControlNode(MathVector position, boolean active) {
            super(position);
            this.active = active;
            this.above = new Node(new MathVector(position.x,position.y + SQUARE_SIZE/2));
            this.right = new Node(new MathVector(position.x + SQUARE_SIZE/2,position.y));
        }
    }

    public class Room implements Comparable<Room>{
        public Vector<Coord> tiles;
        public Vector<Coord> edgeTiles;
        public Vector<Room> connectedRooms;
        public int roomSize;
        public boolean isMainRoom, isAccessibleFromMainRoom;

        public Room(){}

        public Room(Vector<Coord> tiles) {
            this.tiles = tiles;
            roomSize = tiles.size();
            connectedRooms = new Vector<Room>();
            edgeTiles = new Vector<Coord>();

            for (Coord tile : tiles){
                for (int x = tile.tileX -1 ; x <= tile.tileX +1 ; x++){
                    for (int y = tile.tileY -1 ; y <= tile.tileY +1 ; y++) {
                        if (x == tile.tileX || y == tile.tileY){
                            if (isInMapRange(x,y)) {
                                if (map[x][y] == 1) {
                                    edgeTiles.add(tile);
                                }
                            }
                        }
                    }
                }
            }
        }

        public boolean isConnected (Room otherRoom){
            return connectedRooms.contains(otherRoom);
        }

        public void setAccessibleFromMainRoom(){
            if (!isAccessibleFromMainRoom){
                isAccessibleFromMainRoom = true;
                for (Room connectedRoom : connectedRooms){
                    connectedRoom.setAccessibleFromMainRoom();
                }
            }
        }

        public Coord getCenterTileInRoom(){
            int xAverage = 0, yAverage = 0;
            for (Coord tile : tiles) {
                xAverage += tile.tileX;
                yAverage += tile.tileY;
            }
            return new Coord(xAverage / roomSize, yAverage / roomSize);
        }

        public boolean isUpOrDown() {
            Coord center = getCenterTileInRoom();
            return center.tileY < 5 || center.tileY > yTiles - 5;
        }

        public void fill(int color) {
            for (Coord tile : tiles) {
                Point position = tile.toRoomPoint();
                CircleShape c = new CircleShape(position.x, position.y, (int) SQUARE_SIZE /3, color, true);
            }
        }

        @Override
        public int compareTo(Room another) {
            return another.roomSize - this.roomSize;
        }
    }

    private void connectRooms(Room roomA, Room roomB){
        if (roomA.isAccessibleFromMainRoom){
            roomB.setAccessibleFromMainRoom();
        } else if (roomB.isAccessibleFromMainRoom){
            roomA.setAccessibleFromMainRoom();
        }
        roomA.connectedRooms.add(roomB);
        roomB.connectedRooms.add(roomA);
    }


    public void draw(Canvas canvas, CavePalette palette){
        drawRock(canvas, palette);
        drawDepth(canvas, palette);
        drawOutlines(canvas, palette.outline);
        for (Point[] crack : cracks) {
            DrawUtil.drawVoidPolygon(crack, canvas, Color.BLACK, MyActivity.TILE_WIDTH / 50, false);
        }
    }

    private void drawRock(Canvas canvas, CavePalette palette) {
        // One path for all the rock, as filling each triangle apart left thin seams between them
        Path rock = new Path();
        for (int i = 0; i < triangles.size(); i += 3) {
            Point a = vertices.get(triangles.get(i));
            Point b = vertices.get(triangles.get(i + 1));
            Point c = vertices.get(triangles.get(i + 2));
            rock.moveTo(a.x, a.y);
            rock.lineTo(b.x, b.y);
            rock.lineTo(c.x, c.y);
            rock.close();
        }
        BitmapShader texture = new BitmapShader(CaveTextures.getRock(palette), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix scale = new Matrix();
        scale.setScale((float) SQUARE_SIZE / ROCK_TEXELS_PER_SQUARE, (float) SQUARE_SIZE / ROCK_TEXELS_PER_SQUARE);
        texture.setLocalMatrix(scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setShader(texture);
        canvas.drawPath(rock, paint);
    }

    public Vector<Point> getVertices() {
        return vertices;
    }

    public Vector<Integer> getTriangles() {
        return triangles;
    }

    public Vector<Vector<Integer>> getOutlines() {
        return outlines;
    }

    // Rock is lit next to the caves and darker deeper in. Worked out per tile and blurred, a pixel for every tile, to
    // be drawn over the rock scaled up smoothly, with its first pixel's middle on the cave's corner
    public Bitmap createShading(CavePalette palette) {
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
        int[] pixels = new int[xTiles * yTiles];
        for (int i = 0; i < pixels.length; i++) {
            int lightAlpha = (int) (RIM_ALPHA * light[i]);
            int alpha = Math.min(255, lightAlpha + (int) (SHADOW_ALPHA * shadow[i]));
            float rimShare = (alpha == 0) ? 0 : (float) lightAlpha / alpha;
            pixels[i] = Color.argb(alpha, (int) (Color.red(palette.rim) * rimShare), (int) (Color.green(palette.rim) * rimShare), (int) (Color.blue(palette.rim) * rimShare));
        }
        return Bitmap.createBitmap(pixels, xTiles, yTiles, Bitmap.Config.ARGB_8888);
    }

    private void drawDepth(Canvas canvas, CavePalette palette) {
        Bitmap shading = createShading(palette);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        // Only on the rock already drawn, and keeping it opaque, as collisions look for opaque pixels
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        float half = (float) SQUARE_SIZE / 2;
        canvas.drawBitmap(shading, null, new RectF(-half, -half, xTiles * (float) SQUARE_SIZE - half, yTiles * (float) SQUARE_SIZE - half), paint);
        shading.recycle();
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

    public void drawMap (Canvas canvas){
        int tileWidth = 12;
        int tileHeight = 8;
        Paint black = new Paint();
        Paint white = new Paint();
        black.setColor(Color.BLACK);
        white.setColor(Color.WHITE);
        for (int x = 0; x < xTiles; x++){
            for (int y = 0; y < xTiles; y++){
                int xPos = x * tileWidth;
                int yPos = y * tileHeight;
                Rect rect = new Rect(xPos,yPos,xPos+tileWidth,yPos+tileHeight);
                if (getMapValue(x,y) == 1) {
                    canvas.drawRect(rect, black);
                } else {
                    canvas.drawRect(rect, white);
                }
            }
        }
    }

    public void drawMesh (Canvas canvas){
        for (int i=0 ; i < triangles.size() ; i+=3){
            Point[] points = new Point[3];
            points[0] = vertices.get(triangles.get(i));
            points[1] = vertices.get(triangles.get(i+1));
            points[2] = vertices.get(triangles.get(i+2));
            DrawUtil.drawPolygon(points, canvas, Color.argb(255, 60, 0, 0), Paint.Style.FILL, true, GameBoard.paint);
            DrawUtil.drawVoidPolygon(points, canvas, Color.BLACK, (float) (SQUARE_SIZE / 3), false);
        }
        drawOutlines(canvas, Color.argb(255, 45, 0, 0));

//        for (Vector<Point> passage : passages){
//            Point[] points = new Point[2];
//            points[0] = passage.get(0);
//            points[1] = passage.get(1);
//            DrawUtil.drawVoidPolygon(points,canvas,Color.GREEN);
//        }
    }

    private void drawOutlines(Canvas canvas, int color) {
        Path path = new Path();
        for (Vector<Integer> outlineIndexes : outlines){
            Point previous = null;
            for (Integer index : outlineIndexes){
                Point point = vertices.get(index);
                // Not along the map's top and bottom borders
                if (point.y == 0 || point.y == getHeight()) {
                    continue;
                }
                // A jump is where a border was skipped, so the outline continues from the new point
                if (previous == null || Math.hypot(point.x - previous.x, point.y - previous.y) > MyActivity.TILE_WIDTH) {
                    path.moveTo(point.x, point.y);
                } else {
                    path.lineTo(point.x, point.y);
                }
                previous = point;
            }
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((float) (SQUARE_SIZE / 4));
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    public void drawNodes (Canvas canvas){
        Paint paint = new Paint();
        int sqWidth = 6;
        int sqWidth2 = 3;
        for (int x = 0 ; x < squareGrid.squares.length ; x++){
            for (int y = 0 ; y < squareGrid.squares[0].length ; y++) {
                drawNode(canvas, squareGrid.squares[x][y].topLeft, sqWidth, paint);
                drawNode(canvas, squareGrid.squares[x][y].topRight, sqWidth, paint);
                drawNode(canvas, squareGrid.squares[x][y].bottomRight, sqWidth, paint);
                drawNode(canvas, squareGrid.squares[x][y].bottomLeft, sqWidth, paint);

                drawNode(canvas, squareGrid.squares[x][y].centreTop, sqWidth2, paint);
                drawNode(canvas, squareGrid.squares[x][y].centreBottom, sqWidth2, paint);
                drawNode(canvas, squareGrid.squares[x][y].centreLeft, sqWidth2, paint);
                drawNode(canvas, squareGrid.squares[x][y].centreRight, sqWidth2, paint);
            }
        }
    }

    private void drawNode(Canvas canvas, ControlNode controlNode, int sqWidth, Paint paint){
        int color = (controlNode.active) ? Color.BLACK : Color.WHITE;
        paint.setColor(color);
        MathVector position = controlNode.position;
        Rect rect = new Rect((int)position.x-sqWidth,(int)position.y-sqWidth,(int)position.x+sqWidth,(int)position.y+sqWidth);
        paint.setAlpha(50);
        canvas.drawRect(rect,paint);
    }

    private void drawNode(Canvas canvas, Node controlNode, int sqWidth, Paint paint){
        int color = Color.GRAY;
        paint.setColor(color);
        MathVector position = controlNode.position;
        Rect rect = new Rect((int)position.x-sqWidth,(int)position.y-sqWidth,(int)position.x+sqWidth,(int)position.y+sqWidth);
        paint.setAlpha(50);
        canvas.drawRect(rect,paint);
    }
}
