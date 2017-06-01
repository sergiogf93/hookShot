package com.htss.hookshot.game.object.debug;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.game.object.shapes.GameShape;

/**
 * Created by Sergio on 28/07/2016.
 */
public class Circle extends GameDynamicObject {

    private int radius, color;

    public Circle(double xPos, double yPos, int mass, int collisionPriority, int radius, int color) {
        super(xPos, yPos, mass, collisionPriority, 500);
        this.radius = radius;
        this.color = color;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(getColor());
        canvas.drawCircle((float)getxPosInScreen(),(float)getyPosInScreen(),getRadius(),paint);
    }

    @Override
    public int getWidth() {
        return 2*getRadius();
    }

    @Override
    public int getHeight() {
        return 2*getRadius();
    }

    @Override
    public GameShape getBounds(){
        return new CircleShape(getxPosInRoom(),getyPosInRoom(),getRadius());
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
