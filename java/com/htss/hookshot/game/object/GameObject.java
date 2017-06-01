package com.htss.hookshot.game.object;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.game.object.shapes.RectShape;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 28/07/2016.
 */
public abstract class GameObject {

    protected double xPos, yPos;

    public GameObject(double xPos, double yPos) {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public abstract void draw(Canvas canvas);
    public abstract int getWidth();
    public abstract int getHeight();

    public double getxPosInScreen() {
        return xPos + MyActivity.canvas.dx;
    }
    public double getxPosInRoom() {
        return xPos;
    }

    public void setxPosInRoom(int xPos) {
        this.xPos = xPos;
    }

    public double getyPosInRoom() {
        return yPos;
    }
    public double getyPosInScreen() {
        return yPos + MyActivity.canvas.dy;
    }

    public void setyPosInRoom(int yPos) {
        this.yPos = yPos;
    }

    public void setPositionInRoom(MathVector position){
        setxPosInRoom((int) position.x);
        setyPosInRoom((int) position.y);
    }

    public void setPositionInRoom (double x, double y){
        setxPosInRoom((int)x);
        setyPosInRoom((int)y);
    }

    public MathVector getPositionInRoom() {
        return new MathVector(getxPosInRoom(),getyPosInRoom());
    }
    public MathVector getPositionInScreen() {
        return new MathVector(getxPosInScreen(),getyPosInScreen());
    }

    public GameShape getBounds(){
        return new RectShape(getxPosInRoom(),getyPosInRoom(),getWidth(),getHeight(),false);
    }

    public GameShape getBoundsInScreen(){
        return new RectShape(getxPosInScreen(),getyPosInScreen(),getWidth(),getHeight(),false);
    }

    public double distanceTo (GameObject object){
        MathVector v = new MathVector(getPositionInRoom(),object.getPositionInRoom());
        return v.magnitude();
    }

    public double distanceTo (MathVector position){
        MathVector v = new MathVector(getPositionInRoom(),position);
        return v.magnitude();
    }

    public boolean pressed (double xScreen, double yScreen){
        return xScreen >= getxPosInScreen() - getWidth()/2 && xScreen <= getxPosInScreen() + getWidth()/2 &&
                yScreen >= getyPosInScreen() - getHeight()/2 && yScreen <= getyPosInScreen() + getHeight()/2;
    }

    public boolean containsPoint (double xRoom, double yRoom){
        return xRoom > getxPosInRoom() - getWidth()/2 && xRoom < getxPosInRoom() + getWidth()/2 && yRoom > getyPosInRoom() - getHeight()/2 && yRoom < getyPosInRoom() + getHeight()/2;
    }

    public void destroy(){
        MyActivity.canvas.gameObjects.remove(this);
    }

}
