package com.htss.hookshot.game.object.enemies;

import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.math.MathVector;

import java.util.Random;

/**
 * Created by Sergio on 31/05/2017.
 */
public abstract class ClickableEnemy extends GameEnemy implements Clickable {

    // Enemies are small and fast, so a near miss still counts as a hit instead of firing the hook
    private static final double TAP_MARGIN = MyActivity.TILE_WIDTH * 0.8;

    private boolean clickable = true, on = false;
    private int touchId = -1, touchIndex = -1;

    public ClickableEnemy(double xPos, double yPos, int mass, int collisionPriority, double maxVelocity, int maxHealth, boolean addToLists, boolean addToEnemyList) {
        super(xPos, yPos, mass, collisionPriority, maxVelocity, maxHealth, addToLists, addToEnemyList);
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setTouchIndex(index);
        setTouchId(id);
        setOn(true);
        hit();
    }

    // Hit by a tap or by the hook
    public void hit() {
        if (canBeHit()) {
            startHitCooldown();
            getHitTarget().getHurt(1);
        }
    }

    // Who loses health when this is hit
    protected GameEnemy getHitTarget() {
        return this;
    }

    @Override
    public boolean pressed(double x, double y) {
        return getPositionInScreen().distanceTo(new MathVector(x, y)) <= getBodyRadius() + TAP_MARGIN;
    }

    public abstract double getBodyRadius();

    @Override
    public void reset() {
        setTouchIndex(-1);
        setTouchId(-1);
        setOn(false);
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

    @Override
    public int getTouchId() {
        return touchId;
    }

    @Override
    public int getTouchIndex() {
        return touchIndex;
    }

    public void setTouchId(int touchId) {
        this.touchId = touchId;
    }

    public void setTouchIndex(int touchIndex) {
        this.touchIndex = touchIndex;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

}
