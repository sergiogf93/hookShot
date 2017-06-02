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
import com.htss.hookshot.game.object.obstacles.Ball;
import com.htss.hookshot.game.object.collectables.CoinBag;
import com.htss.hookshot.game.object.obstacles.Door;
import com.htss.hookshot.game.object.enemies.EnemyStalker;
import com.htss.hookshot.game.object.obstacles.WallButton;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

public class Map {

    private final int SMOOTH_ITERATIONS = 5,
            WALL_COUNT_SMOOTH_THRESHOLD = 4,
            BORDER_SIZE = 3,
            WALL_COUNT_SIZE_THRESHOLD = 5,
            ROOM_COUNT_SIZE_THRESHOLD = 5,
            PASSAGE_RADIUS = 3,
            MAX_SIZE_FOR_SUSCEPTIBLE = 40;
    public static final double SQUARE_SIZE = 45;

    private int[][] map;
    private int xTiles, yTiles, fillPercent, upCenter, downCenter;
    private long seed;
    private Random random;
    private SquareGrid squareGrid;
    private Vector<Point> vertices;
    private Vector<Integer> triangles;
    private HashMap<Integer,Vector<Triangle>> triangleDictionary = new HashMap<Integer, Vector<Triangle>>();
    private Vector<Vector<Integer>> outlines = new Vector<Vector<Integer>>();
    private HashSet<Integer> checkedVertices = new HashSet<Integer>();
    private Vector<Point[]> cracks = new Vector<Point[]>();
    private Vector<Room> roomRegions = new Vector<Room>();
    private boolean blockPassage = true;
    private Vector<Room> roomAs = new Vector<Room>(), roomBs = new Vector<Room>(), susceptibleRooms = new Vector<Room>();
    private Line passageToBlock;

    private Vector<Line> passages = new Vector<Line>();

    public Map (int xTiles, int yTiles, int fillPercent, boolean useRandomSeed, long seed){
        this.map = new int[xTiles][yTiles];
        this.xTiles = xTiles;
        this.yTiles = yTiles;
        this.fillPercent = fillPercent;
        if (useRandomSeed){
            this.seed = System.currentTimeMillis();
        } else {
            this.seed = seed;
        }
        this.random = new Random();
        this.random.setSeed(this.seed);
        randomFillMap(false,this.fillPercent);
        for (int i=0; i < SMOOTH_ITERATIONS; i++){
            smoothMap();
        }

        manageRoomDownAndUp();

        manageRoomRemovingAndConnection();

        addBallObstacles(1);
//        addCoins();
        addDoorObstacle(1);
//        addEnemies(1);



        generateMesh();

    }

