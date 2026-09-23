package com.htss.hookshot.map;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.enemies.EnemyBat;
import com.htss.hookshot.game.object.enemies.EnemyBeetle;
import com.htss.hookshot.game.object.enemies.EnemyDeepWorm;
import com.htss.hookshot.game.object.enemies.EnemySnipper;
import com.htss.hookshot.game.object.enemies.EnemySpitter;
import com.htss.hookshot.game.object.enemies.EnemyStalker;
import com.htss.hookshot.game.object.enemies.EnemyTerraWorm;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.interactables.CoinBag;
import com.htss.hookshot.game.object.interactables.HealthDrop;
import com.htss.hookshot.game.object.interactables.Shop;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;
import com.htss.hookshot.game.object.obstacles.Door;
import com.htss.hookshot.game.object.obstacles.WallButton;
import com.htss.hookshot.map.Map.Passage;
import com.htss.hookshot.map.Map.Room;
import com.htss.hookshot.math.MathVector;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import static com.htss.hookshot.map.Map.BORDER_SIZE;
import static com.htss.hookshot.map.Map.BOSS_CAVERN_X;
import static com.htss.hookshot.map.Map.BOSS_CAVERN_Y;
import static com.htss.hookshot.map.Map.BOSS_EVERY;
import static com.htss.hookshot.map.Map.PASSAGE_RADIUS;
import static com.htss.hookshot.map.Map.SPAWN_BAT;
import static com.htss.hookshot.map.Map.SPAWN_BEETLE;
import static com.htss.hookshot.map.Map.SPAWN_DEEP_WORM;
import static com.htss.hookshot.map.Map.SPAWN_SNIPPER;
import static com.htss.hookshot.map.Map.SPAWN_SPITTER;
import static com.htss.hookshot.map.Map.SPAWN_STALKER;
import static com.htss.hookshot.map.Map.SPAWN_TERRA_WORM;
import static com.htss.hookshot.map.Map.SQUARE_SIZE;
import static com.htss.hookshot.map.Map.VAULT_CORRIDOR;
import static com.htss.hookshot.map.Map.VAULT_RADIUS;
import static com.htss.hookshot.map.Map.isBossLevel;

/**
 * What's in a cave, put into the game once it's made: gates and the buttons that open them, enemies, powers, health,
 * the vault's riches and the odd stall. Everything is placed from the cave's own seed, so the same cave always gets the
 * same things, made in the same order. The open world tells one pickup from another by that order, to remember which
 * were taken, so what's placed here, and the random numbers it takes, must stay in the order they are.
 *
 * It works from its cave's tiles and rooms, and uses up the rooms it places things in, so there's one for every time
 * the cave's things are placed. Only the main thread places things, as that puts them in the game.
 */
class CaveContents {

    private static final int MAX_BUTTONS = 7, MIN_BUTTONS = 2;
    // The levels new enemies start appearing at, how many groups of enemies a level can have, and how many tiles away
    // from the entrance new kinds of enemy are placed
    private static final int BAT_LEVEL = 3, SPITTER_LEVEL = 6, SNIPPER_LEVEL = 8, BEETLE_LEVEL = 12, MAX_ENEMY_GROUPS = 3,
            SAFE_FROM_ENTRANCE = 18;
    private static final int MAX_POWERUPS = 3;
    private static final int MAX_ENEMIES = 2;
    private static final int MAX_HEALTH = 2;
    // Large odd number (the golden ratio in 64 bits), so consecutive levels get very different seeds
    private static final long LEVEL_SEED_SPREAD = 0x9E3779B97F4A7C15L;
    // How often a piece has a stall, and every how many caves one comes in a game of caves one after another, and how
    // many open tiles it needs over the floor it stands on
    private static final double CHUNK_SHOPS = 0.2;
    private static final int SHOP_EVERY = 3, SHOP_HEADROOM = 3;
    // How many of the worms that would come with the pieces are left out
    private static final double CHUNK_WORMS_DROPPED = 0.6;
    // How many buttons open a vault and how many powers are kept inside
    private static final int VAULT_BUTTONS = 2, VAULT_POWERS = 2;
    // Seals: how many rows apart the barred rows are, and how many buttons break one
    private static final int SEAL_EVERY = 10, SEAL_BUTTONS = 3;
    // How far from the character, in tiles, the playground places enemies
    private static final int SPAWN_NEAREST = 6, SPAWN_FURTHEST = 14;

    private final Map cave;
    // The cave's, the rooms being the very same ones, as placing things uses them up
    private final int[][] map;
    private final int xTiles, yTiles, level, chunkX, chunkY;
    private final long seed;
    private final double originX, originY;
    private final boolean chunk;
    private final Coord entrance, vaultCenter, vaultDoorTile;
    private final MathVector vaultDoorVector;
    private final Room entranceRoom, exitRoom;
    private final Vector<Room> roomRegions, susceptibleRooms, roomsWithInterest;
    private final Vector<Passage> passages;

    CaveContents(Map cave) {
        this.cave = cave;
        map = cave.map;
        xTiles = cave.xTiles;
        yTiles = cave.yTiles;
        level = cave.level;
        seed = cave.seed;
        originX = cave.originX;
        originY = cave.originY;
        chunk = cave.chunk;
        chunkX = cave.chunkX;
        chunkY = cave.chunkY;
        entrance = cave.entrance;
        vaultCenter = cave.vaultCenter;
        vaultDoorTile = cave.vaultDoorTile;
        vaultDoorVector = cave.vaultDoorVector;
        entranceRoom = cave.entranceRoom;
        exitRoom = cave.exitRoom;
        roomRegions = cave.roomRegions;
        susceptibleRooms = cave.susceptibleRooms;
        roomsWithInterest = cave.roomsWithInterest;
        passages = cave.passages;
    }

