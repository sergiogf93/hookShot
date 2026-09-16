package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 05/06/2017.
 */
public abstract class GamePowerUp extends GameDynamicObject implements Interactable {

    public static final int PORTAL = 0, COMPASS = 1, BOMB = 2, INFINITE_JUMPS = 3;

    private int type;
    private int width, height;
    private Paint paint = new Paint(), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Items in the cave glow, their icons in the HUD and the pause menu don't
    private boolean glowing;

    public GamePowerUp(double xPos, double yPos, int width, int height, int type, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, 0, 0, 0, addToGameObjects, addToDynamicObjects);
        this.width = width;
        this.height = height;
        this.type = type;
        this.glowing = addToGameObjects;
    }

    // Pulsing behind the item, so it stands out in the cave
    protected void drawGlow(Canvas canvas) {
        if (glowing) {
            float pulse = 1 + 0.12f * (float) Math.sin(2 * Math.PI * getFrame() / 60);
            DrawUtil.drawGlow(canvas, glowPaint, (float) getxPosInScreen(), (float) getyPosInScreen(), Math.max(getWidth(), getHeight()) * 1.1f * pulse, getGlowColor());
        }
    }

    private int getGlowColor() {
        switch (type) {
            case PORTAL:
                return Color.argb(150, 200, 120, 255);
            case COMPASS:
                return Color.argb(150, 255, 240, 120);
            case BOMB:
                return Color.argb(150, 255, 150, 60);
            default:
                return Color.argb(150, 120, 230, 255);
        }
    }

    public double getDy(int frame, float maxDy) {
        return maxDy*Math.sin(2*Math.PI*frame/50);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void detect() {
        if (distanceTo(MyActivity.character) < MyActivity.TILE_WIDTH /2){
            MyActivity.canvas.gameObjects.remove(this);
            MyActivity.character.addPowerUp(this.type);
            MyActivity.character.checkIfRemoveInterest(this);
            Particles.burst(getxPosInRoom(), getyPosInRoom(), 12, Color.rgb(170, 255, 255), 0.07f, 0.03f, 0.5, 0);
        }
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
