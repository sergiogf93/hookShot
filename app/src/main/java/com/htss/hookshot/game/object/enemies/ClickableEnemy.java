package com.htss.hookshot.game.object.enemies;

import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.math.MathVector;

import java.util.Random;

/**
 * Created by Sergio on 31/05/2017.
 */
public abstract class ClickableEnemy extends GameEnemy implements Clickable {

    // Enemies are small and fast, so a near miss still throws the hook at the enemy instead of at the rock behind it
    private static final double TAP_MARGIN = MyActivity.TILE_WIDTH * 0.8;

    private boolean clickable = true, on = false;
    private int touchId = -1, touchIndex = -1;

    public ClickableEnemy(double xPos, double yPos, int mass, int collisionPriority, double maxVelocity, int maxHealth, boolean addToLists, boolean addToEnemyList) {
        super(xPos, yPos, mass, collisionPriority, maxVelocity, maxHealth, addToLists, addToEnemyList);
    }

    @Override
    public void press(double x, double y, int id, int index) {
        // Tapping an enemy throws the hook at it
        MyActivity.strikeAt(this);
    }

    // Hit by the hook, thrown from a point in the room. Every hit knocks it back and hurts it, so none is wasted
    public void hit(MathVector from) {
        knockBack(new MathVector(from, getPositionInRoom()));
        Particles.burst(getxPosInRoom(), getyPosInRoom(), 6, getParticleColor(), 0.06f, 0.03f, 0.35, 0.003f);
        getHitTarget().getHurt(1);
    }

    @Override
    public void die() {
        super.die();
        float size = Math.max(0.03f, (float) getBodyRadius() / MyActivity.TILE_WIDTH * 0.3f);
        Particles.burst(getxPosInRoom(), getyPosInRoom(), 18, getParticleColor(), 0.1f, size, 0.7, 0.004f);
        Particles.burst(getxPosInRoom(), getyPosInRoom(), 6, Color.WHITE, 0.14f, 0.02f, 0.3, 0);
        ScreenShake.shake(0.04f);
    }

    // The colour of the bits flying off when it's hit or dies
    protected int getParticleColor() {
        return Color.rgb(140, 20, 20);
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
