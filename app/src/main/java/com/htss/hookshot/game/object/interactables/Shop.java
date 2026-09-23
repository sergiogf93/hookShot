package com.htss.hookshot.game.object.interactables;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.UiStyle;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.util.DrawUtil;

/**
 * A merchant's stall standing on the cave floor now and then: a counter under a striped awning, lit by a lantern. It
 * sells powers and health for coins. Standing at it, the button that uses powers opens it instead, which a sign over
 * the stall says. It's behind everything else, and nothing bumps into it.
 */
public class Shop extends GameDynamicObject {

    private static final float WIDTH = (float) (Map.SQUARE_SIZE * 2.6), HEIGHT = (float) (Map.SQUARE_SIZE * 2.8),
            COUNTER = (float) (Map.SQUARE_SIZE * 1.05), AWNING = (float) (Map.SQUARE_SIZE * 0.75);
    private static final int WOOD = Color.rgb(122, 78, 44), WOOD_LIGHT = Color.rgb(168, 116, 68), WOOD_DARK = Color.rgb(70, 42, 22),
            EDGE = Color.rgb(34, 20, 10), STRIPE = Color.rgb(186, 52, 44), STRIPE_LIGHT = Color.rgb(236, 222, 190),
            LANTERN = Color.rgb(255, 196, 90), LANTERN_GLOW = Color.argb(120, 255, 180, 80), GLASS = Color.rgb(255, 236, 170);
    private static final int STRIPES = 6;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    // Standing on the floor at the given point, with its middle over it
    public Shop(double x, double floorY) {
        super(x, floorY - HEIGHT / 2, 0, 0, 0, false, false);
        setGhost(true);
        // Behind everything else, so the character and what it picks up are drawn over it
        MyActivity.canvas.gameObjects.add(0, this);
        textPaint.setTypeface(GameBoard.paint.getTypeface());
    }

    // It stands still, only its lantern flickers
    @Override
    public void update() {
        updateFrame();
    }

    // The stall the character is standing at, if any
    public static Shop getNearby() {
        if (MyActivity.character == null) {
            return null;
        }
        for (GameObject object : MyActivity.canvas.gameObjects) {
            if (object instanceof Shop && ((Shop) object).isNear()) {
                return (Shop) object;
            }
        }
        return null;
    }

    private boolean isNear() {
        double dx = MyActivity.character.getxPosInRoom() - getxPosInRoom(), dy = MyActivity.character.getyPosInRoom() - getyPosInRoom();
        return Math.abs(dx) < WIDTH * 0.65 && Math.abs(dy) < HEIGHT * 0.6;
    }

