package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

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

    public void drawBounds(Canvas canvas) {
        setColor(Color.YELLOW);
        setStyle(Paint.Style.STROKE);
        Rect r = new Rect(getxCenter() - getWidth() / 2, getyCenter() - getHeight() / 2, getxCenter() + getWidth() / 2, getyCenter() + getHeight() / 2);
        canvas.drawRect(r,getPaint());
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

    public void setColor(int color) {
        this.paint.setColor(color);
    }

    public void setAlpha(int alpha) {
        this.paint.setAlpha(alpha);
    }

    public void setStyle(Paint.Style style) {
        this.paint.setStyle(style);
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
