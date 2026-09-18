package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * How explosions look, in the cave, as the bomb power-up's icon and above the character for every explosion left:
 * red, orange and yellow bursts with a dark outline, hottest towards the top left. The inner bursts flicker. Drawn
 * from a design 100 units wide, with the outer burst's points 42 units from the middle.
 */
public class BurstArt {

    private static final int OUTLINE = Color.rgb(122, 18, 8), RED = Color.rgb(216, 50, 28), ORANGE = Color.rgb(255, 122, 26),
            YELLOW = Color.rgb(255, 210, 63), CORE = Color.rgb(255, 246, 200);
    private static final float DESIGN_RADIUS = 42;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    // With its outer points at the given radius, turned by the rotation in degrees. The frame sets the flicker
    public void draw(Canvas canvas, float x, float y, float radius, int frame, float rotation) {
        if (radius <= 0) {
            return;
        }
        canvas.save();
        canvas.translate(x, y);
        canvas.scale(radius / DESIGN_RADIUS, radius / DESIGN_RADIUS);
        canvas.rotate(rotation);
        canvas.translate(-50, -52);
        star(50, 52, 42, 28, 12, -90, 1);
        fill(RED);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(3);
        paint.setColor(OUTLINE);
        canvas.drawPath(path, paint);
        star(48, 50, 30, 20, 10, -72, 1 + 0.06f * (float) Math.sin(frame * 0.6));
        fill(ORANGE);
        canvas.drawPath(path, paint);
        star(46, 48, 19, 12, 8, -90, 1 + 0.1f * (float) Math.sin(frame * 0.9 + 1));
        fill(YELLOW);
        canvas.drawPath(path, paint);
        fill(CORE);
        canvas.drawCircle(44, 46, 6, paint);
        canvas.restore();
    }

    private void fill(int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
    }

    // Sets the path to a star with the given points, the first one at the rotation in degrees, grown by the scale
    private void star(float cx, float cy, float outer, float inner, int points, float rotation, float scale) {
        path.reset();
        for (int k = 0; k < points * 2; k++) {
            float radius = (k % 2 == 0 ? outer : inner) * scale;
            double angle = Math.toRadians(rotation + k * 180.0 / points);
            float x = cx + radius * (float) Math.cos(angle), y = cy + radius * (float) Math.sin(angle);
            if (k == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }

}