    @Override
    public void draw(Canvas canvas) {
        float x = (float) getxPosInScreen(), top = (float) getyPosInScreen() - HEIGHT / 2, floor = top + HEIGHT;
        float left = x - WIDTH / 2, right = x + WIDTH / 2, post = WIDTH * 0.07f;
        float flicker = 1 + 0.06f * (float) Math.sin(getFrame() * 0.21) + 0.04f * (float) Math.sin(getFrame() * 0.53);
        float lanternX = left + post * 2.6f, lanternY = top + AWNING + (float) Map.SQUARE_SIZE * 0.45f;
        DrawUtil.drawGlow(canvas, glowPaint, lanternX, lanternY, (float) Map.SQUARE_SIZE * 2.2f * flicker, LANTERN_GLOW);

        // The posts holding up the awning, behind the counter
        fillRect(canvas, left + post * 0.3f, top + AWNING * 0.5f, left + post * 1.3f, floor, WOOD_DARK);
        fillRect(canvas, right - post * 1.3f, top + AWNING * 0.5f, right - post * 0.3f, floor, WOOD_DARK);

        // The counter, of planks, with a lighter board on top and what's for sale on it
        float counterTop = floor - COUNTER;
        fillRect(canvas, left, counterTop, right, floor, WOOD);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(post * 0.3f);
        paint.setColor(WOOD_DARK);
        for (int i = 1; i < 3; i++) {
            float plank = counterTop + COUNTER * i / 3;
            canvas.drawLine(left, plank, right, plank, paint);
        }
        fillRect(canvas, left - post * 0.4f, counterTop - post * 0.7f, right + post * 0.4f, counterTop, WOOD_LIGHT);
        outline(canvas, left, counterTop, right, floor);
        outline(canvas, left - post * 0.4f, counterTop - post * 0.7f, right + post * 0.4f, counterTop);
        drawWares(canvas, x, counterTop - post * 0.7f);

        // The lantern, hanging from the awning
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(post * 0.25f);
        paint.setColor(EDGE);
        canvas.drawLine(lanternX, top + AWNING, lanternX, lanternY - post, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(DrawUtil.blend(LANTERN, GLASS, 0.5f * (flicker - 0.9f)));
        rect.set(lanternX - post * 0.8f, lanternY - post, lanternX + post * 0.8f, lanternY + post);
        canvas.drawRoundRect(rect, post * 0.4f, post * 0.4f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(EDGE);
        canvas.drawRoundRect(rect, post * 0.4f, post * 0.4f, paint);

        drawAwning(canvas, left - post, right + post, top);
        // A coin on the awning, so what the stall is for is seen from afar
        Coin.drawCoin(canvas, x, top + AWNING * 0.45f, (float) Map.SQUARE_SIZE * 0.3f, 1);

        if (isNear() && !MyActivity.paused) {
            drawHint(canvas, x, top - (float) Map.SQUARE_SIZE * 0.4f);
        }
    }

    // Striped cloth, sloping out from the back, with a scalloped edge
    private void drawAwning(Canvas canvas, float left, float right, float top) {
        float stripe = (right - left) / STRIPES, scallop = stripe / 2;
        for (int i = 0; i < STRIPES; i++) {
            float from = left + i * stripe, to = from + stripe;
            path.reset();
            path.moveTo(from + stripe * 0.15f, top);
            path.lineTo(to + stripe * 0.15f - ((i == STRIPES - 1) ? stripe * 0.15f : 0), top);
            path.lineTo(to, top + AWNING);
            path.arcTo(from, top + AWNING - scallop, to, top + AWNING + scallop, 0, 180, false);
            path.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor((i % 2 == 0) ? STRIPE : STRIPE_LIGHT);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stripe * 0.08f);
            paint.setColor(EDGE);
            canvas.drawPath(path, paint);
        }
    }

    // A potion and a bomb on the counter
    private void drawWares(Canvas canvas, float x, float counterTop) {
        float unit = (float) Map.SQUARE_SIZE * 0.22f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(90, 220, 110));
        canvas.drawCircle(x - unit * 2.2f, counterTop - unit, unit, paint);
        fillRect(canvas, x - unit * 2.5f, counterTop - unit * 2.4f, x - unit * 1.9f, counterTop - unit * 1.6f, Color.rgb(200, 190, 170));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(40, 40, 48));
        canvas.drawCircle(x + unit * 2.2f, counterTop - unit, unit, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(unit * 0.25f);
        paint.setColor(Color.rgb(230, 170, 60));
        canvas.drawLine(x + unit * 2.6f, counterTop - unit * 1.8f, x + unit * 3f, counterTop - unit * 2.4f, paint);
    }

    // What to press to shop, over the stall: the power button, or B with the classic controls, which have none
    private void drawHint(Canvas canvas, float x, float y) {
        String hint = (MyActivity.controls == MyActivity.CONTROLS_CLASSIC) ? "B TO SHOP" : "USE TO SHOP";
        textPaint.setTextSize(MyActivity.TILE_WIDTH * 0.3f);
        UiStyle.drawText(canvas, textPaint, hint, x - textPaint.measureText(hint) / 2, y, UiStyle.GOLD);
    }

    private void fillRect(Canvas canvas, float left, float top, float right, float bottom, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(left, top, right, bottom, paint);
    }

    private void outline(Canvas canvas, float left, float top, float right, float bottom) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(WIDTH * 0.02f);
        paint.setColor(EDGE);
        canvas.drawRect(left, top, right, bottom, paint);
    }

    @Override
    public int getWidth() {
        return (int) WIDTH;
    }

    @Override
    public int getHeight() {
        return (int) HEIGHT;
    }
}
