package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.interactables.Loot;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 05/06/2017.
 */
public abstract class GamePowerUp extends GameDynamicObject implements Interactable {

    public static final int PORTAL = 0, COMPASS = 1, BOMB = 2, INFINITE_JUMPS = 3;

    private int type;
    private int width, height;
    private Paint paint = new Paint(), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // For drawing the icons, which are designed in a 100 by 100 box, lit from the top left with dark outlines
    protected final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Path iconPath = new Path();
    protected final RectF iconOval = new RectF();
    // Items in the cave glow, their icons in the HUD and the pause menu don't
    private boolean glowing;
    // Dropped by an enemy, so it flies to the character once it's close
    private boolean dropped = false, flying = false;

    public GamePowerUp(double xPos, double yPos, int width, int height, int type, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, 0, 0, 0, addToGameObjects, addToDynamicObjects);
        this.width = width;
        this.height = height;
        this.type = type;
        this.glowing = addToGameObjects;
    }

    // Pulsing behind the item, so it stands out in the cave
    protected void drawGlow(Canvas canvas) {
        if (glowing) {
            float pulse = 1 + 0.12f * (float) Math.sin(2 * Math.PI * getFrame() / 60);
            DrawUtil.drawGlow(canvas, glowPaint, (float) getxPosInScreen(), (float) getyPosInScreen(), Math.max(getWidth(), getHeight()) * 1.1f * pulse, getGlowColor());
        }
    }

    // Starts drawing in the icon's 100 by 100 box, scaled to the given size, with the design's point (50, centerY) on
    // the item, moved down by dy. Every call needs a canvas.restore() once the icon is drawn
    protected void beginIcon(Canvas canvas, float size, float centerY, float dy) {
        canvas.save();
        canvas.translate((float) getxPosInScreen(), (float) getyPosInScreen() + dy);
        canvas.scale(size / 100f, size / 100f);
        canvas.translate(-50, -centerY);
    }

    protected void fillCircle(Canvas canvas, float x, float y, float radius, int color) {
        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(color);
        canvas.drawCircle(x, y, radius, iconPaint);
    }

    protected void strokeCircle(Canvas canvas, float x, float y, float radius, int color, float width) {
        setStroke(color, width);
        canvas.drawCircle(x, y, radius, iconPaint);
    }

    protected void fillPath(Canvas canvas, Path path, int color) {
        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(color);
        canvas.drawPath(path, iconPaint);
    }

    // With round ends and corners
    protected void strokePath(Canvas canvas, Path path, int color, float width) {
        setStroke(color, width);
        canvas.drawPath(path, iconPaint);
    }

    // Part of the ellipse around the oval, from the start angle and clockwise, both in degrees from 3 o'clock
    protected void strokeArc(Canvas canvas, float cx, float cy, float rx, float ry, float start, float sweep, int color, float width) {
        setStroke(color, width);
        iconOval.set(cx - rx, cy - ry, cx + rx, cy + ry);
        canvas.drawArc(iconOval, start, sweep, false, iconPaint);
    }

    private void setStroke(int color, float width) {
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeCap(Paint.Cap.ROUND);
        iconPaint.setStrokeJoin(Paint.Join.ROUND);
        iconPaint.setStrokeWidth(width);
        iconPaint.setColor(color);
    }

    // Sets iconPath to a closed polygon through the given points, as x, y pairs
    protected Path polygon(float... points) {
        iconPath.reset();
        iconPath.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            iconPath.lineTo(points[i], points[i + 1]);
        }
        iconPath.close();
        return iconPath;
    }

    private int getGlowColor() {
        switch (type) {
            case PORTAL:
                return Color.argb(150, 200, 120, 255);
            case COMPASS:
                return Color.argb(150, 255, 240, 120);
            case BOMB:
                return Color.argb(150, 255, 150, 60);
            default:
                return Color.argb(150, 120, 230, 255);
        }
    }

    public double getDy(int frame, float maxDy) {
        return maxDy*Math.sin(2*Math.PI*frame/50);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setDropped() {
        dropped = true;
    }

    @Override
    public void detect() {
        if (dropped) {
            flying = Loot.pull(this, flying);
        }
        if (distanceTo(MyActivity.character) < MyActivity.TILE_WIDTH /2){
            MyActivity.canvas.gameObjects.remove(this);
            MyActivity.character.addPowerUp(this.type);
            MyActivity.character.checkIfRemoveInterest(this);
            Particles.burst(getxPosInRoom(), getyPosInRoom(), 12, Color.rgb(170, 255, 255), 0.07f, 0.03f, 0.5, 0);
        }
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
