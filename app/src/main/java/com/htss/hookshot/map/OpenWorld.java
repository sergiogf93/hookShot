package com.htss.hookshot.map;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.interactables.CoinBag;
import com.htss.hookshot.game.object.interactables.HealthDrop;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.obstacles.Door;
import com.htss.hookshot.game.object.obstacles.WallButton;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The open world: one endless cave, made of pieces that come and go around the character. The pieces next to the one
 * the character is in are made in the background, nearest first, and the ones it has left well behind are dropped.
 * A piece is the same whenever it's made, so what the character changed in it is remembered for when it comes back:
 * the holes dug, the powers and health picked up, the buttons pressed, whether its enemies were all beaten, and where
 * its water and lava had flowed, if they had. Those memories are kept in a file, so the world is still as it was left
 * when the game is opened again.
 *
 * Only the main thread touches this. The thread that makes a piece only hands it over when it's done, and the one that
 * writes the file only gets memories that no longer change.
 */
public class OpenWorld {

    // Pieces this close to the character's are made, and ones further than the second are dropped. The gap between them
    // keeps pieces from coming and going as the character moves about the edge of one
    private static final int NEAR = 1, FAR = 2;
    // The file the memories are kept in, and how it's laid out, so an older one is known and left aside
    private static final String FILE = "open_world";
    // Version 2 added the water and lava that had flowed, and version 3 keeps the holes dug in squares rather than
    // pixels, as how many pixels a square is comes from the screen the game was opened on, which on a phone that folds
    // can be another the next time. Older files are still read, their holes taken as dug on a screen like this one
    private static final int FILE_VERSION = 3;

    private static long seed;
    private static int pieceWidth, pieceHeight;
    private static final HashMap<Long, Cave> pieces = new HashMap<Long, Cave>();
    private static final HashMap<Long, Memory> memories = new HashMap<Long, Memory>();
    private static volatile Cave made;
    private static boolean making = false;
    private static int run = 0, currentX, currentY;
    // The world the memories are of. None until they're read from the file
    private static Long remembered = null;
    // Reads and writes the file one after another, in the order they're asked for, away from the main thread
    private static final ExecutorService files = Executors.newSingleThreadExecutor();

    // What's remembered of a piece that was dropped
    private static class Memory {
        // Where each hole was and how big, in squares from the piece's corner
        ArrayList<float[]> digs = new ArrayList<float[]>();
        BitSet taken = new BitSet();
        BitSet pressed = new BitSet();
        boolean cleared = false;
        // Where its water and lava had flowed to, if they had
        FluidGrid.Saved fluid = null;
    }

    // Starts over, with the piece at the given place made right away to stand in
    static Cave start(long worldSeed, boolean forget, double x, double y) {
        seed = worldSeed;
        run++;
        made = null;
        making = false;
        pieces.clear();
        if (forget) {
            memories.clear();
            forgetFile();
        } else if (remembered == null || remembered != worldSeed) {
            memories.clear();
            memories.putAll(readFile(worldSeed));
        }
        remembered = worldSeed;
        pieceWidth = Map.getChunkWidth();
        pieceHeight = Map.getChunkHeight();
        currentX = (int) Math.floor(x / pieceWidth);
        currentY = Math.max(0, (int) Math.floor(y / pieceHeight));
        Cave cave = make(currentX, currentY, seed);
        join(cave);
        return cave;
    }

