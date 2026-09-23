package com.htss.hookshot.map;


/**
 * Created by Sergio on 25/08/2016.
 */

import android.graphics.Point;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.math.MathVector;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

public class Map {

    // Every tenth level is the deep worm's, in a cavern this many tiles wide and tall from its middle. Switched off for
    // now, while the controls are being tried out: the worm can still be placed from the playground's menu
    private static final boolean BOSS_LEVELS = false;
    static final int BOSS_EVERY = 10, BOSS_CAVERN_X = 22, BOSS_CAVERN_Y = 13;
    private static final int SMOOTH_ITERATIONS = 2,  //5, 4, 3, 5, 5, 3
            WALL_COUNT_SMOOTH_THRESHOLD = 4,
            WALL_COUNT_SIZE_THRESHOLD = 2,
            ROOM_COUNT_SIZE_THRESHOLD = 2;
    static final int BORDER_SIZE = 3, PASSAGE_RADIUS = 3;
    public static final double SQUARE_SIZE = 45 * MyActivity.TILE_WIDTH / 100;
    // How the rock texture is drawn: its pixels per cave square
    public static final int ROCK_TEXELS_PER_SQUARE = 24;
    // Two points of an outline further apart than this aren't joined: a border was skipped between them
    public static final double OUTLINE_JUMP = MyActivity.TILE_WIDTH;

    // What's in the cave is placed from these, by CaveContents
    int[][] map;
    int xTiles, yTiles;
    private int fillPercent, maxSizeForSusceptible;
    // Which level this cave is, and the seed of the game it's in, as several caves exist at once: the one being
    // played, and the next, which is made while the last one still is
    int level;
    long seed;
    // Where a piece's vault was hollowed out and where along its corridor the door that bars it goes, both in tiles,
    // as the piece doesn't know where it is in the world until its objects are placed. Null if the piece has none
    Coord vaultCenter, vaultDoorTile;
    MathVector vaultDoorVector;
    // A piece of the open world, and where among the others
    boolean chunk = false;
    int chunkX, chunkY;
    private long worldSeed;
    // The size of the open world's pieces in tiles, and how much of them starts out as rock
    public static final int CHUNK_X = 48, CHUNK_Y = 32;
    private static final int CHUNK_FILL = 50;
    // Vaults: how often a piece has one, how big the chamber and its corridor are in tiles, how far out that corridor
    // will look for the cave, how far from the piece's own sides the whole thing stays, and how many tries it gets at
    // finding rock to sit in
    private static final double CHUNK_VAULTS = 0.6;
    static final int VAULT_RADIUS = 4, VAULT_CORRIDOR = 2;
    private static final int VAULT_REACH = 8, VAULT_EDGE = BORDER_SIZE + 1, VAULT_TRIES = 300, VAULT_WAYS = 16;
    // Where the cave's corner is in the world, which everything placed in it is placed from
    double originX = 0, originY = 0;
    Coord entrance;
    private Coord exit;
    private CaveMesh mesh;
    Room entranceRoom, exitRoom;
    Vector<Room> roomRegions = new Vector<Room>();
    Vector<Room> susceptibleRooms = new Vector<Room>();
    Vector<Room> roomsWithInterest = new Vector<Room>();
    Vector<Passage> passages = new Vector<Passage>();
    // Where the character starts in the playground, which has no entrance to start from
    private MathVector playgroundStart = null;

    // The kinds of enemy the playground can place
    public static final int SPAWN_STALKER = 0, SPAWN_TERRA_WORM = 1, SPAWN_BAT = 2, SPAWN_SPITTER = 3, SPAWN_SNIPPER = 4,
            SPAWN_BEETLE = 5, SPAWN_DEEP_WORM = 6;
    // The playground's size in tiles
    private static final int PLAYGROUND_X = 100, PLAYGROUND_Y = 56;

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

    boolean isWellInside(int tileX, int tileY, int margin) {
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

    MathVector at(Coord tile) {
        return at(tile.tileX, tile.tileY);
    }

    // A tile's place in the world
    MathVector at(double tileX, double tileY) {
        return new MathVector(originX + tileX * SQUARE_SIZE, originY + tileY * SQUARE_SIZE);
    }

    // Puts what's in the cave into the game: gates and their buttons, enemies, powers and health. From the main thread,
    // once, with the cave's corner at the given place in the world
    public void addObjects(double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
        new CaveContents(this).add();
    }

    // Places an enemy of the given kind a few tiles from a point in the room, where that kind lives. Tells whether there
    // was somewhere to put it
    public boolean spawnNear(int kind, MathVector at, Random random) {
        return new CaveContents(this).spawnNear(kind, at, random);
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

    // On the vault's side of its door, where the character is never put down, as the door would have it shut in
    boolean isBehindVaultDoor(int tileX, int tileY) {
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

    int getSurroundingCount (int gridX, int gridY){
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

    // Once the tiles are done
    private void generateMesh() {
        mesh = new CaveMesh(map);
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

    void addSusceptibleRoom(Room room) {
        if (!susceptibleRooms.contains(room)) {
            susceptibleRooms.add(room);
        } else {
            susceptibleRooms.remove(room);
            susceptibleRooms.add(room);
        }
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

    boolean isInMapRange (int x, int y){
        return x >= 0 && x < xTiles && y >= 0 && y < yTiles;
    }

    public int[][] getMap() {
        return map;
    }

    public int getMapValue(int x, int y){
        return map[x][y];
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


    public Vector<Point> getVertices() {
        return mesh.getVertices();
    }

    public Vector<Integer> getTriangles() {
        return mesh.getTriangles();
    }

    public Vector<Vector<Integer>> getOutlines() {
        return mesh.getOutlines();
    }
}
