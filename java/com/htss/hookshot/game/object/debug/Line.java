package com.htss.hookshot.game.object.debug;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 30/07/2016.
 */
public class Line extends GameObject {

    private double endX, endY;

    public Line(double xPos, double yPos, double endX, double endY) {
        super(xPos, yPos);
        this.endX = endX;
        this.endY = endY;
    }

    public Line(MathVector point1, MathVector point2) {
        super(point1.x, point1.y);
        this.endX = point2.x;
        this.endY = point2.y;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawLine((int)getxPosInScreen(),(int)getyPosInScreen(),(int)endX,(int)endY,paint);
    }

    @Override
    public int getWidth() {
        return (int) (getEndX()-getxPosInRoom());
    }

    @Override
    public int getHeight() {
        return (int) (getEndY()-getyPosInRoom());
    }

    public double getEndX() {
        return endX;
    }

    public void setEndX(double endX) {
        this.endX = endX;
    }

    public double getEndY() {
        return endY;
    }

    public void setEndY(double endY) {
        this.endY = endY;
    }
}
