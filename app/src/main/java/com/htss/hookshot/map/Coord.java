package com.htss.hookshot.map;

import android.graphics.Point;

import com.htss.hookshot.math.GameMath;

import java.util.Vector;

/**
 * Created by Sergio on 26/08/2016.
 */
public class Coord {
    public int tileX;
    public int tileY;

    public Coord(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    }

    public Coord() {

    }

    public boolean isAdjacent(Coord other){
        if (tileX == other.tileX && tileY == other.tileY){
            return false;
        }
        return GameMath.ins(tileX - 1, other.tileX, tileX + 1) && GameMath.ins(tileY-1,other.tileY,tileY+1);
    }

    public Point toRoomPoint(){
        return new Point((int)Map.SQUARE_SIZE*tileX, (int) (Map.SQUARE_SIZE*tileY));
    }

    public Map.Room getRoom (Vector<Map.Room> rooms){
        for (Map.Room room : rooms){
            for (Coord tile : room.tiles){
                if (tile.tileX == this.tileX && tile.tileY == this.tileY){
                    return room;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return tileX + " " + tileY;
    }
}
