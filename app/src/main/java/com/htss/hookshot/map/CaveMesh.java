package com.htss.hookshot.map;

import android.graphics.Point;

import com.htss.hookshot.math.MathVector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

/**
 * The rock of a cave as a mesh, made from its tiles by marching squares: a corner of a square on every tile's middle,
 * rock or not, and each square filled with the triangles its rock corners call for. It's what the cave's picture is
 * drawn from, and what's solid is worked out from that picture.
 *
 * The outlines are the rock's edges: the triangles' sides that only one triangle has, followed one into the next. The
 * dark line along the rock is drawn along them.
 *
 * It touches nothing but the tiles it's given, so it can be made away from the main thread, with its cave.
 */
public class CaveMesh {

    private final Vector<Point> vertices = new Vector<Point>();
    private final Vector<Integer> triangles = new Vector<Integer>();
    private final Vector<Vector<Integer>> outlines = new Vector<Vector<Integer>>();
    // The triangles each vertex is in, and the vertices already on an outline, or deep in the rock where none can be
    private final HashMap<Integer, Vector<Triangle>> triangleDictionary = new HashMap<Integer, Vector<Triangle>>();
    private final HashSet<Integer> checkedVertices = new HashSet<Integer>();

    // From the tiles, 1 for rock and 0 for open, by column
    public CaveMesh(int[][] map) {
        Square[][] squares = makeSquares(map);
        for (int x = 0; x < squares.length; x++) {
            for (int y = 0; y < squares[0].length; y++) {
                triangulateSquare(squares[x][y]);
            }
        }
        calculateMeshOutlines();
    }

    public Vector<Point> getVertices() {
        return vertices;
    }

    // Each three in a row are a triangle's vertices
    public Vector<Integer> getTriangles() {
        return triangles;
    }

    // Each outline is its vertices in order
    public Vector<Vector<Integer>> getOutlines() {
        return outlines;
    }

    private static Square[][] makeSquares(int[][] map) {
        int nodeCountX = map.length;
        int nodeCountY = map[0].length;

        ControlNode[][] controlNodes = new ControlNode[nodeCountX][nodeCountY];

        for (int x = 0; x < nodeCountX; x++) {
            for (int y = 0; y < nodeCountY; y++) {
                MathVector position = new MathVector(x * Map.SQUARE_SIZE, y * Map.SQUARE_SIZE);
                controlNodes[x][y] = new ControlNode(position, map[x][y] == 1);
            }
        }

        Square[][] squares = new Square[nodeCountX - 1][nodeCountY - 1];

        for (int x = 0; x < nodeCountX - 1; x++) {
            for (int y = 0; y < nodeCountY - 1; y++) {
                squares[x][y] = new Square(controlNodes[x][y + 1], controlNodes[x + 1][y + 1], controlNodes[x + 1][y], controlNodes[x][y]);
            }
        }
        return squares;
    }

