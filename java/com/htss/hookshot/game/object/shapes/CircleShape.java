package com.htss.hookshot.game.object.shapes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 30/07/2016.
 */
public class CircleShape extends GameShape {

    private int radius;

    public CircleShape(double xPos, double yPos, int radius, boolean addToGameObjects) {
        super(xPos, yPos, Color.YELLOW, addToGameObjects);
        this.radius = radius;
    }

    public CircleShape(double xPos, double yPos, int radius, int color, boolean addToGameObjects) {
        super(xPos, yPos, color, addToGameObjects);
        this.radius = radius;
    }

    @Override
    public boolean intersect(GameShape shape) {
        return distanceTo(shape) <= getRadius() + shape.getIntersectMagnitude();
    }

    @Override
    public boolean contains(MathVector p) {
        return distanceTo(p) <= getRadius();
    }

    @Override
    public int getIntersectMagnitude() {
        return getRadius();
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
//        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle((float)getxPosInScreen(),(float)getyPosInScreen(),radius,paint);
    }

    @Override
    public int getWidth() {
        return getRadius()/2;
    }

    @Override
    public int getHeight() {
        return getRadius()/2;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

}
