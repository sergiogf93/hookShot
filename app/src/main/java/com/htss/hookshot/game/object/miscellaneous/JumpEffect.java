package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 08/06/2017.
 */
public class JumpEffect extends GameDynamicObject {

    // The ring every jump in the air leaves under the character, growing and fading, like the swiftness icon's rings
    private static final double DURATION = TimeUtil.secondsToUpdates(0.3);
    private static final int OUTLINE = Color.rgb(10, 58, 71), RING = Color.rgb(43, 184, 214);

    private int maxWidth, maxHeight;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF oval = new RectF();

    public JumpEffect(double xPos, double yPos, int maxWidth, int maxHeight, boolean addToGameObjectsList, boolean addToDynamicObjectsList) {
        super(xPos, yPos, 0, 0, 0, addToGameObjectsList, addToDynamicObjectsList);
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
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
        float t = (float) Math.min(1, getFrame() / DURATION);
        // Quick at first, slowing down as it fades
        float grown = 1 - (1 - t) * (1 - t);
        float x = (float) getxPosInScreen(), y = (float) getyPosInScreen(), rx = maxWidth / 2f * grown, ry = maxHeight / 2f * grown;
        canvas.saveLayerAlpha(x - maxWidth, y - maxWidth, x + maxWidth, y + maxWidth, (int) (255 * (1 - t)));
        drawRing(canvas, paint, oval, x, y, rx, ry, maxWidth / 9f, maxWidth / 15f, RING);
        canvas.restore();
    }

    // A cyan ring open at the top, with a dark outline, as jumps leave and the swiftness icon shows
    public static void drawRing(Canvas canvas, Paint paint, RectF oval, float cx, float cy, float rx, float ry, float outlineWidth, float width, int color) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        oval.set(cx - rx, cy - ry, cx + rx, cy + ry);
        paint.setStrokeWidth(outlineWidth);
        paint.setColor(OUTLINE);
        canvas.drawArc(oval, -35, 250, false, paint);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        canvas.drawArc(oval, -35, 250, false, paint);
    }

    @Override
    public int getWidth() {
        return maxWidth;
    }

    @Override
    public int getHeight() {
        return maxHeight;
    }
}
