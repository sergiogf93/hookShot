package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.util.DrawUtil;

/**
 * The look shared by menus, tips and controls: dark see-through fills with an edge, and light text with a shadow.
 * The edge and pressed colours follow the cave's colours.
 */
public class UiStyle {

    public static final int TEXT = Color.rgb(240, 240, 245);
    // Gold for what the chain can do right now, like where it will grip, and grey for what it can't
    public static final int GOLD = Color.rgb(247, 196, 58), IDLE = Color.rgb(170, 176, 186);
    private static final int TEXT_SHADOW = Color.argb(200, 0, 0, 0);
    // Menus and tips hide what's behind them more than the controls, which sit over the game
    private static final int PANEL_ALPHA = 215, CONTROL_ALPHA = 90, PRESSED_CONTROL_ALPHA = 150, EDGE_ALPHA = 190;

    private static final int IDLE_ALPHA = 45, IDLE_EDGE_ALPHA = 120;

    private static RectF circle = new RectF();
    private static DashPathEffect dashes;

    public static int getAccent() {
        return CavePalette.forLevel(MyActivity.canvas.myActivity.level).rim;
    }

    public static void drawPanel(Canvas canvas, Paint paint, RectF rect, float cornerRadius) {
        drawShape(canvas, paint, rect, cornerRadius, getFill(false, PANEL_ALPHA));
    }

    // Round buttons in menus
    public static void drawCircle(Canvas canvas, Paint paint, float x, float y, float radius, boolean pressed) {
        circle.set(x - radius, y - radius, x + radius, y + radius);
        drawShape(canvas, paint, circle, radius, getFill(pressed, PANEL_ALPHA));
    }

    // Controls over the game
    public static void drawControl(Canvas canvas, Paint paint, float x, float y, float radius, boolean pressed) {
        circle.set(x - radius, y - radius, x + radius, y + radius);
        drawControl(canvas, paint, circle, pressed);
    }

    public static void drawControl(Canvas canvas, Paint paint, RectF oval, boolean pressed) {
        float cornerRadius = Math.min(oval.width(), oval.height()) / 2;
        drawShape(canvas, paint, oval, cornerRadius, getFill(pressed, pressed ? PRESSED_CONTROL_ALPHA : CONTROL_ALPHA));
    }

    // A control with an edge of its own colour, like the stick that works the chain
    public static void drawControl(Canvas canvas, Paint paint, float x, float y, float radius, boolean pressed, int edge) {
        circle.set(x - radius, y - radius, x + radius, y + radius);
        drawShape(canvas, paint, circle, radius, getFill(pressed, pressed ? PRESSED_CONTROL_ALPHA : CONTROL_ALPHA), edge, false);
    }

    // A control with nothing to do right now, like letting go with no chain out: fainter, with a dashed edge
    public static void drawIdleControl(Canvas canvas, Paint paint, float x, float y, float radius) {
        circle.set(x - radius, y - radius, x + radius, y + radius);
        drawShape(canvas, paint, circle, radius, getFill(false, IDLE_ALPHA), DrawUtil.withAlpha(IDLE, IDLE_EDGE_ALPHA), true);
    }

    // Light text with a dark shadow, readable over anything
    public static void drawText(Canvas canvas, Paint paint, String text, float x, float y, int color) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        float offset = paint.getTextSize() / 16;
        paint.setColor(TEXT_SHADOW);
        canvas.drawText(text, x + offset, y + offset, paint);
        paint.setColor(color);
        canvas.drawText(text, x, y, paint);
    }

    private static int getFill(boolean pressed, int alpha) {
        CavePalette palette = CavePalette.forLevel(MyActivity.canvas.myActivity.level);
        int fill = pressed ? DrawUtil.blend(palette.backgroundBottom, palette.rim, 0.45f) : DrawUtil.blend(Color.BLACK, palette.backgroundBottom, 0.7f);
        return DrawUtil.withAlpha(fill, alpha);
    }

    private static void drawShape(Canvas canvas, Paint paint, RectF rect, float cornerRadius, int fill) {
        drawShape(canvas, paint, rect, cornerRadius, fill, DrawUtil.withAlpha(getAccent(), EDGE_ALPHA), false);
    }

    private static void drawShape(Canvas canvas, Paint paint, RectF rect, float cornerRadius, int fill, int edge, boolean dashed) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(MyActivity.TILE_WIDTH / 30f);
        paint.setColor(edge);
        if (dashed) {
            if (dashes == null) {
                dashes = new DashPathEffect(new float[]{MyActivity.TILE_WIDTH * 0.08f, MyActivity.TILE_WIDTH * 0.08f}, 0);
            }
            paint.setPathEffect(dashes);
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
        paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);
    }
}
