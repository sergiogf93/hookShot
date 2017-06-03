package com.htss.hookshot.game.hud;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 03/08/2016.
 */
public class Joystick extends HUDElement implements Clickable {

    private static final double MARGIN = MyActivity.TILE_WIDTH;

    private int xJ, yJ, touchId, touchIndex, width, height, alpha = 99;
    private double xDown, yDown;
    private boolean on, showing, clickable;

    public Joystick(int xCenter, int yCenter, int width, int height) {
        super(xCenter, yCenter);
        this.xJ = 0;
        this.yJ = 0;
        this.width = width;
        this.height = height;
        this.on = false;
        this.touchId = -1;
        this.showing = true;
        this.clickable = true;
    }

    @Override
    public void draw(Canvas canvas){
        drawBase(canvas);
        drawHandle(canvas);
    }

    private void drawBase(Canvas canvas) {
        drawCircle(canvas, getxCenter(), getyCenter(), Color.rgb(30, 30, 30), alpha, getRadius());
        drawCircle(canvas, getxCenter(), getyCenter(), Color.WHITE, alpha, (float) (0.95 * getRadius()));
        Paint p = new Paint();
        p.setColor(Color.YELLOW);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(getxCenter(),getyCenter(),getWidth()/2,p);
    }

    private void drawHandle(Canvas canvas) {
        drawCircle(canvas, getxCenter() + getxJ(), getyCenter() + getyJ(), Color.rgb(30, 30, 30), alpha, getHandleRadius());
        drawCircle(canvas, getxCenter() + getxJ(), getyCenter() + getyJ(), Color.rgb(200, 200, 200), alpha, (float) (0.95*getHandleRadius()));
    }

    public void moveJoystick(double x, double y){
        MathVector vTouch = new MathVector(x-getxCenter(),y-getyCenter());
        int dx, dy;
        double dist = vTouch.magnitude();
        vTouch.normalize();
        if(dist <= getHeight()/3){
            dx = (int) Math.round(vTouch.x*dist);
            dy = (int) Math.round(vTouch.y*dist);
        } else {
            dx = (int) Math.round(vTouch.x*getHeight()/3);
            dy = (int) Math.round(vTouch.y*getHeight()/3);
        }
        setxJ(dx);
        setyJ(dy);
        moveCharacter(getxJ(),getyJ());
    }

    private void moveCharacter(int dx, int dy) {
        MathVector newP;
        if (MyActivity.character.isHooked() && MyActivity.character.getHook().getNodes().size() <= MainCharacter.MIN_HOOSKSHOT_NODES + 2) {
            newP = new MathVector(dx, dy);
        } else {
            newP = new MathVector(dx, 0);
            if (MyActivity.character.isOnFloor()){
                newP.y += dy;
            }
        }
        MyActivity.character.addP(newP);
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setOn(true);
        setTouchId(id);
        setTouchIndex(index);
    }

    @Override
    public void reset() {
        setOn(false);
        setxJ(0);
        setyJ(0);
        setTouchId(-1);
        setTouchIndex(-1);
        MyActivity.character.setP(new MathVector(0, MyActivity.character.getP().y));
    }

    @Override
    public boolean isOn() {
        return on;
    }

    @Override
    public boolean isClickable() {
        return clickable;
    }

    @Override
    public void setClickable(boolean bool) {
        this.clickable = bool;
    }

    public boolean isShowing() {
        return showing;
    }

    public void setShowing(boolean showing) {
        this.showing = showing;
    }

    public int getxJ() {
        return xJ;
    }

    public void setxJ(int xJ) {
        this.xJ = xJ;
    }

    public int getyJ() {
        return yJ;
    }

    public void setyJ(int yJ) {
        this.yJ = yJ;
    }

    public MathVector getJ() {return new MathVector(getxJ(),getyJ());}

    @Override
    public int getWidth(){
        return (int) (width + MARGIN);
    }

    @Override
    public int getHeight(){
        return (int) (height + MARGIN);
    }

    public void setOn(boolean on){
        this.on = on;
    }

    public double getxDown() {
        return xDown;
    }

    public void setxDown(double xDown) {
        this.xDown = xDown;
    }

    public double getyDown() {
        return yDown;
    }

    public void setyDown(double yDown) {
        this.yDown = yDown;
    }

    @Override
    public int getTouchId() {
        return touchId;
    }

    public void setTouchId(int touchId) {
        this.touchId = touchId;
    }

    @Override
    public int getTouchIndex() {
        return touchIndex;
    }

    public void setTouchIndex(int touchIndex) {
        this.touchIndex = touchIndex;
    }

    @Override
    public boolean pressed(double x, double y) {
        return getCenter().distanceTo(new MathVector(x, y)) <= MARGIN + getWidth() / 2;
    }

    public float getRadius() {
        return width/2;
    }

    public float getHandleRadius() {
        return (float) (width*0.3);
    }

}
