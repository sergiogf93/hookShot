package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

/**
 * The acid a rock spitter spits: it flies straight, and splashes on the character, on rock and on gates.
 */
public class AcidGlob extends GameDynamicObject {

    private static final float RADIUS = MyActivity.TILE_WIDTH * 0.12f;
    private static final double SPEED = MyActivity.TILE_WIDTH * 7.0 / MyActivity.UPDATES_PER_SECOND;
    private static final int DAMAGE = 5, LIFETIME = (int) TimeUtil.secondsToUpdates(4);
    public static final int ACID = Color.rgb(125, 255, 90), ACID_DARK = Color.rgb(30, 74, 18), ACID_LIGHT = Color.rgb(230, 255, 217);

    private final MathVector velocity;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AcidGlob(MathVector from, MathVector towards) {
        super(from.x, from.y, 0, 0, 0, true, false);
        setGhost(true);
        MathVector direction = new MathVector(from, towards);
        velocity = direction.isNull() ? new MathVector(SPEED, 0) : direction.rescaled(SPEED);
    }

    // In two halves, so it can't skip over thin rock or the character
    @Override
    public void update() {
        updateFrame();
        if (getFrame() > LIFETIME) {
            destroy();
            return;
        }
        for (int half = 0; half < 2; half++) {
            setPositionInRoom(velocity.scaled(0.5).applyTo(getPositionInRoom()));
            if (GameEnemy.isRock(getxPosInRoom(), getyPosInRoom()) || MyActivity.checkIfDoorsContain(getPositionInRoom())) {
                splash();
                return;
            }
            if (distanceTo(MyActivity.character) < RADIUS + MainCharacter.BODY_RADIUS) {
                MyActivity.character.getHurt(DAMAGE);
                splash();
                return;
            }
        }
    }

    private void splash() {
        destroy();
        Particles.burst(getxPosInRoom(), getyPosInRoom(), 10, ACID, 0.06f, 0.03f, 0.4, 0.004f);
    }

    // A bright drop with a dark edge and a fading trail
    @Override
    public void draw(Canvas canvas) {
        float x = (float) getxPosInScreen(), y = (float) getyPosInScreen();
        MathVector back = velocity.getUnitVector().scaled(-RADIUS);
        DrawUtil.drawGlow(canvas, glowPaint, x, y, RADIUS * 3, DrawUtil.withAlpha(ACID, 90));
        paint.setStyle(Paint.Style.FILL);
        for (int i = 3; i >= 1; i--) {
            paint.setColor(DrawUtil.withAlpha(ACID, 50 * (4 - i)));
            canvas.drawCircle(x + (float) back.x * i * 1.2f, y + (float) back.y * i * 1.2f, RADIUS * (1 - i * 0.2f), paint);
        }
        paint.setColor(ACID);
        canvas.drawCircle(x, y, RADIUS, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(RADIUS * 0.3f);
        paint.setColor(ACID_DARK);
        canvas.drawCircle(x, y, RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ACID_LIGHT);
        canvas.drawCircle(x - RADIUS * 0.35f, y - RADIUS * 0.35f, RADIUS * 0.3f, paint);
    }

    @Override
    public int getWidth() {
        return (int) (RADIUS * 2);
    }

    @Override
    public int getHeight() {
        return (int) (RADIUS * 2);
    }
}
