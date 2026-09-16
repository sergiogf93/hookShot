package com.htss.hookshot.game.object.obstacles;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.interfaces.Interactable;

/**
 * Created by Sergio on 03/09/2016.
 */
public class WallButton extends GameDynamicObject implements Interactable{

    private float radius;
    private boolean on;

    public WallButton(double xPos, double yPos, float radius, boolean on, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, 0, 0, 0, addToGameObjects, addToDynamicObjects);
        this.radius = radius;
        this.on = on;
    }

    @Override
    public void detect() {
        if (!isOn()){
            if (distanceTo(MyActivity.character) < getRadius()*1.5){
                setOn(true);
                MyActivity.character.checkIfRemoveInterest(this);
                Particles.burst(getxPosInRoom(), getyPosInRoom(), 10, Color.rgb(140, 255, 100), 0.05f, 0.025f, 0.4, 0);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.argb(255,200,200,0));
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), getRadius(), paint);
        if (isOn()){
            paint.setColor(Color.GREEN);
        } else {
            paint.setColor(Color.RED);
        }
        canvas.drawCircle((float)getxPosInScreen(), (float) getyPosInScreen(),getRadius()/3,paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getRadius() / 50);
        paint.setColor(Color.argb(255, 100, 100, 100));
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), getRadius(), paint);
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), 2*getRadius()/3, paint);
        canvas.drawCircle((float)getxPosInScreen(), (float) getyPosInScreen(),getRadius()/3,paint);
    }

    @Override
    public int getWidth() {
        return (int) radius;
    }

    @Override
    public int getHeight() {
        return (int) radius;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }
}
