package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.interactables.Loot;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

import java.util.Random;

/**
 * Created by Sergio on 31/05/2017.
 */
public abstract class ClickableEnemy extends GameEnemy implements Clickable {

    // Enemies are small and fast, so a near miss still throws the hook at the enemy instead of at the rock behind it
    private static final double TAP_MARGIN = MyActivity.TILE_WIDTH * 0.8;

    // A hit that hurts it lights it up and shoves it away from the hit for this long, fading out
    private static final int HURT_FLASH = (int) TimeUtil.secondsToUpdates(0.2);
    private static final double HURT_JOLT = MyActivity.TILE_WIDTH * 0.12;
    private static final int FLASH_LIGHT = Color.rgb(255, 110, 90);

    private boolean clickable = true, on = false, dead = false;
    private int touchId = -1, touchIndex = -1;
    private int hurtFrame = Integer.MIN_VALUE / 2;
    private MathVector jolt = new MathVector(0, 0);
    private boolean flashLayer = false;
    private final Paint flashPaint = new Paint();
    private final RectF flashBounds = new RectF();

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
        MathVector away = new MathVector(from, getPositionInRoom());
        knockBack(away);
        Particles.burst(getxPosInRoom(), getyPosInRoom(), 6, getParticleColor(), 0.06f, 0.03f, 0.35, 0.003f);
        hurtFrame = getFrame();
        jolt = away.isNull() ? new MathVector(0, 0) : away.rescaled(HURT_JOLT);
        getHitTarget().getHurt(1);
    }

    // 1 right after a hit that hurt it, fading to 0
    protected float getHurtFlash() {
        int since = getFrame() - hurtFrame;
        return (since < 0 || since >= HURT_FLASH) ? 0 : 1 - since / (float) HURT_FLASH;
    }

    // What's drawn from here until endHurtFlash lights up white and red, and is shoved away from the hit, for a moment
    // after a hit that hurt it
    protected void beginHurtFlash(Canvas canvas) {
        float flash = getHurtFlash();
        canvas.save();
        flashLayer = flash > 0;
        if (flashLayer) {
            canvas.translate((float) (jolt.x * flash), (float) (jolt.y * flash));
            flashPaint.setColorFilter(new LightingColorFilter(Color.WHITE, DrawUtil.blend(Color.BLACK, FLASH_LIGHT, 0.7f * flash)));
            float x = (float) getxPosInScreen(), y = (float) getyPosInScreen(), reach = (float) getBodyRadius() * 3;
            flashBounds.set(x - reach, y - reach, x + reach, y + reach);
            canvas.saveLayer(flashBounds, flashPaint);
        }
    }

    protected void endHurtFlash(Canvas canvas) {
        if (flashLayer) {
            canvas.restore();
        }
        canvas.restore();
    }

    // A hit that can't hurt it bounces off in sparks, and doesn't knock it back
    protected void deflect() {
        Particles.burst(getxPosInRoom(), getyPosInRoom(), 8, Color.rgb(255, 236, 190), 0.08f, 0.02f, 0.25, 0.002f);
    }

    // Bursts, leaving its loot behind. Only once, even if it's hit again as it dies, like by a bomb and the hook at once
    @Override
    public void die() {
        if (dead) {
            return;
        }
        dead = true;
        super.die();
        Loot.drop(getxPosInRoom(), getyPosInRoom(), getCoinsDropped(), getHealthDropChance(), getPowerUpDropChance());
        float size = Math.max(0.03f, (float) getBodyRadius() / MyActivity.TILE_WIDTH * 0.3f);
        Particles.burst(getxPosInRoom(), getyPosInRoom(), 18, getParticleColor(), 0.1f, size, 0.7, 0.004f);
        Particles.burst(getxPosInRoom(), getyPosInRoom(), 6, Color.WHITE, 0.14f, 0.02f, 0.3, 0);
        ScreenShake.shake(0.04f);
    }

    // What it leaves when it dies: some coins, and with these chances from 0 to 1, a health drop and a power-up
    protected int getCoinsDropped() {
        return 1;
    }

    protected double getHealthDropChance() {
        return 0.1;
    }

    protected double getPowerUpDropChance() {
        return 0.03;
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
