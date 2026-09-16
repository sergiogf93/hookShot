package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.enemies.ClickableEnemy;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

import java.util.ArrayList;

/**
 * Created by Sergio on 07/06/2017.
 */
public class ExplosionObject extends GameDynamicObject {

    private static double DURATION = TimeUtil.secondsToUpdates(0.083);
    // A whole stalker or worm segment
    private static final int DAMAGE = 5;

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
            MyActivity.canvas.clearCircle(MyActivity.canvas.mapBitmap, (float) getxPosInRoom(), (float) getyPosInRoom(), getRadius());
            hurtEnemies();
            burst();
        }
    }

    // Fire and bits of rock, and a strong shake
    private void burst() {
        float x = (float) getxPosInRoom(), y = (float) getyPosInRoom();
        Particles.burst(x, y, 14, Color.rgb(255, 210, 70), 0.14f, 0.05f, 0.45, 0.002f);
        Particles.burst(x, y, 10, Color.rgb(255, 100, 30), 0.12f, 0.05f, 0.4, 0.002f);
        Particles.burst(x, y, 16, CavePalette.forLevel(MyActivity.canvas.myActivity.level).rock, 0.11f, 0.05f, 0.8, 0.006f);
        ScreenShake.shake(0.12f);
    }

    private void hurtEnemies() {
        // Copied, as enemies leave the list when they die
        for (GameEnemy enemy : new ArrayList<GameEnemy>(MyActivity.enemies)) {
            if (enemy instanceof ClickableEnemy) {
                ClickableEnemy target = (ClickableEnemy) enemy;
                if (distanceTo(target) < getRadius() + target.getBodyRadius()) {
                    target.getHurt(DAMAGE);
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        paint.setAlpha((int) GameMath.linealValue(0,255,DURATION,50,getFrame()));
        DrawUtil.drawRadialGradient(canvas, paint, (float) getxPosInScreen(), (float) getyPosInScreen(), getRadius(), Color.YELLOW, Color.RED, Shader.TileMode.MIRROR);
//        MyActivity.canvas.clearCircle(MyActivity.canvas.mapBitmap, (float) getxPosInRoom(), (float) getyPosInRoom(), getRadius());
    }

    public float getRadius() {
        double radius = GameMath.linealValue(0, 1, DURATION, maxRadius, getFrame());
        double radiusDown = (MyActivity.screenHeight - getyPosInScreen()) * 0.8;
        double radiusRight = (MyActivity.screenWidth - getxPosInScreen()) * 0.8;
        double radiusLeft = getxPosInScreen() * 0.8;
        double r =  Math.min(Math.min(Math.min(radius, radiusDown), radiusLeft), radiusRight);
        return (float) Math.max(r, 1);
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
