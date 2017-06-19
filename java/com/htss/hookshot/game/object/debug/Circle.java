package com.htss.hookshot.game.object.debug;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 28/07/2016.
 */
public class Circle extends GameDynamicObject {

    private int radius, color;

    public Circle(double xPos, double yPos, int mass, int collisionPriority, int radius, int color, boolean addToLists) {
        super(xPos, yPos, mass, collisionPriority, 500, addToLists, addToLists);
        this.radius = radius;
        this.color = color;
    }

    @Override
    protected boolean checkCollisionWithOtherObjects(double x, double y) {
        if (getCollisionPriority() != 0) {
            for (GameDynamicObject dynamicObject : MyActivity.dynamicObjects) {
                GameShape bounds = dynamicObject.getBounds();
                MathVector point = new MathVector(x,y);
                if (!dynamicObject.equals(this) && dynamicObject.distanceTo(point) < bounds.getIntersectMagnitude() && !dynamicObject.isGhost() && !(dynamicObject instanceof Circle) && !(dynamicObject instanceof MainCharacter)) {
                    if (bounds.contains(point)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
    public GameShape getBounds() {
        return new CircleShape(getxPosInRoom(), getyPosInRoom(), getRadius(), false);
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
