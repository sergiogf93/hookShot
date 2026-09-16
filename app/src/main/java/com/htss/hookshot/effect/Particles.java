package com.htss.hookshot.effect;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.util.TimeUtil;

import java.util.Random;

/**
 * Small short-lived dots for hits, dust, debris and sparkles. Kept in fixed arrays, so bursts don't allocate.
 */
public class Particles {

    private static final int MAX = 400;
    // How much of its speed a particle keeps on each update
    private static final float DRAG = 0.96f;

    private static float[] xs = new float[MAX], ys = new float[MAX], speedsX = new float[MAX], speedsY = new float[MAX],
            sizes = new float[MAX], gravities = new float[MAX];
    private static int[] colors = new int[MAX], ages = new int[MAX], lifetimes = new int[MAX];
    private static int count = 0;
    private static Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static Random random = new Random();

    // Particles flying out of a point in the room in every direction. Speed, size and gravity are in tiles
    public static void burst(double x, double y, int amount, int color, float speed, float size, double seconds, float gravity) {
        burst(x, y, amount, color, speed, size, seconds, gravity, 0, 360);
    }

    // The same, between two angles in degrees, clockwise from the right, as the screen's y points down
    public static void burst(double x, double y, int amount, int color, float speed, float size, double seconds, float gravity, float fromAngle, float toAngle) {
        float tile = MyActivity.TILE_WIDTH;
        double updates = TimeUtil.secondsToUpdates(seconds);
        for (int i = 0; i < amount && count < MAX; i++) {
            double angle = Math.toRadians(fromAngle + random.nextFloat() * (toAngle - fromAngle));
            float particleSpeed = speed * tile * (0.3f + 0.7f * random.nextFloat());
            xs[count] = (float) x;
            ys[count] = (float) y;
            speedsX[count] = (float) Math.cos(angle) * particleSpeed;
            speedsY[count] = (float) Math.sin(angle) * particleSpeed;
            sizes[count] = size * tile * (0.6f + 0.8f * random.nextFloat());
            gravities[count] = gravity * tile;
            colors[count] = color;
            ages[count] = 0;
            lifetimes[count] = Math.max(1, (int) (updates * (0.6f + 0.4f * random.nextFloat())));
            count++;
        }
    }

    // Once per game update
    public static void update() {
        for (int i = 0; i < count; i++) {
            ages[i]++;
            if (ages[i] >= lifetimes[i]) {
                remove(i);
                i--;
                continue;
            }
            speedsX[i] *= DRAG;
            speedsY[i] = speedsY[i] * DRAG + gravities[i];
            xs[i] += speedsX[i];
            ys[i] += speedsY[i];
        }
    }

    // Fading out and shrinking as they age
    public static void draw(Canvas canvas) {
        for (int i = 0; i < count; i++) {
            float progress = (float) ages[i] / lifetimes[i];
            paint.setColor(colors[i]);
            paint.setAlpha((int) (Color.alpha(colors[i]) * (1 - progress)));
            canvas.drawCircle(xs[i] + GameBoard.dx, ys[i] + GameBoard.dy, sizes[i] * (1 - 0.5f * progress), paint);
        }
    }

    public static void clear() {
        count = 0;
    }

    // The last particle takes the removed one's place
    private static void remove(int i) {
        count--;
        xs[i] = xs[count];
        ys[i] = ys[count];
        speedsX[i] = speedsX[count];
        speedsY[i] = speedsY[count];
        sizes[i] = sizes[count];
        gravities[i] = gravities[count];
        colors[i] = colors[count];
        ages[i] = ages[count];
        lifetimes[i] = lifetimes[count];
    }
}
