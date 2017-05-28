package com.htss.hookshot.game.object.shapes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;

/**
 * Created by Sergio on 03/08/2016.
 */
public class RectShape extends GameShape{

    private int width, height;

    public RectShape(double xPos, double yPos, int width, int height) {
        super(xPos, yPos, Color.BLACK);
        this.width = width;
        this.height = height;
    }

    public RectShape(double xPos, double yPos, int width, int height, int color) {
        super(xPos, yPos, color);
        this.width = width;
        this.height = height;
    }

    public RectShape (Rect r, int color){
        super(r.centerX(),r.centerY(),color);
        this.width = r.width();
        this.height = r.height();
    }

    @Override
    public int getIntersectMagnitude() {
        return Math.min(getWidth(), getHeight())/2;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
//        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect((float) getxPosInScreen() - getWidth() / 2, (float) getyPosInScreen() - getHeight() / 2, (float) getxPosInScreen() + getWidth() / 2, (float) getyPosInScreen() + getHeight() / 2, paint);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }


    @Override
    public boolean intersect(GameShape shape) {
        Rect r1 = new Rect((int)getxPosInRoom()-getWidth()/2,(int)getyPosInRoom()-getHeight()/2,(int)getxPosInRoom()+getWidth()/2,(int)getyPosInRoom()+getHeight()/2);
        Rect r2;
        if (shape instanceof CircleShape){
            r2 = new Rect((int) shape.getxPosInRoom() - ((CircleShape) shape).getRadius(), (int) shape.getyPosInRoom() - ((CircleShape) shape).getRadius(), (int) shape.getxPosInRoom() + ((CircleShape) shape).getRadius(), (int) shape.getyPosInRoom() + ((CircleShape) shape).getRadius());
        } else {
            r2 = new Rect((int) shape.getxPosInRoom() - shape.getWidth() / 2, (int) shape.getyPosInRoom() - shape.getHeight() / 2, (int) shape.getxPosInRoom() + shape.getWidth() / 2, (int) shape.getyPosInRoom() + shape.getHeight() / 2);
        }
        return r1.intersect(r2);
    }
}
