package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;
import android.graphics.Color;

import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 08/06/2017.
 */
public class InfiniteJumpsPowerUp extends GamePowerUp {
    public InfiniteJumpsPowerUp(double xPos, double yPos, int width, int height, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, width, height, INFINITE_JUMPS, addToGameObjects, addToDynamicObjects);
        getPaint().setStrokeWidth(width / 8);
    }

    @Override
    public void draw(Canvas canvas) {
        DrawUtil.drawArc(canvas, getPaint(), (float) getxPosInScreen() - getWidth() / 2, (float) getyPosInScreen() - getHeight() / 4, (float) getxPosInScreen() + getWidth() / 2, (float) getyPosInScreen() + getHeight() / 4, Color.CYAN, -45, 270);
        DrawUtil.drawArc(canvas, getPaint(), (float) (getxPosInScreen() - getWidth() / 2.8), (float) getyPosInScreen(), getWidth() / 3, Color.CYAN, -80, 80);
        DrawUtil.drawArc(canvas, getPaint(), (float) (getxPosInScreen() + getWidth() / 2.8), (float) getyPosInScreen(), getWidth() / 3, Color.CYAN, 180, 80);
    }
}
