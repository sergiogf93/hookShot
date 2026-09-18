package com.htss.hookshot.game.object.interactables;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.math.MathVector;

import java.util.Random;

/**
 * A gold coin dropped by a defeated enemy. It's thrown out, falls and bounces on the rock, spinning, and flies to the
 * character once it's close.
 */
public class Coin extends GameDynamicObject implements Interactable{

    public static final float RADIUS = MyActivity.TILE_WIDTH * 0.12f;
    private static final double GRAVITY = MyActivity.TILE_WIDTH / 100.0, MAX_FALL = MyActivity.TILE_WIDTH * 0.2,
            BOUNCE = 0.35, FRICTION = 0.8;
    private static final int GOLD = Color.rgb(247, 196, 58), GOLD_DARK = Color.rgb(196, 132, 20), EDGE = Color.rgb(90, 56, 8),
            SHINE = Color.rgb(255, 244, 196);

    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF oval = new RectF();
    private final MathVector velocity;
    private final float spinPhase;
    private boolean flying = false;

    // Thrown out of a point, upwards and to the sides
    public Coin(double xPos, double yPos) {
        super(xPos, yPos, 0, 0, 0, true, false);
        setGhost(true);
        Random random = new Random();
        double angle = Math.toRadians(-150 + random.nextDouble() * 120), speed = MyActivity.TILE_WIDTH * (0.05 + 0.07 * random.nextDouble());
        velocity = new MathVector(Math.cos(angle) * speed, Math.sin(angle) * speed);
        spinPhase = random.nextFloat() * 6;
    }

    @Override
    public void update() {
        updateFrame();
        if (flying) {
            return;
        }
        double x = getxPosInRoom(), y = getyPosInRoom();
        if (MyActivity.isInRoom(x, y) && GameEnemy.isRock(x, y)) {
            // Dropped inside rock, like by a worm digging through it, so it rises out of it
            velocity.x = 0;
            velocity.y = 0;
            setPositionInRoom(x, y - RADIUS / 2);
            return;
        }
        velocity.y = Math.min(velocity.y + GRAVITY, MAX_FALL);
        if (GameEnemy.isRock(x + velocity.x + Math.signum(velocity.x) * RADIUS, y)) {
            velocity.x = -velocity.x * BOUNCE;
        } else {
            x += velocity.x;
        }
        if (velocity.y > 0 && GameEnemy.isRock(x, y + velocity.y + RADIUS)) {
            // Lands, bouncing a little and sliding to a stop
            velocity.y = (velocity.y > GRAVITY * 3) ? -velocity.y * BOUNCE : 0;
            velocity.x *= FRICTION;
        } else if (velocity.y < 0 && GameEnemy.isRock(x, y + velocity.y - RADIUS)) {
            velocity.y = 0;
        } else {
            y += velocity.y;
        }
        setPositionInRoom(x, y);
    }

    // Flies to the character once it's close, and is picked up on reaching it
    @Override
    public void detect() {
        flying = Loot.pull(this, flying);
        if (distanceTo(MyActivity.character) < MyActivity.TILE_WIDTH / 3) {
            destroy();
            MyActivity.character.addCoins(1);
            Particles.burst(getxPosInRoom(), getyPosInRoom(), 5, SHINE, 0.04f, 0.02f, 0.25, 0);
        }
    }

    // Spinning, so it looks thinner and wider in turn, with a dark rim, a raised middle and a shine
    @Override
    public void draw(Canvas canvas) {
        float spin = Math.abs((float) Math.cos(getFrame() * 0.12 + spinPhase));
        drawCoin(canvas, (float) getxPosInScreen(), (float) getyPosInScreen(), RADIUS, 0.25f + 0.75f * spin);
    }

    // A coin seen at the given width, 1 facing straight on
    public static void drawCoin(Canvas canvas, float x, float y, float radius, float width) {
        float rx = radius * width;
        oval.set(x - rx, y - radius, x + rx, y + radius);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(GOLD);
        canvas.drawOval(oval, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.18f);
        paint.setColor(EDGE);
        canvas.drawOval(oval, paint);
        oval.set(x - rx * 0.6f, y - radius * 0.6f, x + rx * 0.6f, y + radius * 0.6f);
        paint.setStrokeWidth(radius * 0.12f);
        paint.setColor(GOLD_DARK);
        canvas.drawOval(oval, paint);
        if (width > 0.4f) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(SHINE);
            canvas.drawCircle(x - rx * 0.35f, y - radius * 0.35f, radius * 0.18f, paint);
        }
    }

    @Override
    public int getWidth() {
        return (int) (RADIUS*2);
    }

    @Override
    public int getHeight() {
        return (int) (RADIUS*2);
    }

}