    private void randomFillMap(boolean resetRandom, int fillPercent){
        if (resetRandom) {
            Random pseudoRandom = new Random();
            pseudoRandom.setSeed(this.seed);
            this.random = pseudoRandom;
            for (int i = 0; i < MyActivity.level; i++) {
                for (int x = 0; x < xTiles; x++) {
                    for (int y = 0; y < yTiles; y++) {
                        if (!(x <= BORDER_SIZE || x >= xTiles - BORDER_SIZE - 1 || y <= BORDER_SIZE || y >= yTiles - BORDER_SIZE - 1)) {
                            this.random.nextInt(100);
                        }
                    }
                }
                upCenter = this.random.nextInt(xTiles);
            }
        }
        for (int x = 0; x < xTiles; x++){
            for (int y = 0; y < yTiles; y++){
                if (x <= BORDER_SIZE || x >= xTiles-BORDER_SIZE-1 || y <= BORDER_SIZE || y >= yTiles-BORDER_SIZE-1){
                    map[x][y] = 1;
                } else {
                    map[x][y] = (this.random.nextInt(100) < fillPercent) ? 1 : 0;
                }
            }
        }
        downCenter = this.random.nextInt(xTiles);
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

    private void createCrack(Node center, int type) {
        switch (type) {
            case 15: {
                Point[] crack = new Point[6];
                Point[] crack2 = new Point[3];
                if (random.nextBoolean()) {
                    crack[0] = new Point((int) (center.position.x), (int) (center.position.y - SQUARE_SIZE / 2));
                    crack[1] = new Point((int) (center.position.x - SQUARE_SIZE / 3), (int) (center.position.y - SQUARE_SIZE / 3));
                    crack[2] = new Point((int) (center.position.x - SQUARE_SIZE / 5), (int) (center.position.y - SQUARE_SIZE / 5));
                    crack[3] = new Point((int) (center.position.x + SQUARE_SIZE / 5), (int) (center.position.y + SQUARE_SIZE / 5));
                    crack[4] = new Point((int) (center.position.x + SQUARE_SIZE / 3), (int) (center.position.y + SQUARE_SIZE / 3));
                    crack[5] = new Point((int) (center.position.x), (int) (center.position.y + SQUARE_SIZE / 2));

                    crack2[0] = new Point((int) (center.position.x - SQUARE_SIZE / 2), (int) (center.position.y));
                    crack2[1] = new Point((int) (center.position.x + SQUARE_SIZE / 4), (int) (center.position.y - SQUARE_SIZE / 3));
                    crack2[2] = new Point((int) (center.position.x + SQUARE_SIZE / 2), (int) (center.position.y));
                } else {
                    crack[0] = new Point((int) (center.position.x - SQUARE_SIZE / 2), (int) (center.position.y));
                    crack[1] = new Point((int) (center.position.x - SQUARE_SIZE / 3), (int) (center.position.y + SQUARE_SIZE / 3));
                    crack[2] = new Point((int) (center.position.x - SQUARE_SIZE / 5), (int) (center.position.y + SQUARE_SIZE / 5));
                    crack[3] = new Point((int) (center.position.x + SQUARE_SIZE / 5), (int) (center.position.y - SQUARE_SIZE / 5));
                    crack[4] = new Point((int) (center.position.x + SQUARE_SIZE / 3), (int) (center.position.y - SQUARE_SIZE / 3));
                    crack[5] = new Point((int) (center.position.x + SQUARE_SIZE / 2), (int) (center.position.y));

                    crack2[0] = new Point((int) (center.position.x), (int) (center.position.y - SQUARE_SIZE / 2));
                    crack2[1] = new Point((int) (center.position.x - SQUARE_SIZE / 4), (int) (center.position.y + SQUARE_SIZE / 3));
                    crack2[2] = new Point((int) (center.position.x), (int) (center.position.y + SQUARE_SIZE / 2));
                }

                this.cracks.add(crack);
                this.cracks.add(crack2);
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

    public void manageRoomRemovingAndConnection() {
        passages.clear();
        roomRegions.clear();
        passageToBlock = null;
        roomAs.clear();
        roomBs.clear();
        susceptibleRooms.clear();

        Vector<Vector<Coord>> wallRegions = getRegions(1);

        for (Vector<Coord> wallRegion : wallRegions){
            if (wallRegion.size() < WALL_COUNT_SIZE_THRESHOLD){
                for (Coord tile : wallRegion){
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

        for (Room room : roomRegions){
            if (room.roomSize < MAX_SIZE_FOR_SUSCEPTIBLE && !room.isUpOrDown()){
                susceptibleRooms.add(room);
            }
        }

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

    private void createPassage (Room roomA, Room roomB, Coord tileA, Coord tileB){
        Line line = getLine(tileA, tileB);
        if (blockPassage){
            double angle = line.vector.angleDeg(new MathVector(1,0));
            if ( angle > 80 && angle < 100) {
                if (true) {
                    roomAs.add(roomA);
                    roomAs.addAll(roomA.connectedRooms);
                    roomBs.add(roomB);
                    roomBs.addAll(roomB.connectedRooms);
                    passageToBlock = line;
                    blockPassage = false;
                    if (roomA.roomSize < MAX_SIZE_FOR_SUSCEPTIBLE && !roomA.isUpOrDown()){
                        susceptibleRooms.add(roomA);
                    }
                    if (roomB.roomSize < MAX_SIZE_FOR_SUSCEPTIBLE && !roomB.isUpOrDown()){
                        susceptibleRooms.add(roomB);
                    }
                }
            }
        } else {
            if (roomAs != null && roomAs.size() > 0){
                if (roomAs.contains(roomA)){
                    roomAs.add(roomB);
                } else if (roomAs.contains(roomB)){
                    roomAs.add(roomA);
                }
                if (roomBs.contains(roomA)){
                    roomBs.add(roomB);
                } else if (roomBs.contains(roomB)){
                    roomBs.add(roomA);
                }
            }
        }

        connectRooms(roomA, roomB);
        for (Coord c : line.line) {
            drawCircle(c, PASSAGE_RADIUS);
        }
        this.passages.add(line);
    }

    private Line getLine (Coord from, Coord to){
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

        return new Line(vector,line);
    }

    private void drawCircle (Coord c, int r){
        for (int x = -r ; x <= r ; x++){
            for (int y = -r ; y <= r ; y++) {
                if (x*x + y*y <= r*r){
                    int drawX = c.tileX+x;
                    int drawY = c.tileY+y;
                    if (isInMapRange(drawX,drawY)){
                        if (drawX != 0 && drawX != xTiles-1) {
                            map[drawX][drawY] = 0;
                        }
                    }
                }
            }
        }
    }

    public MathVector startPosition (){
        for (int yTile = 0 ; yTile < yTiles ; yTile++){
            for (int xTile = 0 ; xTile < xTiles ; xTile++){
                if (map[xTile][yTile] == 0){
                    if (MyActivity.level == 0) {
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
        return new MathVector(0,0);
    }

    private void manageRoomDownAndUp(){
        drawCircle(new Coord(downCenter,yTiles-1), (int) (PASSAGE_RADIUS*1.5));
        for (int x = 0 ; x < xTiles ; x++){
            for (int i = 0 ; i < 5 ; i++) {
                map[x][yTiles-1-i] = map[x][yTiles-1];
            }
        }
        if (MyActivity.level > 0){
            drawCircle(new Coord(upCenter,0), (int) (PASSAGE_RADIUS*1.5));
        }
    }

    public Vector<Coord> findPath(Coord start, Coord goal){
        Vector<Coord> path = new Vector<Coord>();

        int counter = 0;
        boolean reachedStart = false;

        LinkedList<Coord> queue = new LinkedList<Coord>();
        HashMap<Coord,Integer> counterMap = new HashMap<Coord, Integer>();
        queue.add(goal);
        counterMap.put(goal,counter);

        while (!reachedStart) {
            counter++;
            Vector<Coord> currentAdjacent = new Vector<Coord>();
            while (queue.size() > 0) {
                Coord current = queue.pop();
                for (int x = current.tileX - 1; x <= current.tileX + 1; x++) {
                    for (int y = current.tileY - 1; y <= current.tileY + 1; y++) {
                        if(x != current.tileX && y != current.tileY) {
                            Coord adjacent = new Coord(x, y);
                            if (x == start.tileX && y == start.tileY){
                                reachedStart = true;
                            }
                            if (map[x][y] == 0) {
                                if (counterMap.containsKey(adjacent)) {
                                    if (counterMap.get(adjacent) > counter) {
                                        counterMap.put(adjacent, counter);
                                    }
                                } else {
                                    counterMap.put(adjacent, counter);
                                    currentAdjacent.add(adjacent);
                                }
                            }
                        }
                    }
                }
            }
            queue.addAll(currentAdjacent);
        }

        path.add(start);
        while (!path.contains(goal)) {
            Coord current = path.lastElement();
            int bestCounter = counterMap.get(current);
            Coord bestCoord = new Coord();
            for (Coord coord : counterMap.keySet()) {
                if (coord.isAdjacent(current)) {
                    if (counterMap.get(coord) < bestCounter) {
                        bestCounter = counterMap.get(coord);
                        bestCoord = coord;
                    }
                }
            }
            path.add(bestCoord);
        }

        return path;
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

    public void extend(int direction){
        blockPassage = true;

        int[] lineToCopy = new int[xTiles];
        int factor = (direction > 0) ? 0 : 1;
        for (int x = 0 ; x < xTiles ; x++){
            lineToCopy[x] = map[x][(1-factor)*(yTiles-1)];
        }

        MyActivity.level += direction;

        upCenter = downCenter;
        randomFillMap(direction < 0,this.fillPercent);

        for (int x = 0 ; x < xTiles ; x++){
            for (int y = 0 ; y < 5 ; y++) {
                map[x][factor*(yTiles-1) + y] = lineToCopy[x];
            }
        }

        for (int i=0; i < SMOOTH_ITERATIONS; i++){
            smoothMap();
        }

        manageRoomDownAndUp();

        manageRoomRemovingAndConnection();

//        addBallObstacles(1);
        addDoorObstacle(direction);
        addEnemies(1);

        generateMesh();
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
                Ball ball = new Ball(xBall * SQUARE_SIZE, yBall * SQUARE_SIZE, 100, 6, (float) (2*SQUARE_SIZE));
                MyActivity.canvas.gameObjects.add(ball);
                MyActivity.dynamicObjects.add(ball);
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

    private void addDoorObstacle(int direction){
        int leftX = 0, rightX = 0;
        double yPos = 0;
        boolean  foundLeft = false;
        for (int x = 1 ; x < xTiles ; x++){
            if (!foundLeft) {
                if (map[x][yTiles - 1] == 0) {
                    leftX = x - 1;
                    foundLeft = true;
                }
            } else {
                if (map[x][yTiles - 1] == 1) {
                    rightX = x;
                    break;
                }
            }
        }
//        for (int y = yTiles - 1 ; y > yTiles - 10 ; y--){
//            if (map[leftX+1][y] == 1 || map[rightX-1][y] == 1){
//                yPos = y*SQUARE_SIZE;
//                break;
//            }
//        }
        yPos = (yTiles-2)*SQUARE_SIZE;
        double xPos = (rightX - leftX)*SQUARE_SIZE/2 + leftX*SQUARE_SIZE;

        addDoor(xPos, yPos, (int) ((rightX - leftX + 1) * SQUARE_SIZE), (int) (1.5*SQUARE_SIZE), 4, false, direction < 0);

        if (passageToBlock != null){
            addDoor(passageToBlock.getCenterInRoom().x, passageToBlock.getCenterInRoom().y, (int) ((PASSAGE_RADIUS + 2) * SQUARE_SIZE * 2), (int) (2*SQUARE_SIZE), 2, true, direction < 0);
        }

    }

    public void addDoor(double xPos, double yPos, int width, int height, int nButtons, boolean forceConnection, boolean buttonsOn){
        Random obstacleRandom = new Random();
        obstacleRandom.setSeed(seed + nButtons + MyActivity.level);
        Vector<WallButton> buttons = new Vector<WallButton>();
        MathVector start = startPosition();
        Coord startCoord = new Coord((int)(start.x/SQUARE_SIZE),(int)(start.y/SQUARE_SIZE));
        Room startRoom = startCoord.getRoom(roomRegions);

        if (forceConnection && startRoom == null){
            return;
        }

        for (int i = 0 ; i < nButtons ; i++){
            MathVector pos = getRandomEmptyPoint(0,obstacleRandom);
            Coord buttonCoord = new Coord((int)(pos.x/SQUARE_SIZE), (int) (pos.y/SQUARE_SIZE));
            if (susceptibleRooms.size() > 0) {
                Room roomToPutButton = susceptibleRooms.lastElement();
                int tileIndex = obstacleRandom.nextInt(roomToPutButton.tiles.size());
                buttonCoord = roomToPutButton.tiles.get(tileIndex);
                int k = 0;
                while (getSurroundingCount(buttonCoord.tileX,buttonCoord.tileY) > 0 && k < 100) {
                    tileIndex = obstacleRandom.nextInt(roomToPutButton.tiles.size());
                    buttonCoord = roomToPutButton.tiles.get(tileIndex);
                    k++;
                }
                susceptibleRooms.remove(roomToPutButton);
            }
            if (forceConnection){
                Room buttonRoom = buttonCoord.getRoom(roomRegions);
                Vector<Room> roomsContainingStartRoom = new Vector<Room>();
                if (roomAs.contains(startRoom)){
                    roomsContainingStartRoom = roomAs;
                } else if (roomBs.contains(startRoom)){
                    roomsContainingStartRoom = roomBs;
                } else {
                    for (Room room : roomAs){
                        if (room.connectedRooms.contains(startRoom)){
                            roomsContainingStartRoom = roomAs;
                            break;
                        }
                    }
                }
                if (roomsContainingStartRoom.size() == 0){
                    for (Room room : roomBs){
                        if (room.connectedRooms.contains(startRoom)){
                            roomsContainingStartRoom = roomBs;
                            break;
                        }
                    }
                }
                if (roomsContainingStartRoom.size() == 0){
                    return;
                }
                if (!roomsContainingStartRoom.contains(buttonRoom)){
                    int roomIndex = obstacleRandom.nextInt(roomsContainingStartRoom.size());
                    int tileIndex = obstacleRandom.nextInt(roomsContainingStartRoom.get(roomIndex).tiles.size());
                    buttonCoord = roomsContainingStartRoom.get(roomIndex).tiles.get(tileIndex);
                    int j = 0;
                    while (getSurroundingCount(buttonCoord.tileX,buttonCoord.tileY) > 0 && j < 100) {
                        roomIndex = obstacleRandom.nextInt(roomsContainingStartRoom.size());
                        tileIndex = obstacleRandom.nextInt(roomsContainingStartRoom.get(roomIndex).tiles.size());
                        buttonCoord = roomsContainingStartRoom.get(roomIndex).tiles.get(tileIndex);
                        j++;
                    }
                    pos = new MathVector(buttonCoord.tileX*SQUARE_SIZE,buttonCoord.tileY*SQUARE_SIZE);
                }
            }
            WallButton button = new WallButton(pos.x,pos.y, (float) (SQUARE_SIZE*0.8),buttonsOn);
            buttons.add(button);
            MyActivity.canvas.gameObjects.add(0,button);
        }

        if (!buttonsOn) {
            Door door = new Door(xPos, yPos, width, height, buttons);
            MyActivity.canvas.gameObjects.add(0,door);
            MyActivity.dynamicObjects.add(0,door);
        }
    }

    public void addEnemies (int N){
        Random enemyRandom = new Random();
        enemyRandom.setSeed(this.seed + MyActivity.level + N);
        for (int i = 0;i < N;i++) {
            MathVector p = getRandomEmptyPoint(0, enemyRandom);
            EnemyStalker stalker = new EnemyStalker(p.x, p.y);
            MyActivity.canvas.gameObjects.add(stalker);
            MyActivity.dynamicObjects.add(stalker);
            MyActivity.enemies.add(stalker);
        }
    }

    public void addCoins(){
        Random coinRandom = new Random();
        coinRandom.setSeed(seed + MyActivity.level);
        if (susceptibleRooms.size() > 0) {
            Room room = susceptibleRooms.lastElement();
            susceptibleRooms.remove(room);
            int tileIndex = coinRandom.nextInt(room.tiles.size());
            Coord tile = room.tiles.get(tileIndex);
            int j = 0;
            while (getSurroundingCount(tile.tileX,tile.tileY) > 0 && j < 100){
                tileIndex = coinRandom.nextInt(room.tiles.size());
                tile = room.tiles.get(tileIndex);
                j++;
            }
            CoinBag coinBag = new CoinBag(tile.tileX*SQUARE_SIZE, tile.tileY*SQUARE_SIZE);
            MyActivity.canvas.gameObjects.add(coinBag);
        } else {
            MathVector pos = getRandomEmptyPoint(0, coinRandom);
            CoinBag coinBag = new CoinBag(pos.x, pos.y);
            MyActivity.canvas.gameObjects.add(coinBag);
        }
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

    public int getWidth() {
        return (int) ((xTiles)*SQUARE_SIZE - SQUARE_SIZE);
    }

    public int getHeight() {
        return (int) ((yTiles)*SQUARE_SIZE - SQUARE_SIZE);
    }

//    public Random getPseudoRandom() {
//        return pseudoRandom;
//    }
//
//    public void setPseudoRandom(Random pseudoRandom) {
//        this.pseudoRandom = pseudoRandom;
//    }

    public class Line {
        public MathVector vector;
        public Vector<Coord> line;

        public Line(MathVector vector, Vector<Coord> line) {
            this.vector = vector.normalized();
            this.line = line;
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

        public boolean isUpOrDown() {
            return tiles.get(0).tileY < 3 || tiles.get(0).tileY > yTiles - 3;
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
        for (Point[] crack : cracks){
            DrawUtil.drawVoidPolygon(crack,canvas,Color.BLACK,MyActivity.TILE_WIDTH /50);
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
            DrawUtil.drawVoidPolygon(points,canvas,Color.BLACK, (float) (SQUARE_SIZE/3));
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
            DrawUtil.drawVoidPolygon(outline.toArray(),canvas,Color.argb(255, 45, 0, 0),(float) (SQUARE_SIZE/4));
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