    // Once every update: takes in the piece that was made, keeps track of which one the character is in, drops the far
    // ones and sees to the next one being made. Gives the piece the character is in
    static Cave update(double x, double y, Cave current) {
        Cave made = OpenWorld.made;
        if (made != null) {
            OpenWorld.made = null;
            making = false;
            Map map = made.map;
            if (!pieces.containsKey(key(map.getChunkX(), map.getChunkY())) && isWithin(map.getChunkX(), map.getChunkY(), FAR)) {
                join(made);
            }
        }
        int pieceX = (int) Math.floor(x / pieceWidth), pieceY = (int) Math.floor(y / pieceHeight);
        // The pieces are made around where the character is, even if its own isn't there, which shouldn't happen but
        // would otherwise leave it in the rock for good
        currentX = pieceX;
        currentY = Math.max(0, pieceY);
        Cave at = pieces.get(key(pieceX, pieceY));
        if (at != null && at != current) {
            current = at;
            MyActivity.currentMap = at.map;
            MyActivity.canvas.myActivity.level = at.level;
            MyActivity.canvas.myActivity.saveOpenWorld();
            if (MyActivity.character.getCompass() != null) {
                MyActivity.character.getCompass().findInterests();
            }
        }
        for (Cave piece : new ArrayList<Cave>(pieces.values())) {
            if (!isWithin(piece.map.getChunkX(), piece.map.getChunkY(), FAR)) {
                drop(piece);
            }
        }
        if (!making) {
            makeNearestMissing();
        }
        return current;
    }

    private static boolean isWithin(int pieceX, int pieceY, int distance) {
        return Math.abs(pieceX - currentX) <= distance && Math.abs(pieceY - currentY) <= distance;
    }

    // The closest piece around the character that isn't there yet, the ones sharing a side with its own first
    private static void makeNearestMissing() {
        int bestX = 0, bestY = 0, best = Integer.MAX_VALUE;
        for (int pieceX = currentX - NEAR; pieceX <= currentX + NEAR; pieceX++) {
            for (int pieceY = Math.max(0, currentY - NEAR); pieceY <= currentY + NEAR; pieceY++) {
                int distance = Math.abs(pieceX - currentX) + Math.abs(pieceY - currentY);
                if (distance < best && !pieces.containsKey(key(pieceX, pieceY))) {
                    best = distance;
                    bestX = pieceX;
                    bestY = pieceY;
                }
            }
        }
        if (best == Integer.MAX_VALUE) {
            return;
        }
        making = true;
        final int pieceX = bestX, pieceY = bestY, thisRun = run;
        final long worldSeed = seed;
        Thread maker = new Thread(new Runnable() {
            @Override
            public void run() {
                Cave cave = make(pieceX, pieceY, worldSeed);
                if (thisRun == run) {
                    made = cave;
                }
            }
        }, "piece maker");
        maker.setPriority(Thread.NORM_PRIORITY - 2);
        maker.start();
    }

    private static Cave make(int pieceX, int pieceY, long worldSeed) {
        return new Cave(Map.chunk(pieceX, pieceY, worldSeed), Math.max(0, pieceY), pieceX * Map.getChunkWidth(), pieceY * Map.getChunkHeight());
    }

    // The piece joins the world with what's in it, but for what's remembered as gone
    private static void join(Cave cave) {
        World.join(cave);
        pieces.put(key(cave.map.getChunkX(), cave.map.getChunkY()), cave);
        Memory memory = memories.get(key(cave.map.getChunkX(), cave.map.getChunkY()));
        if (memory == null) {
            return;
        }
        for (float[] dig : memory.digs) {
            cave.dig((float) (dig[0] * Map.SQUARE_SIZE), (float) (dig[1] * Map.SQUARE_SIZE), (float) (dig[2] * Map.SQUARE_SIZE));
        }
        // After the digs, which the water may have flowed through
        if (memory.fluid != null) {
            cave.loadFluid(memory.fluid);
        }
        ArrayList<Object> pickups = getPickups(cave);
        for (int i = 0; i < pickups.size(); i++) {
            if (memory.taken.get(i)) {
                takeOut(pickups.get(i));
            }
        }
        ArrayList<Object> buttons = getButtons(cave);
        for (int i = 0; i < buttons.size(); i++) {
            if (memory.pressed.get(i)) {
                ((WallButton) buttons.get(i)).setOn(true);
            }
        }
        // The doors those buttons had already opened are open again, rather than opening once more as the piece comes back
        for (Object object : cave.objects) {
            if (object instanceof Door && ((Door) object).isUnlocked()) {
                ((Door) object).openAtOnce();
            }
        }
        if (memory.cleared) {
            for (Object object : cave.objects) {
                if (object instanceof GameEnemy) {
                    takeOut(object);
                }
            }
        }
    }

