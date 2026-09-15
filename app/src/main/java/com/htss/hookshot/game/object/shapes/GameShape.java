package com.htss.hookshot.game.object.shapes;

import android.graphics.Point;

import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 30/07/2016.
 */
public abstract class GameShape extends GameObject {

    int color;

    public GameShape(double xPos, double yPos, int color, boolean addToGameObjectsList) {
        super(xPos, yPos, addToGameObjectsList);
        this.color = color;
    }

    public abstract boolean intersect(GameShape shape);

    public abstract boolean contains(MathVector p);

    public abstract int getIntersectMagnitude();

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
