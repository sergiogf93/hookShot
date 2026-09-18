package com.htss.hookshot.game.object.obstacles;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.HUDNotification;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.game.object.shapes.RectShape;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

import java.util.Vector;

/**
 * Created by Sergio on 03/09/2016.
 */
public class Door extends GameDynamicObject {

    // Opens this long once every button is pressed, sliding into the anchors at its ends, which stay in the rock
    private static final int OPEN_UPDATES = (int) TimeUtil.secondsToUpdates(0.5);
    private static final int STEEL_LIGHT = Color.rgb(170, 176, 188), STEEL = Color.rgb(110, 116, 128),
            STEEL_DARK = Color.rgb(58, 62, 72), STEEL_EDGE = Color.rgb(28, 30, 36), BAND = Color.rgb(78, 83, 95),
            ANCHOR_LIGHT = Color.rgb(96, 101, 114), ANCHOR_DARK = Color.rgb(36, 38, 46), RIVET = Color.rgb(196, 202, 214),
            LOCK = Color.rgb(24, 26, 32), LIGHT_OFF = Color.rgb(255, 70, 50), LIGHT_ON = Color.rgb(120, 255, 100);

    private int width, height;
    private Vector<WallButton> buttons;
    // An enemy that has to be beaten too, like the deep worm, which gets its own light on the lock
    private GameEnemy guardian;
    private MathVector vector;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rect = new RectF();
    private LinearGradient plateShader, anchorShader;
    // Updates since it started opening, or -1 while closed
    private int openingUpdates = -1;

    public Door(double xPos, double yPos, int width, int height, MathVector vector, Vector<WallButton> buttons, boolean addToLists) {
        super(xPos, yPos, 0, 0, 0, addToLists, addToLists);
        this.width = width;
        this.height = height;
        this.vector = vector;
        this.buttons = buttons;
    }

    @Override
    public void update(){
        super.update();
        if (openingUpdates >= 0) {
            openingUpdates = Math.min(openingUpdates + 1, OPEN_UPDATES);
            return;
        }
        boolean allOn = true;
        for (WallButton button : buttons){
            allOn = allOn && button.isOn();
        }
        if (guardian != null && MyActivity.enemies.contains(guardian)) {
            allOn = false;
        }
        if (allOn){
            // Out of the objects that block and can be hooked, but still drawn while it slides open
            MyActivity.dynamicObjects.remove(this);
            setGhost(true);
            openingUpdates = 0;
            MyActivity.notifications.add(new HUDNotification("DOOR OPENED!", TimeUtil.secondsToUpdates(1.667)));
            // Sparks where it splits
            Particles.burst(getxPosInRoom(), getyPosInRoom(), 16, Color.rgb(255, 220, 140), 0.1f, 0.03f, 0.5, 0.004f);
            ScreenShake.shake(0.05f);
        }
    }

    // Banded iron plates between two anchors bolted into the rock, with a lock in the middle that has a light for
    // every wall button. Drawn along the x axis, turned to the door's direction. Opening, it splits at the lock and
    // each half slides into its anchor
    @Override
    public void draw(Canvas canvas) {
        float half = getWidth() / 2f, thick = getHeight() / 2f;
        float open = (openingUpdates < 0) ? 0 : Math.min(1, openingUpdates / (float) OPEN_UPDATES);
        float slide = open * open * (3 - 2 * open) * half;
        // Turned whichever way keeps its lit edge towards the top left, like the rest of the cave
        double angle = Math.atan2(getVector().y, getVector().x);
        if (Math.cos(angle) - Math.sin(angle) < 0) {
            angle += Math.PI;
        }
        canvas.save();
        canvas.translate((float) getxPosInScreen(), (float) getyPosInScreen());
        canvas.rotate((float) Math.toDegrees(angle));
        for (int side = -1; side <= 1; side += 2) {
            canvas.save();
            canvas.clipRect(side < 0 ? -half : slide, -thick * 2, side < 0 ? -slide : half, thick * 2);
            canvas.translate(side * slide, 0);
            drawPlates(canvas, half, thick);
            drawLock(canvas, half, thick);
            canvas.restore();
        }
        drawAnchor(canvas, -half, thick);
        drawAnchor(canvas, half, thick);
        canvas.restore();
    }

