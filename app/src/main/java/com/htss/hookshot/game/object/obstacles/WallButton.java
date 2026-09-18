package com.htss.hookshot.game.object.obstacles;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 03/09/2016.
 */
public class WallButton extends GameDynamicObject implements Interactable{

    private static final int STEEL_LIGHT = Color.rgb(170, 176, 188), STEEL_DARK = Color.rgb(58, 62, 72),
            EDGE = Color.rgb(28, 30, 36), RECESS = Color.rgb(24, 26, 32), RIVET = Color.rgb(196, 202, 214),
            LIGHT_OFF = Color.rgb(255, 70, 50), LIGHT_ON = Color.rgb(120, 255, 100);

    private float radius;
    private boolean on;
    private Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG), paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient ringShader;
    private Matrix ringMatrix = new Matrix();

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

    // A steel ring bolted to the rock around a light, red and pulsing until the character touches it, like the lights
    // on the doors it opens
    @Override
    public void draw(Canvas canvas) {
        float x = (float) getxPosInScreen(), y = (float) getyPosInScreen(), r = getRadius();
        float pulse = 0.55f + 0.45f * (float) Math.sin(getFrame() * 0.15);
        int light = isOn() ? LIGHT_ON : LIGHT_OFF;
        DrawUtil.drawGlow(canvas, glowPaint, x, y, r * 1.7f, DrawUtil.withAlpha(light, isOn() ? 140 : (int) (120 * pulse)));
        if (ringShader == null) {
            ringShader = new LinearGradient(0, -r, 0, r, STEEL_LIGHT, STEEL_DARK, Shader.TileMode.CLAMP);
        }
        ringMatrix.setTranslate(x, y);
        ringShader.setLocalMatrix(ringMatrix);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setShader(ringShader);
        canvas.drawCircle(x, y, r, paint);
        paint.setShader(null);
        paint.setColor(RECESS);
        canvas.drawCircle(x, y, r * 0.62f, paint);
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI / 4 + i * Math.PI / 2;
            float rx = x + (float) Math.cos(angle) * r * 0.81f, ry = y + (float) Math.sin(angle) * r * 0.81f;
            paint.setColor(EDGE);
            canvas.drawCircle(rx, ry, r * 0.09f, paint);
            paint.setColor(RIVET);
            canvas.drawCircle(rx - r * 0.02f, ry - r * 0.02f, r * 0.055f, paint);
        }
        paint.setColor(isOn() ? light : DrawUtil.blend(Color.rgb(90, 20, 14), light, pulse));
        canvas.drawCircle(x, y, r * 0.32f, paint);
        paint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawCircle(x - r * 0.11f, y - r * 0.11f, r * 0.1f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(EDGE);
        paint.setStrokeWidth(r / 10f);
        canvas.drawCircle(x, y, r, paint);
        paint.setStrokeWidth(r / 14f);
        canvas.drawCircle(x, y, r * 0.62f, paint);
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
