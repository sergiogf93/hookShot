package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.htss.hookshot.util.TimeUtil;

/**
 * How portals look, placed in the cave and as the power-up's icon: a round ring, half red and half blue, lit from the
 * top left, around a dark middle with a swirl in it. The split turns back and forth and the swirl spins. Drawn from a
 * design 100 units wide, with the ring's outline 36 units from the middle.
 */
public class PortalArt {

    public static final int RED = Color.rgb(232, 57, 47), BLUE = Color.rgb(47, 111, 240);
    private static final int OUTLINE = Color.rgb(18, 12, 28), DEPTH = Color.rgb(13, 10, 24),
            SWIRL_BLUE = Color.rgb(92, 141, 255), SWIRL_RED = Color.rgb(255, 106, 90),
            LIGHT = Color.argb(120, 255, 255, 255), SHADE = Color.argb(90, 0, 0, 20), GLINT = Color.argb(210, 255, 240, 240);
    private static final float DESIGN_RADIUS = 36;
    private static final double SPLIT_PERIOD = TimeUtil.secondsToUpdates(1.667);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();

    // With its outline at the given radius. Open, linked to its twin, its middle is dark and swirls. Closed, the cave
    // shows through it
    public void draw(Canvas canvas, float x, float y, float radius, int frame, boolean open) {
        if (radius <= 0) {
            return;
        }
        canvas.save();
        canvas.translate(x, y);
        canvas.scale(radius / DESIGN_RADIUS, radius / DESIGN_RADIUS);
        canvas.translate(-50, -50);
        stroke(OUTLINE, 12);
        canvas.drawCircle(50, 50, 30, paint);
        stroke(BLUE, 9);
        canvas.drawCircle(50, 50, 28.5f, paint);
        paint.setColor(RED);
        oval.set(21.5f, 21.5f, 78.5f, 78.5f);
        canvas.drawArc(oval, 155 + 180 * (float) Math.sin(2 * Math.PI * frame / SPLIT_PERIOD), 180, false, paint);
        if (open) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(DEPTH);
            canvas.drawCircle(50, 50, 24, paint);
            canvas.save();
            canvas.rotate(frame * 4, 50, 50);
            drawSwirl(canvas, SWIRL_BLUE);
            canvas.rotate(180, 50, 50);
            drawSwirl(canvas, SWIRL_RED);
            canvas.restore();
        } else {
            stroke(OUTLINE, 1.5f);
            canvas.drawCircle(50, 50, 24, paint);
        }
        path.reset();
        path.moveTo(23, 38);
        path.quadTo(29, 22, 44, 17);
        stroke(LIGHT, 4);
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(77, 62);
        path.quadTo(71, 78, 56, 83);
        stroke(SHADE, 4);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(GLINT);
        canvas.drawCircle(29, 27, 2.5f, paint);
        canvas.restore();
    }

    private void drawSwirl(Canvas canvas, int color) {
        path.reset();
        path.moveTo(50, 50);
        path.cubicTo(50, 44, 56, 44, 56, 50);
        path.cubicTo(56, 57, 44, 57, 44, 50);
        path.cubicTo(44, 41, 59, 39, 62, 49);
        stroke(color, 2.5f);
        canvas.drawPath(path, paint);
    }

    private void stroke(int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setColor(color);
    }

}
