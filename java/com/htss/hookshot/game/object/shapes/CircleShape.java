package com.htss.hookshot.game.object.shapes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by Sergio on 30/07/2016.
 */
public class CircleShape extends GameShape {

    private int radius;

    public CircleShape(double xPos, double yPos, int radius) {
        super(xPos,yPos,Color.YELLOW);
        this.radius = radius;
    }

    @Override
    public boolean intersect(GameShape shape) {
        return distanceTo(shape) <= getRadius() + shape.getIntersectMagnitude();
    }

    @Override
    public int getIntersectMagnitude() {
        return getRadius();
    }

    public CircleShape(double xPos, double yPos, int radius, int color) {
        super(xPos,yPos,color);
        this.radius = radius;
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
