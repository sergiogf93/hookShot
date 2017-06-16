package com.htss.hookshot.map;


/**
 * Created by Sergio on 25/08/2016.
 */

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.enemies.EnemyStalker;
import com.htss.hookshot.game.object.enemies.EnemyTerraWorm;
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
    private static final int SMOOTH_ITERATIONS = 2,  //5, 4, 3, 5, 5, 3
            WALL_COUNT_SMOOTH_THRESHOLD = 4,
            BORDER_SIZE = 3,
            WALL_COUNT_SIZE_THRESHOLD = 2,
            ROOM_COUNT_SIZE_THRESHOLD = 2,
            PASSAGE_RADIUS = 3;
    public static final double SQUARE_SIZE = 45 * MyActivity.TILE_WIDTH / 100;
    private static final int MAX_POWERUPS = 3;
    private static final int MAX_ENEMIES = 2;

    private int[][] map;
    private int xTiles, yTiles, fillPercent, maxSizeForSusceptible;
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

    public Map (int xTiles, int yTiles, int fillPercent, Coord entrance){
        this.map = new int[xTiles][yTiles];
        this.xTiles = xTiles;
        this.yTiles = yTiles;
        this.fillPercent = fillPercent;
        this.maxSizeForSusceptible = (int) ((xTiles*yTiles*(100-fillPercent)/100) * 0.005);

        this.entrance = entrance;

        createMap();

        manageAddingFunctions();

        generateMesh();
    }

    public void extend(){
        int[] lineToCopy = getLineToCopy();

        MyActivity.canvas.myActivity.level += 1;

        entrance = getEntranceFromExit(exit);

        createMap();

        copyLine(lineToCopy, 3);

        manageAddingFunctions();

        generateMesh();
    }

    private void manageAddingFunctions() {
        Random addingRandom = new Random();
        addingRandom.setSeed(MyActivity.canvas.myActivity.seed * MyActivity.canvas.myActivity.level);
        susceptibleRooms.remove(entranceRoom);
        susceptibleRooms.remove(exitRoom);
        if (roomRegions.size() > 2) {
            roomRegions.remove(entranceRoom);
            roomRegions.remove(exitRoom);
        }
        if (MyActivity.canvas.myActivity.level > 0) {
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
        }
        addPowerUps(addingRandom);
    }

    private void createMap() {
        Random random = new Random();
        random.setSeed(MyActivity.canvas.myActivity.seed + MyActivity.canvas.myActivity.level);
        randomFillMap(this.fillPercent, random);

        for (int i=0; i < SMOOTH_ITERATIONS; i++){
            smoothMap();
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

    private void followOutline(int vertexIndex, int outlineIndex) {
        outlines.get(outlineIndex).add(vertexIndex);
        checkedVertices.add(vertexIndex);
        int nextVertexIndex = getConnectedOutlineVertex(vertexIndex);
        if (nextVertexIndex != -1){
            followOutline(nextVertexIndex, outlineIndex);
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
        if (MyActivity.canvas.myActivity.level > 0) {
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
                    if (isInMapRange(drawX,drawY)){
                        map[drawX][drawY] = 0;
                    }
                }
            }
        }
    }

    public MathVector startPosition () {
        if (MyActivity.canvas.myActivity.level == 0) {
            for (int yTile = 0; yTile < yTiles; yTile++) {
                for (int xTile = 0; xTile < xTiles; xTile++) {
                    if (map[xTile][yTile] == 0) {
                        if (MyActivity.canvas.myActivity.level == 0) {
                            if (getSurroundingCount(xTile, yTile) == 0) {
                                if (isInMapRange(xTile, yTile + 2)) {
                                    if (map[xTile][yTile + 2] == 1) {
                                        return new MathVector((xTile) * SQUARE_SIZE, (yTile) * SQUARE_SIZE);
                                    }
                                }
                            }
                        } else {
                            return new MathVector((xTile) * SQUARE_SIZE, (yTile) * SQUARE_SIZE);
                        }
                    }
                }
            }
        } else {
            return new MathVector(getEntrance().tileX * SQUARE_SIZE, getEntrance().tileY * SQUARE_SIZE);
        }
        return new MathVector(0, 0);
    }

    private void manageEntranceAndExit(Random random) {
        // Decide where the exit will be
        boolean exitOnSides = random.nextBoolean();
        if (MyActivity.canvas.myActivity.level == 0) {
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
        if (MyActivity.canvas.myActivity.level > 0) {
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
        return new MathVector(coord.tileX * SQUARE_SIZE, coord.tileY * SQUARE_SIZE);
    }

    public MathVector getRandomPointInRoom(Room room, int maxWallCount, Random r) {
        Coord coord;
        int n = 0;
        do {
            coord = room.tiles.get(r.nextInt(room.roomSize));
            n++;
        } while ((map[coord.tileX][coord.tileY] == 1 || getSurroundingCount(coord.tileX,coord.tileY) > maxWallCount || isUpOrDown(coord)) && n < 1000);
        return new MathVector(coord.tileX * SQUARE_SIZE, coord.tileY * SQUARE_SIZE);
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
        return new MathVector(x*SQUARE_SIZE,y*SQUARE_SIZE);
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
                Ball ball = new Ball(xBall * SQUARE_SIZE, yBall * SQUARE_SIZE, 100, 6, (float) (2*SQUARE_SIZE), true);
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

    private void addExitDoor(Random random, int maxButtons) {
        int nButtons = getNButtons(random, maxButtons);
//        Calculate the width for the door
        MathVector vector = new MathVector(1, 0);
        int start = -1;
        int end = yTiles;
        if (getExit().tileX == 0 || getExit().tileX == xTiles - 1) {
            vector = new MathVector(0, 1);
            for (int y = 0; y < yTiles; y++) {
                if (start == -1) {
                    if (map[getExit().tileX][y] == 0) {
                        start = y;
                    }
                } else {
                    if (map[getExit().tileX][y] == 1) {
                        end = y;
                        break;
                    }
                }
            }
        } else {
            for (int x = 0; x < xTiles; x++) {
                if (start == -1) {
                    if (map[x][yTiles - 1] == 0) {
                        start = x;
                    }
                } else {
                    if (map[x][yTiles - 1] == 1) {
                        end = x;
                        break;
                    }
                }
            }
        }

//      Set the WallButtons
        Vector<WallButton> buttons = createWallButtons(roomRegions, nButtons, random, true);
        addDoor(getExit().tileX * SQUARE_SIZE, getExit().tileY * SQUARE_SIZE, (int) ((end - start + 2) * SQUARE_SIZE), (int) (1.5 * SQUARE_SIZE), vector, buttons);
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
        obstacleRandom.setSeed(MyActivity.canvas.myActivity.seed + nButtons + MyActivity.canvas.myActivity.level);

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
        int N = getNPowerUps(random);
        for (int i = 0; i < N; i++) {
            MathVector position;
            if (susceptibleRooms.size() > 0) {
                position = getPositionFromSusceptibleRooms(random);
            } else {
                position = getRandomPointInRooms(roomRegions, 0, random);
            }
            int powerUpType = random.nextInt(4);
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
    }

    private int getNPowerUps(Random random) {
        return random.nextInt(MAX_POWERUPS);
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

    public void addEnemies (Random random) {
        if (random.nextBoolean()) {
            int N = getNEnemies(random);
            for (int i = 0; i < N; i++) {
                MathVector p = getRandomEmptyPoint(0, random);
                new EnemyStalker(p.x, p.y, true);
            }
        } else {
            MathVector p = getRandomEmptyPoint(0, random);
            new EnemyTerraWorm(p.x, p.y, 5, true, true);
        }
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
            Coord coord = line.get(line.size()/2);
            return new MathVector(coord.tileX*SQUARE_SIZE,coord.tileY*SQUARE_SIZE);
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


    public void draw(Canvas canvas){
        for (int i=0 ; i < triangles.size() ; i+=3){
            Point[] points = new Point[3];
            points[0] = vertices.get(triangles.get(i));
            points[1] = vertices.get(triangles.get(i+1));
            points[2] = vertices.get(triangles.get(i+2));
            DrawUtil.drawPolygon(points, canvas, Color.argb(255, 120, 0, 0));
        }
        for (Point[] crack : cracks) {
            DrawUtil.drawVoidPolygon(crack, canvas, Color.BLACK, MyActivity.TILE_WIDTH / 50, false);
        }
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
            DrawUtil.drawPolygon(points, canvas, Color.argb(255, 60, 0, 0));
            DrawUtil.drawVoidPolygon(points, canvas, Color.BLACK, (float) (SQUARE_SIZE / 3), false);
        }
        drawOutlines(canvas);

//        for (Vector<Point> passage : passages){
//            Point[] points = new Point[2];
//            points[0] = passage.get(0);
//            points[1] = passage.get(1);
//            DrawUtil.drawVoidPolygon(points,canvas,Color.GREEN);
//        }
    }

    public void drawOutlines(Canvas canvas) {
        Vector<Point> outline = new Vector<Point>();
        for (Vector<Integer> outlineIndexes : outlines){
            outline.clear();
            for (Integer index : outlineIndexes){
                if (vertices.get(index).y != 0 && vertices.get(index).y != getHeight()){
                    outline.add(vertices.get(index));
                }
            }
            DrawUtil.drawVoidPolygon(outline.toArray(), canvas, Color.argb(255, 45, 0, 0), (float) (SQUARE_SIZE / 4), false);
        }
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