    private void drawPlates(Canvas canvas, float half, float thick) {
        if (plateShader == null) {
            plateShader = new LinearGradient(0, -thick, 0, thick, new int[]{STEEL_LIGHT, STEEL, STEEL_DARK},
                    new float[]{0, 0.4f, 1}, Shader.TileMode.CLAMP);
        }
        rect.set(-half, -thick, half, thick);
        paint.setStyle(Paint.Style.FILL);
        // Opaque, whatever the lights' highlights left in the paint, as the shader is drawn with the paint's alpha
        paint.setAlpha(255);
        paint.setShader(plateShader);
        canvas.drawRect(rect, paint);
        paint.setShader(null);
        // A band across every seam between plates, riveted near both edges
        float band = thick * 0.3f;
        for (float x = -half + thick * 2; x < half - thick; x += thick * 2) {
            rect.set(x - band / 2, -thick, x + band / 2, thick);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(BAND);
            canvas.drawRect(rect, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(thick / 16f);
            paint.setColor(STEEL_EDGE);
            canvas.drawRect(rect, paint);
            drawRivet(canvas, x, -thick * 0.62f, thick);
            drawRivet(canvas, x, thick * 0.62f, thick);
        }
        rect.set(-half, -thick, half, thick);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(thick / 6f);
        paint.setColor(STEEL_EDGE);
        canvas.drawRect(rect, paint);
    }

    // The lights are red, pulsing, until their button is pressed, and then green
    private void drawLock(Canvas canvas, float half, float thick) {
        int n = buttons.size() + ((guardian != null) ? 1 : 0);
        float spacing = Math.min(thick * 0.8f, (half * 2 - thick * 4) / Math.max(1, n));
        float light = Math.min(thick * 0.22f, spacing * 0.35f);
        float first = -(n - 1) * spacing / 2;
        float plateHalf = -first + thick * 0.5f;
        rect.set(-plateHalf, -thick * 0.5f, plateHalf, thick * 0.5f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(LOCK);
        canvas.drawRoundRect(rect, thick * 0.5f, thick * 0.5f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(thick / 8f);
        paint.setColor(STEEL_EDGE);
        canvas.drawRoundRect(rect, thick * 0.5f, thick * 0.5f, paint);
        float pulse = 0.55f + 0.45f * (float) Math.sin(getFrame() * 0.15);
        for (int i = 0; i < n; i++) {
            float x = first + i * spacing;
            boolean on = (i < buttons.size()) ? buttons.get(i).isOn() : !MyActivity.enemies.contains(guardian);
            int color = on ? LIGHT_ON : LIGHT_OFF;
            DrawUtil.drawGlow(canvas, glowPaint, x, 0, light * 3, DrawUtil.withAlpha(color, on ? 150 : (int) (150 * pulse)));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(on ? color : DrawUtil.blend(Color.rgb(90, 20, 14), color, pulse));
            canvas.drawCircle(x, 0, light, paint);
            paint.setColor(Color.argb(150, 255, 255, 255));
            canvas.drawCircle(x - light * 0.35f, -light * 0.35f, light * 0.3f, paint);
        }
    }

    // A block, thicker than the plates, bolted into the rock where the door ends
    private void drawAnchor(Canvas canvas, float x, float thick) {
        float halfWidth = thick * 0.8f, halfHeight = thick * 1.3f;
        if (anchorShader == null) {
            anchorShader = new LinearGradient(0, -halfHeight, 0, halfHeight, ANCHOR_LIGHT, ANCHOR_DARK, Shader.TileMode.CLAMP);
        }
        rect.set(x - halfWidth, -halfHeight, x + halfWidth, halfHeight);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setShader(anchorShader);
        canvas.drawRoundRect(rect, thick * 0.25f, thick * 0.25f, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(thick / 6f);
        paint.setColor(STEEL_EDGE);
        canvas.drawRoundRect(rect, thick * 0.25f, thick * 0.25f, paint);
        drawRivet(canvas, x, -halfHeight * 0.6f, thick);
        drawRivet(canvas, x, halfHeight * 0.6f, thick);
    }

    private void drawRivet(Canvas canvas, float x, float y, float thick) {
        float radius = thick * 0.11f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(STEEL_EDGE);
        canvas.drawCircle(x, y, radius, paint);
        paint.setColor(RIVET);
        canvas.drawCircle(x - radius * 0.25f, y - radius * 0.25f, radius * 0.6f, paint);
    }

    @Override
    public GameShape getBounds() {
        return new RectShape(getxPosInRoom(), getyPosInRoom(), getWidth(), getHeight(), getVector(), false, false);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setGuardian(GameEnemy guardian) {
        this.guardian = guardian;
    }

    public MathVector getVector() {
        return vector;
    }

    public void setVector(MathVector vector) {
        this.vector = vector;
    }
}
