package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 16/06/2017.
 */
public class TerraWormBody extends ClickableEnemy {

    private static final int COLLISION_PRIORITY = 0, MASS = 0, MAX_HEALTH = 5;

    private float radius;
    private EnemyTerraWorm terraWorm;

    public TerraWormBody(double xPos, double yPos, float radius, EnemyTerraWorm terraWorm, boolean addToLists, boolean addToEnemyList) {
        super(xPos, yPos, MASS, COLLISION_PRIORITY, 0, MAX_HEALTH, addToLists, addToEnemyList);
        this.terraWorm = terraWorm;
        this.radius = radius;
        getPaint().setStrokeWidth(radius / 7);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!isOn()){
            DrawUtil.drawCircle(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), radius, Color.rgb(120,100,30), Paint.Style.STROKE);
            DrawUtil.drawRadialGradient(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), (float) (radius * 0.95), Color.rgb(10,10,10), Color.rgb(70, 0, 0), Shader.TileMode.CLAMP);
        } else {
            DrawUtil.drawCircle(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), radius, Color.RED, Paint.Style.FILL);
        }
        if (this == terraWorm.getBodyParts().lastElement()){
            drawFangs(canvas);
        } else {
            drawLegs(canvas);
        }
    }

    private void drawLegs(Canvas canvas) {
        Point[] points = new Point[3];
        points[0] = getP().getNormal().rotatedDeg(10).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        points[1] = getP().getNormal().rescaled(getRadius() * 1.3).applyTo(getPositionInScreen()).toPoint();
        points[2] = getP().getNormal().rotatedDeg(-10).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, Color.rgb(120, 100, 30), Paint.Style.FILL, false, getPaint());
        points[0] = getP().getNormal().rotatedDeg(10).rescaled(-getRadius()).applyTo(getPositionInScreen()).toPoint();
        points[1] = getP().getNormal().rescaled(-getRadius() * 1.3).applyTo(getPositionInScreen()).toPoint();
        points[2] = getP().getNormal().rotatedDeg(-10).rescaled(-getRadius()).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, Color.rgb(120, 100, 30), Paint.Style.FILL, false, getPaint());
    }

    private void drawFangs(Canvas canvas) {
        Point[] points = new Point[5];
        int angle = (terraWorm.getFrame() % TimeUtil.convertSecondToGameSecond(0.5) < TimeUtil.convertSecondToGameSecond(0.25)) ? 35 : 40;
        points[0] = getP().rotatedDeg(angle).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        points[1] = getP().rotatedDeg(angle - 5).rescaled(getRadius() * 1.5).applyTo(getPositionInScreen()).toPoint();
        points[2] = getP().rotatedDeg(angle - 10).rescaled(getRadius() * 1.9).applyTo(getPositionInScreen()).toPoint();
        points[3] = getP().rotatedDeg(angle - 15).rescaled(getRadius() * 1.2).applyTo(getPositionInScreen()).toPoint();
        points[4] = getP().rotatedDeg(angle - 20).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, Color.rgb(120, 100, 30), Paint.Style.FILL, false, getPaint());
        points[0] = getP().rotatedDeg(-angle).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        points[1] = getP().rotatedDeg(-angle + 5).rescaled(getRadius() * 1.5).applyTo(getPositionInScreen()).toPoint();
        points[2] = getP().rotatedDeg(-angle + 10).rescaled(getRadius() * 1.9).applyTo(getPositionInScreen()).toPoint();
        points[3] = getP().rotatedDeg(-angle + 15).rescaled(getRadius() * 1.2).applyTo(getPositionInScreen()).toPoint();
        points[4] = getP().rotatedDeg(-angle + 20).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, Color.rgb(120, 100, 30), Paint.Style.FILL, false, getPaint());
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setTouchIndex(index);
        setTouchId(id);
        setOn(true);
        terraWorm.getBodyParts().firstElement().getHurt(1);
    }

    @Override
    public void die() {
        terraWorm.removeBodyPart(this);
        super.die();
    }

    @Override
    public double getHurtDistance() {
        return radius * 1.5;
    }

    @Override
    public int getDamageDone() {
        return 1;
    }

    @Override
    public int getWidth() {
        return (int) (radius * 2);
    }

    @Override
    public int getHeight() {
        return (int) (radius * 2);
    }

    public float getRadius() {
        return radius;
    }
}
