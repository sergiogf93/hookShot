package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 07/06/2017.
 */
public class BombPowerUp extends GamePowerUp {

    public BombPowerUp(double xPos, double yPos, int width, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, width, width, GamePowerUp.BOMB, addToGameObjects, addToDynamicObjects);
        getPaint().setStrokeWidth(getWidth() / 2);
    }

    @Override
    public void draw(Canvas canvas) {
        DrawUtil.drawRadialGradient(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), (float) getWidth() / 2, Color.YELLOW, Color.RED, Shader.TileMode.MIRROR);
        DrawUtil.drawCircle(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), (float) getWidth() / 2, Color.rgb(50,30,0), Paint.Style.STROKE);
    }
}