    private static void drop(Cave cave) {
        memories.put(key(cave.map.getChunkX(), cave.map.getChunkY()), remember(cave));
        pieces.remove(key(cave.map.getChunkX(), cave.map.getChunkY()));
        World.remove(cave);
    }

    // What there is to remember of a piece as it is now
    private static Memory remember(Cave cave) {
        Memory memory = new Memory();
        for (float[] dig : cave.getDigs()) {
            memory.digs.add(inSquares(dig));
        }
        ArrayList<Object> pickups = getPickups(cave);
        for (int i = 0; i < pickups.size(); i++) {
            memory.taken.set(i, !MyActivity.canvas.gameObjects.contains(pickups.get(i)));
        }
        ArrayList<Object> buttons = getButtons(cave);
        for (int i = 0; i < buttons.size(); i++) {
            memory.pressed.set(i, ((WallButton) buttons.get(i)).isOn());
        }
        memory.cleared = true;
        for (Object object : cave.objects) {
            if (object instanceof GameEnemy && MyActivity.enemies.contains(object)) {
                memory.cleared = false;
            }
        }
        memory.fluid = cave.saveFluid();
        return memory;
    }

    // Keeps the memories in the file, with the pieces around the character as they are now
    public static void save() {
        if (remembered == null) {
            return;
        }
        for (Cave piece : pieces.values()) {
            memories.put(key(piece.map.getChunkX(), piece.map.getChunkY()), remember(piece));
        }
        final long worldSeed = remembered;
        final HashMap<Long, Memory> kept = new HashMap<Long, Memory>(memories);
        final File file = getFile();
        files.execute(new Runnable() {
            @Override
            public void run() {
                writeFile(file, worldSeed, kept);
            }
        });
    }

    private static File getFile() {
        return new File(MyActivity.canvas.myActivity.getFilesDir(), FILE);
    }

