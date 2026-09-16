package com.htss.hookshot.game.object.interactables;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 17/06/2017.
 */
public class HealthDrop extends GameDynamicObject implements Interactable {

    private static final float RADIUS = MyActivity.TILE_WIDTH / 7;
    private static final double HEALTH = 10;
    private Paint paint = new Paint(), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HealthDrop(double xPos, double yPos, boolean addToGameObjectsList, boolean addToDynamicObjectsList) {
        super(xPos, yPos, 0,0,0, addToGameObjectsList, addToDynamicObjectsList);
        paint.setStrokeWidth(RADIUS / 8);
    }

    @Override
    public void draw(Canvas canvas) {
        float pulse = 1 + 0.12f * (float) Math.sin(2 * Math.PI * getFrame() / 60);
        DrawUtil.drawGlow(canvas, glowPaint, (float) getxPosInScreen(), (float) getyPosInScreen(), RADIUS * 2.6f * pulse, Color.argb(150, 120, 255, 120));
        DrawUtil.drawCircle(canvas, paint, (float) getxPosInScreen(), (float) getyPosInScreen(), RADIUS, Color.GREEN, Paint.Style.FILL);
        DrawUtil.drawCircle(canvas, paint, (float) getxPosInScreen(), (float) getyPosInScreen(), RADIUS, Color.RED, Paint.Style.STROKE);
        DrawUtil.drawCircle(canvas, paint, (float) getxPosInScreen() + RADIUS/4, (float) getyPosInScreen() - RADIUS/4, (float) (RADIUS * 0.1), Color.WHITE, Paint.Style.STROKE);
    }

    @Override
    public int getWidth() {
        return (int) (RADIUS * 2);
    }

    @Override
    public int getHeight() {
        return (int) (RADIUS*2);
    }

    @Override
    public void detect() {
        if (distanceTo(MyActivity.character) < MyActivity.TILE_WIDTH /2){
            MyActivity.canvas.gameObjects.remove(this);
            MyActivity.character.addHealth(HEALTH);
            MyActivity.character.checkIfRemoveInterest(this);
            Particles.burst(getxPosInRoom(), getyPosInRoom(), 12, Color.rgb(120, 255, 120), 0.07f, 0.03f, 0.5, 0);
        }
    }
}
