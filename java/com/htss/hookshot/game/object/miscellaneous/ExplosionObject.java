package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 07/06/2017.
 */
public class ExplosionObject extends GameDynamicObject {

    private static double DURATION = TimeUtil.convertSecondToGameSecond(0.1);

    private float maxRadius;
    private Paint paint = new Paint();

    public ExplosionObject(double xPos, double yPos, float maxRadius, boolean addToGameObjectsList, boolean addToDynamicObjectsList) {
        super(xPos, yPos, 0, 0, 0, addToGameObjectsList, addToDynamicObjectsList);
        this.maxRadius = maxRadius;
        setGhost(true);
    }

    @Override
    public void update() {
        updateFrame();
        if (getFrame() > DURATION) {
            this.destroy();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        paint.setAlpha((int) GameMath.linealValue(0,255,DURATION,50,getFrame()));
        DrawUtil.drawRadialGradient(canvas, paint, (float) getxPosInScreen(), (float) getyPosInScreen(), getRadius(), Color.YELLOW, Color.RED, Shader.TileMode.MIRROR);
        MyActivity.canvas.clearCircle(MyActivity.canvas.mapBitmap, (float) getxPosInRoom(), (float) getyPosInRoom(), getRadius());
    }

    public float getRadius() {
        double radius = GameMath.linealValue(0, 0, DURATION, maxRadius, getFrame());
        double radiusDown = (MyActivity.screenHeight - getyPosInScreen()) * 0.8;
        double radiusRight = (MyActivity.screenWidth - getxPosInScreen()) * 0.8;
        double radiusLeft = getxPosInScreen() * 0.8;
        return (float) Math.min(Math.min(Math.min(radius, radiusDown), radiusLeft), radiusRight);
    }

    @Override
    public int getWidth() {
        return (int) getRadius();
    }

    @Override
    public int getHeight() {
        return (int) getRadius();
    }
}
