package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 06/06/2017.
 */
public class CompassPowerUp extends GamePowerUp {

    public CompassPowerUp(double xPos, double yPos, int width, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, width, width, GamePowerUp.COMPASS, addToGameObjects, addToDynamicObjects);
        getPaint().setStrokeWidth(getWidth() / 5);
    }

    @Override
    public void draw(Canvas canvas) {
        DrawUtil.drawCircle(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), getWidth() / 2, Color.YELLOW, Paint.Style.STROKE);
        DrawUtil.drawCircle(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), getWidth() / 2, Color.rgb(200,200,200), Paint.Style.FILL);
        drawNeedles(canvas);
    }

    private void drawNeedles(Canvas canvas) {
        Point[] points = new Point[4];
        MathVector vector = getVector();
        points[0] = vector.rescaled(getWidth() / 5).applyTo(getPositionInScreen()).toPoint();
        points[1] = vector.getNormal().rescaled(getWidth() / 5).applyTo(getPositionInScreen()).toPoint();
        points[2] = vector.rescaled(getWidth() / 2).applyTo(getPositionInScreen()).toPoint();
        points[3] = vector.getNormal().rescaled(-1 * getWidth() / 5).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, Color.RED);
        points[2] = vector.rescaled(-1 * getWidth() / 2).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, Color.BLUE);
    }

    private MathVector getVector() {
        double angle = (Math.PI/4)*Math.sin(2*Math.PI*getFrame()/ TimeUtil.convertSecondToGameSecond(1));
        return new MathVector(Math.cos(angle), Math.sin(angle));
    }
}
