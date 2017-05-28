package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 03/08/2016.
 */
public abstract class HUDElement {

    private int xCenter, yCenter;

    public HUDElement(int xCenter, int yCenter) {
        this.xCenter = xCenter;
        this.yCenter = yCenter;
    }

    public boolean pressed(double x, double y) {
        return x < getxCenter()+getWidth()/2 && x > getxCenter()-getWidth()/2 &&
                y < getyCenter()+getHeight()/2 && y > getyCenter()-getHeight()/2;
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

    public abstract void draw(Canvas canvas);
    public abstract int getWidth();
    public abstract int getHeight();
}
