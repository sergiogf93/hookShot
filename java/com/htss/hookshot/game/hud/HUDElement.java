package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.game.object.shapes.RectShape;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 03/08/2016.
 */
public abstract class HUDElement {

    private int xCenter, yCenter, width, height;
    private Paint paint = new Paint();

    public HUDElement(int xCenter, int yCenter, int width, int height) {
        this.xCenter = xCenter;
        this.yCenter = yCenter;
        this.width = width;
        this.height = height;
    }

    public boolean pressed(double x, double y) {
        return x < getxCenter()+getWidth()/2 && x > getxCenter()-getWidth()/2 &&
                y < getyCenter()+getHeight()/2 && y > getyCenter()-getHeight()/2;
    }

    public GameShape getBounds(){
        return new RectShape(getxCenter(),getyCenter(),getWidth(),getHeight(),false);
    }

    public void drawCircle(Canvas canvas, float cx, float cy, int color, int alpha, float radius) {
        paint.setColor(color);
        paint.setAlpha(alpha);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    public int getxCenter() {
        return xCenter;
    }

    public void setxCenter(int xCenter) {
        this.xCenter = xCenter;
    }

    public int getyCenter() {
        return yCenter;
    }

    public void setyCenter(int yCenter) {
        this.yCenter = yCenter;
    }

    public void setCenter (MathVector center){
        setxCenter((int) center.x);
        setyCenter((int) center.y);
    }

    public MathVector getCenter (){
        return new MathVector(getxCenter(),getyCenter());
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public abstract void draw(Canvas canvas);
}
