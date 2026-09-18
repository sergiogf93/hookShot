package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.enemies.ClickableEnemy;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

import java.util.ArrayList;

/**
 * Created by Sergio on 07/06/2017.
 */
public class ExplosionObject extends GameDynamicObject {

    // The burst grows for this long, then blasts: it digs the rock and hurts enemies. It keeps growing and fades for a
    // while after, with a shockwave spreading out
    private static double DURATION = TimeUtil.secondsToUpdates(0.083), FADE = TimeUtil.secondsToUpdates(0.35);
    // A whole stalker or worm segment
    private static final int DAMAGE = 5;
    private static final int SHOCKWAVE = Color.rgb(255, 214, 140);

    private float maxRadius, blastRadius;
    private boolean blasted = false;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final BurstArt art = new BurstArt();

    public ExplosionObject(double xPos, double yPos, float maxRadius, boolean addToGameObjectsList, boolean addToDynamicObjectsList) {
        super(xPos, yPos, 0, 0, 0, addToGameObjectsList, addToDynamicObjectsList);
        this.maxRadius = maxRadius;
        setGhost(true);
    }

    @Override
    public void update() {
        updateFrame();
        if (!blasted && getFrame() > DURATION) {
            blasted = true;
            blastRadius = getRadius();
            MyActivity.canvas.clearCircle(MyActivity.canvas.mapBitmap, (float) getxPosInRoom(), (float) getyPosInRoom(), blastRadius);
            hurtEnemies();
            burst();
        }
        if (getFrame() > DURATION + FADE) {
            this.destroy();
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
                if (distanceTo(target) < blastRadius + target.getBodyRadius()) {
                    target.knockBack(new MathVector(getPositionInRoom(), target.getPositionInRoom()));
                    target.getHurt(DAMAGE);
                }
            }
        }
    }

    // The burst of the bomb power-up's icon, spinning as it grows
    @Override
    public void draw(Canvas canvas) {
        float x = (float) getxPosInScreen(), y = (float) getyPosInScreen(), spin = getFrame() * 6;
        if (!blasted) {
            art.draw(canvas, x, y, getRadius() * 1.15f, getFrame(), spin);
            return;
        }
        float t = (float) Math.min(1, (getFrame() - DURATION) / FADE);
        float shockwave = blastRadius * (1.1f + 0.9f * t);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(blastRadius * 0.14f * (1 - t) + 1);
        paint.setColor(DrawUtil.withAlpha(SHOCKWAVE, (int) (200 * (1 - t))));
        canvas.drawCircle(x, y, shockwave, paint);
        float reach = blastRadius * 1.5f;
        canvas.saveLayerAlpha(x - reach, y - reach, x + reach, y + reach, (int) (255 * (1 - t) * (1 - t)));
        art.draw(canvas, x, y, blastRadius * (1.15f + 0.25f * t), getFrame(), spin);
        canvas.restore();
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
