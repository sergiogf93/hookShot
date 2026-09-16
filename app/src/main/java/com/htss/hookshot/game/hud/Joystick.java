package com.htss.hookshot.game.hud;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 03/08/2016.
 */
public class Joystick extends HUDElement implements Clickable {

    private static final double MARGIN = MyActivity.TILE_WIDTH;
    // Shares of the handle's reach: where it starts steering, and where it steers fully
    private static final double DEAD_ZONE = 0.2, FULL_STEER = 0.7;

    private int xJ, yJ, touchId, touchIndex, alpha = 99;
    private double xDown, yDown;
    private boolean on, showing, clickable;

    public Joystick(int xCenter, int yCenter, int width, int height) {
        super(xCenter, yCenter, width, height);
        this.xJ = 0;
        this.yJ = 0;
        this.on = false;
        this.touchId = -1;
        this.showing = true;
        this.clickable = true;
        setAlpha(alpha);
    }

    @Override
    public void draw(Canvas canvas){
        drawBase(canvas);
        drawHandle(canvas);
    }

    private void drawBase(Canvas canvas) {
        UiStyle.drawControl(canvas, getPaint(), getxCenter(), getyCenter(), getRadius(), false);
    }

    private void drawHandle(Canvas canvas) {
        // Filled like a pressed button, so it stands out from the base
        UiStyle.drawControl(canvas, getPaint(), getxCenter() + getxJ(), getyCenter() + getyJ(), getHandleRadius(), true);
    }

    // Only moves the handle. The character reads where it is on every update, so how it moves doesn't depend on how
    // often the screen reports the finger
    public void moveJoystick(double x, double y){
        MathVector vTouch = new MathVector(x-getxCenter(),y-getyCenter());
        if (vTouch.magnitude() > getReach()) {
            vTouch.rescale(getReach());
        }
        setxJ((int) Math.round(vTouch.x));
        setyJ((int) Math.round(vTouch.y));
    }

    // The furthest the handle goes from the centre
    private float getReach() {
        return getHeight() / 3f;
    }

    // How far the handle is pushed up or down, from -1 (up) to 1
    public double getPushY() {
        return getyJ() / getReach();
    }

    // How much the handle steers along each axis, from -1 to 1
    public double getSteerX() {
        return steering(getxJ() / getReach());
    }

    public double getSteerY() {
        return steering(getPushY());
    }

    // Near the centre it doesn't steer, so a resting thumb doesn't move the character, and it steers fully a bit
    // before the edge
    private static double steering(double push) {
        double amount = (Math.abs(push) - DEAD_ZONE) / (FULL_STEER - DEAD_ZONE);
        return Math.signum(push) * Math.max(0, Math.min(1, amount));
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setOn(true);
        setTouchId(id);
        setTouchIndex(index);
        moveJoystick(x, y);
    }

    @Override
    public void reset() {
        setOn(false);
        setxJ(0);
        setyJ(0);
        setTouchId(-1);
        setTouchIndex(-1);
        MyActivity.character.releaseJoystick();
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
        return (int) (super.getWidth() + MARGIN);
    }

    @Override
    public int getHeight(){
        return (int) (super.getHeight() + MARGIN);
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
        return super.getWidth()/2;
    }

    public float getHandleRadius() {
        return (float) (super.getWidth()*0.3);
    }

}
