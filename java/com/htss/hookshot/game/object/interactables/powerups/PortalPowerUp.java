package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Created by Sergio on 05/06/2017.
 */
public class PortalPowerUp extends GamePowerUp {

    public PortalPowerUp(double xPos, double yPos, int width, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, width, width*2, GamePowerUp.PORTAL, addToGameObjects, addToDynamicObjects);
        getPaint().setStrokeWidth(getWidth() / 5);
    }

    @Override
    public void draw(Canvas canvas) {
        RectF oval = new RectF((float) getxPosInScreen() - getWidth() / 2, (float) (getyPosInScreen() - getHeight() / 2 + getDy(getFrame(), getHeight() / 4)), (float) getxPosInScreen() + getWidth() / 2, (float) (getyPosInScreen() + getHeight() / 2 + getDy(getFrame(), getHeight() / 4)));
        drawArc(canvas, oval, Color.BLUE, 35, 180);
        drawArc(canvas, oval, Color.RED, 215, 180);
        drawOval(canvas, oval);
    }

    private void drawArc(Canvas canvas, RectF oval, int color, int start, int sweep) {
        getPaint().setColor(color);
        getPaint().setStyle(Paint.Style.STROKE);
        canvas.drawArc(oval, start, sweep, true, getPaint());
    }

    private void drawOval(Canvas canvas, RectF oval) {
        getPaint().setColor(Color.BLACK);
        getPaint().setStyle(Paint.Style.FILL);
        canvas.drawOval(oval, getPaint());
    }
}