    // Written aside first and then put in place, so a game closed halfway through leaves the last file whole
    private static void writeFile(File file, long worldSeed, HashMap<Long, Memory> kept) {
        File written = new File(file.getPath() + ".new");
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(written))));
            try {
                out.writeInt(FILE_VERSION);
                out.writeLong(worldSeed);
                out.writeInt(kept.size());
                for (java.util.Map.Entry<Long, Memory> entry : kept.entrySet()) {
                    Memory memory = entry.getValue();
                    out.writeLong(entry.getKey());
                    out.writeInt(memory.digs.size());
                    for (float[] dig : memory.digs) {
                        out.writeFloat(dig[0]);
                        out.writeFloat(dig[1]);
                        out.writeFloat(dig[2]);
                    }
                    writeBits(out, memory.taken);
                    writeBits(out, memory.pressed);
                    out.writeBoolean(memory.cleared);
                    writeFluid(out, memory.fluid);
                }
            } finally {
                out.close();
            }
            if (!written.renameTo(file)) {
                throw new IOException("Couldn't put " + written + " in place");
            }
        } catch (IOException e) {
            e.printStackTrace();
            written.delete();
        }
    }

    // The memories of the given world. None if the file is of another one, or can't be read
    private static HashMap<Long, Memory> readFile(final long worldSeed) {
        final File file = getFile();
        try {
            // After whatever is still being written
            return files.submit(new Callable<HashMap<Long, Memory>>() {
                @Override
                public HashMap<Long, Memory> call() throws IOException {
                    HashMap<Long, Memory> read = new HashMap<Long, Memory>();
                    if (!file.exists()) {
                        return read;
                    }
                    DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
                    try {
                        int version = in.readInt();
                        if (version < 1 || version > FILE_VERSION || in.readLong() != worldSeed) {
                            return read;
                        }
                        for (int count = in.readInt(); count > 0; count--) {
                            long key = in.readLong();
                            Memory memory = new Memory();
                            for (int digs = in.readInt(); digs > 0; digs--) {
                                float[] dig = {in.readFloat(), in.readFloat(), in.readFloat()};
                                // Before version 3 they were in pixels, of a screen that's taken to be like this one
                                memory.digs.add((version < 3) ? inSquares(dig) : dig);
                            }
                            memory.taken = readBits(in);
                            memory.pressed = readBits(in);
                            memory.cleared = in.readBoolean();
                            if (version >= 2) {
                                memory.fluid = readFluid(in);
                            }
                            read.put(key, memory);
                        }
                    } finally {
                        in.close();
                    }
                    return read;
                }
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<Long, Memory>();
        }
    }

    // A new world starts with nothing to remember, once whatever is still being written is done
    private static void forgetFile() {
        final File file = getFile();
        files.execute(new Runnable() {
            @Override
            public void run() {
                file.delete();
            }
        });
    }

    // A hole, from pixels to squares
    private static float[] inSquares(float[] dig) {
        return new float[]{(float) (dig[0] / Map.SQUARE_SIZE), (float) (dig[1] / Map.SQUARE_SIZE), (float) (dig[2] / Map.SQUARE_SIZE)};
    }

    // Whether there's any, the grid's size, and each cell with fluid in it. Cells are half a square, so a piece's grid
    // is as many cells whatever the size of a square, and what's saved fits it on any screen
    private static void writeFluid(DataOutputStream out, FluidGrid.Saved fluid) throws IOException {
        out.writeBoolean(fluid != null);
        if (fluid == null) {
            return;
        }
        out.writeInt(fluid.columns);
        out.writeInt(fluid.rows);
        out.writeInt(fluid.cells.length);
        for (int n = 0; n < fluid.cells.length; n++) {
            out.writeInt(fluid.cells[n]);
            out.writeFloat(fluid.masses[n]);
            out.writeByte(fluid.kinds[n]);
        }
    }

    private static FluidGrid.Saved readFluid(DataInputStream in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        int columns = in.readInt(), rows = in.readInt(), count = in.readInt();
        int[] cells = new int[count];
        float[] masses = new float[count];
        byte[] kinds = new byte[count];
        for (int n = 0; n < count; n++) {
            cells[n] = in.readInt();
            masses[n] = in.readFloat();
            kinds[n] = in.readByte();
        }
        return new FluidGrid.Saved(columns, rows, cells, masses, kinds);
    }

    private static void writeBits(DataOutputStream out, BitSet bits) throws IOException {
        long[] words = bits.toLongArray();
        out.writeInt(words.length);
        for (long word : words) {
            out.writeLong(word);
        }
    }

    private static BitSet readBits(DataInputStream in) throws IOException {
        long[] words = new long[in.readInt()];
        for (int i = 0; i < words.length; i++) {
            words[i] = in.readLong();
        }
        return BitSet.valueOf(words);
    }

    // The powers and health that came with a piece, in the order they were made, which is the same every time, and then
    // its big coins. Those came after, so the ones remembered from before keep their place
    private static ArrayList<Object> getPickups(Cave cave) {
        ArrayList<Object> pickups = new ArrayList<Object>();
        for (Object object : cave.objects) {
            if (object instanceof GamePowerUp || object instanceof HealthDrop) {
                pickups.add(object);
            }
        }
        for (Object object : cave.objects) {
            if (object instanceof CoinBag) {
                pickups.add(object);
            }
        }
        return pickups;
    }

    // The buttons that came with a piece, in the order they were made, which is the same every time
    private static ArrayList<Object> getButtons(Cave cave) {
        ArrayList<Object> buttons = new ArrayList<Object>();
        for (Object object : cave.objects) {
            if (object instanceof WallButton) {
                buttons.add(object);
            }
        }
        return buttons;
    }

    private static void takeOut(Object object) {
        MyActivity.canvas.gameObjects.remove(object);
        MyActivity.dynamicObjects.remove(object);
        MyActivity.enemies.remove(object);
    }

    private static long key(int pieceX, int pieceY) {
        return ((long) pieceX << 32) ^ (pieceY & 0xFFFFFFFFL);
    }
}
