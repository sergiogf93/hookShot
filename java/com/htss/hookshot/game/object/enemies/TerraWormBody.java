package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;

import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;

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
            DrawUtil.drawCircle(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), radius, Color.rgb(120,120,30), Paint.Style.STROKE);
            DrawUtil.drawRadialGradient(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), (float) (radius * 0.95), Color.GRAY, Color.rgb(70, 0, 0), Shader.TileMode.CLAMP);
        } else {
            DrawUtil.drawCircle(canvas, getPaint(), (float) getxPosInScreen(), (float) getyPosInScreen(), radius, Color.RED, Paint.Style.FILL);
        }
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