    // Places an enemy of the given kind a few tiles from a point in the room, where that kind lives: bats on the
    // ceiling, spitters on a wall, beetles on the floor. Tells whether there was somewhere to put it
    boolean spawnNear(int kind, MathVector at, Random random) {
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

    // Puts what's in the cave into the game: gates and their buttons, enemies, powers and health. From the main thread,
    // once, with the cave's corner at the given place in the world
    void add() {
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
        // With a random of its own, so the rest of what's in the cave stays as it was before there were stalls
        if (level % SHOP_EVERY == SHOP_EVERY - 1 && !isBossLevel(level)) {
            addShop(new Random(seed * 53 + level * LEVEL_SEED_SPREAD + 11));
        }
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
        // With a random of its own, so the rest of what's in the piece stays as it was before there were stalls
        Random shopRandom = new Random(seed * 13 + 101);
        if (!start && shopRandom.nextDouble() < CHUNK_SHOPS) {
            addShop(shopRandom);
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

    // Every so many rows down, the way on down is barred. Every piece in the row is, so there's no falling past one
    // further along: a whole band has to be worked over before it can be left behind
    private boolean isSealRow() {
        return chunkY > 0 && chunkY % SEAL_EVERY == SEAL_EVERY - 1;
    }

    // The seal is on the piece's way down, with its buttons spread over the piece's caves
    private void addSeal(Random random) {
        addExitGate(createWallButtons(roomRegions, SEAL_BUTTONS, random, false), null);
    }

    // A stall on a stretch of floor in one of the rooms, flat and wide enough for it, with room above it, and inside the
    // cave's borders. None if there's nowhere like that
    private void addShop(Random random) {
        Vector<Coord> spots = new Vector<Coord>();
        for (Room room : roomRegions) {
            for (Coord tile : room.tiles) {
                if (isShopSpot(tile.tileX, tile.tileY)) {
                    spots.add(tile);
                }
            }
        }
        if (spots.isEmpty()) {
            return;
        }
        Coord spot = spots.get(random.nextInt(spots.size()));
        MathVector at = at(spot.tileX, spot.tileY);
        // The floor is half a tile under the last open tile
        new Shop(at.x, at.y + SQUARE_SIZE / 2);
    }

    private boolean isShopSpot(int x, int y) {
        if (!isWellInside(x, y, BORDER_SIZE + 1) || isBehindVaultDoor(x, y)) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            if (map[x + dx][y + 1] != 1) {
                return false;
            }
            for (int dy = 0; dy <= SHOP_HEADROOM; dy++) {
                if (map[x + dx][y - dy] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private MathVector getRandomPointInRooms(Vector<Room> rooms, int maxWallCount, Random r) {
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

    private MathVector getRandomPointInRoom(Room room, int maxWallCount, Random r) {
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

    private MathVector getRandomEmptyPoint(int wallCount, Random r){
        int x,y;
        int n = 0;
        do {
            x = r.nextInt(xTiles);
            y = r.nextInt(yTiles);
            n++;
        } while ((map[x][y] == 1 || getSurroundingCount(x,y) != wallCount) && n < 1000);
        return at(x, y);
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

    private void addDoor(double xPos, double yPos, int width, int height, MathVector vector, Vector<WallButton> buttons) {
        new Door(xPos, yPos, width, height, vector, buttons, true);
    }

    private void addPassageDoor(int nButtons) {
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

    private Vector<Room> getRoomsConnectedWithException(Room room, Room exceptionRoom) {
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

    private Vector<WallButton> createWallButtons(Vector<Room> rooms, int nButtons, Random random, boolean useSusceptibleRooms) {
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

    private void addPowerUps(Random random) {
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

    private void addHealth(Random random) {
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

    // The deep worm, and no other enemies. The exit stays shut until it's beaten
    private void addBoss() {
        EnemyDeepWorm worm = new EnemyDeepWorm(at(xTiles / 2, yTiles / 2).x, at(xTiles / 2, yTiles / 2).y,
                Math.max(BOSS_CAVERN_X, BOSS_CAVERN_Y) * SQUARE_SIZE * 1.4);
        addExitGate(new Vector<WallButton>(), worm);
    }

    // More groups of enemies come as the cave gets deeper, and new kinds of enemy join them
    private void addEnemies (Random random) {
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

    // A tile's place in the world
    private MathVector at(Coord tile) {
        return cave.at(tile);
    }

    private MathVector at(double tileX, double tileY) {
        return cave.at(tileX, tileY);
    }

    private boolean isInMapRange(int x, int y) {
        return cave.isInMapRange(x, y);
    }

    private int getSurroundingCount(int gridX, int gridY) {
        return cave.getSurroundingCount(gridX, gridY);
    }

    private boolean isWellInside(int tileX, int tileY, int margin) {
        return cave.isWellInside(tileX, tileY, margin);
    }

    private boolean isBehindVaultDoor(int tileX, int tileY) {
        return cave.isBehindVaultDoor(tileX, tileY);
    }

    private void addSusceptibleRoom(Room room) {
        cave.addSusceptibleRoom(room);
    }

    private Coord getExit() {
        return cave.getExit();
    }
}
