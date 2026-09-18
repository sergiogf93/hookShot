package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;
import android.graphics.Color;

import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 06/06/2017.
 */
public class CompassPowerUp extends GamePowerUp {

    // A brass pocket compass with a ring on top, its needle swinging from side to side
    private static final int DARK_BRASS = Color.rgb(58, 42, 16), BRASS = Color.rgb(201, 154, 60),
            BRASS_LIGHT = Color.rgb(240, 205, 122), FACE = Color.rgb(244, 236, 216), TICK = Color.rgb(138, 106, 42),
            NORTH = Color.rgb(224, 58, 58), SOUTH = Color.rgb(58, 110, 224);
    private static final double SWING_PERIOD = TimeUtil.secondsToUpdates(1.667);

    public CompassPowerUp(double xPos, double yPos, int width, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, width, width, GamePowerUp.COMPASS, addToGameObjects, addToDynamicObjects);
    }

    @Override
    public void draw(Canvas canvas) {
        drawGlow(canvas);
        // Its case as wide as the old compass with its rim
        beginIcon(canvas, getWidth() * 1.2f * 100 / 66f, 50, 0);
        strokeCircle(canvas, 50, 16, 6, DARK_BRASS, 7);
        strokeCircle(canvas, 50, 16, 6, BRASS, 3);
        fillCircle(canvas, 50, 54, 33, DARK_BRASS);
        fillCircle(canvas, 50, 54, 30, BRASS);
        iconPath.reset();
        iconPath.moveTo(26, 44);
        iconPath.quadTo(32, 28, 48, 25);
        strokePath(canvas, iconPath, BRASS_LIGHT, 4);
        fillCircle(canvas, 50, 54, 22, FACE);
        strokeCircle(canvas, 50, 54, 22, TICK, 2);
        iconPath.reset();
        iconPath.moveTo(50, 34);
        iconPath.lineTo(50, 38);
        iconPath.moveTo(50, 70);
        iconPath.lineTo(50, 74);
        iconPath.moveTo(30, 54);
        iconPath.lineTo(34, 54);
        iconPath.moveTo(66, 54);
        iconPath.lineTo(70, 54);
        strokePath(canvas, iconPath, TICK, 2);
        canvas.save();
        canvas.rotate(45 * (float) Math.sin(2 * Math.PI * getFrame() / SWING_PERIOD), 50, 54);
        fillPath(canvas, polygon(50, 36, 55, 54, 45, 54), NORTH);
        fillPath(canvas, polygon(50, 72, 55, 54, 45, 54), SOUTH);
        canvas.restore();
        fillCircle(canvas, 50, 54, 3, DARK_BRASS);
        canvas.restore();
    }

}