    private void triangulateSquare(Square square) {
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
                checkedVertices.add(square.topLeft.vertexIndex);
                checkedVertices.add(square.bottomRight.vertexIndex);
                checkedVertices.add(square.topRight.vertexIndex);
                checkedVertices.add(square.bottomLeft.vertexIndex);
                break;
            }
        }
    }

    private void meshFromPoints(Node... points) {
        assignVertices(points);

        if (points.length >= 3) {
            createTriangle(points[0], points[1], points[2]);
        }
        if (points.length >= 4) {
            createTriangle(points[0], points[2], points[3]);
        }
        if (points.length >= 5) {
            createTriangle(points[0], points[3], points[4]);
        }
        if (points.length >= 6) {
            createTriangle(points[0], points[4], points[5]);
        }
    }

    private void assignVertices(Node[] points) {
        for (int i = 0; i < points.length; i++) {
            if (points[i].vertexIndex == -1) {
                points[i].vertexIndex = vertices.size();
                vertices.add(points[i].position.toPoint());
            }
        }
    }

    private void createTriangle(Node a, Node b, Node c) {
        triangles.add(a.vertexIndex);
        triangles.add(b.vertexIndex);
        triangles.add(c.vertexIndex);

        Triangle triangle = new Triangle(a.vertexIndex, b.vertexIndex, c.vertexIndex);
        addTriangleToDictionary(a.vertexIndex, triangle);
        addTriangleToDictionary(b.vertexIndex, triangle);
        addTriangleToDictionary(c.vertexIndex, triangle);
    }

    private void addTriangleToDictionary(int vertexIndexKey, Triangle triangle) {
        if (triangleDictionary.containsKey(vertexIndexKey)) {
            triangleDictionary.get(vertexIndexKey).add(triangle);
        } else {
            Vector<Triangle> triangleList = new Vector<Triangle>();
            triangleList.add(triangle);
            triangleDictionary.put(vertexIndexKey, triangleList);
        }
    }

    private boolean isOutlineEdge(int vertexA, int vertexB) {
        Vector<Triangle> trianglesContainingA = triangleDictionary.get(vertexA);
        int sharedTriangleCount = 0;

        for (int i = 0; i < trianglesContainingA.size(); i++) {
            if (trianglesContainingA.get(i).contains(vertexB)) {
                sharedTriangleCount++;
                if (sharedTriangleCount > 1) {
                    break;
                }
            }
        }

        return sharedTriangleCount == 1;
    }

    private int getConnectedOutlineVertex(int vertexIndex) {
        Vector<Triangle> trianglesContainingVertex = triangleDictionary.get(vertexIndex);
        for (int i = 0; i < trianglesContainingVertex.size(); i++) {
            Triangle triangle = trianglesContainingVertex.get(i);
            for (int j = 0; j < 3; j++) {
                int vertexB = triangle.get(j);
                if (vertexB != vertexIndex && !checkedVertices.contains(vertexB)) {
                    if (isOutlineEdge(vertexIndex, vertexB)) {
                        return vertexB;
                    }
                }
            }
        }
        return -1;
    }

    private void calculateMeshOutlines() {
        for (int vertexIndex = 0; vertexIndex < vertices.size(); vertexIndex++) {
            if (!checkedVertices.contains(vertexIndex)) {
                int newOutlineVertex = getConnectedOutlineVertex(vertexIndex);
                if (newOutlineVertex != -1) {
                    checkedVertices.add(vertexIndex);
                    Vector<Integer> newOutline = new Vector<Integer>();
                    newOutline.add(vertexIndex);
                    outlines.add(newOutline);
                    followOutline(newOutlineVertex, outlines.size() - 1);
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

    // A square between four tile middles, with the corners that are rock making up its configuration, one bit each
    private static class Square {
        final ControlNode topLeft, topRight, bottomRight, bottomLeft;
        final Node centreTop, centreRight, centreBottom, centreLeft;
        int configuration = 0;

        Square(ControlNode topLeft, ControlNode topRight, ControlNode bottomRight, ControlNode bottomLeft) {
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

    private static class Triangle {
        final int[] vertices;

        Triangle(int vertexIndexA, int vertexIndexB, int vertexIndexC) {
            vertices = new int[3];
            vertices[0] = vertexIndexA;
            vertices[1] = vertexIndexB;
            vertices[2] = vertexIndexC;
        }

        boolean contains(int vertexIndex) {
            return vertexIndex == vertices[0] || vertexIndex == vertices[1] || vertexIndex == vertices[2];
        }

        int get(int i) {
            return vertices[i];
        }
    }

    // A point the mesh can use, which becomes a vertex the first time it is
    private static class Node {
        final MathVector position;
        int vertexIndex = -1;

        Node(MathVector position) {
            this.position = position;
        }
    }

    // A tile's middle, rock or not, with the points half way to the tiles below it and to its right, which the squares
    // around it share
    private static class ControlNode extends Node {
        final boolean active;
        final Node above, right;

        ControlNode(MathVector position, boolean active) {
            super(position);
            this.active = active;
            this.above = new Node(new MathVector(position.x, position.y + Map.SQUARE_SIZE / 2));
            this.right = new Node(new MathVector(position.x + Map.SQUARE_SIZE / 2, position.y));
        }
    }
}
