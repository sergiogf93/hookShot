package com.htss.hookshot.game.object.obstacles;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.HUDNotification;
import com.htss.hookshot.game.object.GameDynamicObject;
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

    private int width, height;
    private Vector<WallButton> buttons;
    private MathVector vector;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final int STEEL_LIGHT = Color.rgb(170, 176, 188), STEEL = Color.rgb(110, 116, 128),
            STEEL_DARK = Color.rgb(58, 62, 72), STEEL_EDGE = Color.rgb(28, 30, 36);

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
        boolean allOn = true;
        for (WallButton button : buttons){
            allOn = allOn && button.isOn();
        }
        if (allOn){
            MyActivity.canvas.gameObjects.remove(this);
            MyActivity.dynamicObjects.remove(this);
            MyActivity.notifications.add(new HUDNotification("DOOR OPENED!", TimeUtil.secondsToUpdates(1.667)));
            // The door breaks apart
            Particles.burst(getxPosInRoom(), getyPosInRoom(), 24, Color.GRAY, 0.12f, 0.05f, 0.8, 0.006f);
            ScreenShake.shake(0.05f);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        drawSteel(canvas);
        drawButtons(canvas, getVector().scaled(-1 * (getWidth() / 2 - buttons.get(0).getRadius())).applyTo(getPositionInScreen()));
    }

    // Steel lit along one side, with bars across it and a dark edge
    private void drawSteel(Canvas canvas) {
        Point[] corners = getCorners();
        Path outline = new Path();
        outline.moveTo(corners[0].x, corners[0].y);
        for (int i = 1; i < corners.length; i++) {
            outline.lineTo(corners[i].x, corners[i].y);
        }
        outline.close();
        MathVector center = getPositionInScreen();
        MathVector across = getVector().getNormal().scaled(getHeight() / 2);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient((float) (center.x + across.x), (float) (center.y + across.y), (float) (center.x - across.x), (float) (center.y - across.y),
                new int[]{STEEL_LIGHT, STEEL, STEEL_DARK}, null, Shader.TileMode.CLAMP));
        canvas.drawPath(outline, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(STEEL_EDGE);
        paint.setStrokeWidth(getHeight() / 10f);
        for (float along = -getWidth() / 2f + getHeight(); along < getWidth() / 2f - getHeight() / 2f; along += getHeight()) {
            MathVector bar = getVector().scaled(along).applyTo(center);
            canvas.drawLine((float) (bar.x + across.x * 0.7), (float) (bar.y + across.y * 0.7), (float) (bar.x - across.x * 0.7), (float) (bar.y - across.y * 0.7), paint);
        }
        paint.setStrokeWidth(getHeight() / 8f);
        canvas.drawPath(outline, paint);
    }

    private Point[] getCorners() {
        Point[] points = new Point[4];
        points[0] = getVector().getNormal().scaled(getHeight() / 2).applyTo(getVector().scaled(-1 * getWidth() / 2).applyTo(getPositionInScreen())).toPoint();
        points[1] = getVector().getNormal().scaled(-1 * getHeight() / 2).applyTo(getVector().scaled(-1 * getWidth() / 2).applyTo(getPositionInScreen())).toPoint();
        points[2] = getVector().getNormal().scaled(-1 * getHeight() / 2).applyTo(getVector().scaled(getWidth() / 2).applyTo(getPositionInScreen())).toPoint();
        points[3] = getVector().getNormal().scaled(getHeight() / 2).applyTo(getVector().scaled(getWidth() / 2).applyTo(getPositionInScreen())).toPoint();
        return points;
    }

    public void drawButtons(Canvas canvas, MathVector startPoint) {
        for (int i = 0 ; i < buttons.size() ; i++){
            paint.setStyle(Paint.Style.FILL);
            WallButton button = buttons.get(i);
            if (button.isOn()){
                paint.setColor(Color.GREEN);
            } else {
                paint.setColor(Color.RED);
            }
            MathVector position = getVector().scaled(i*4*button.getRadius()/3).applyTo(startPoint);
            canvas.drawCircle((float) position.x, (float) position.y, button.getRadius() / 3, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(button.getRadius() / 50);
            paint.setColor(Color.argb(255, 20, 20, 20));
            canvas.drawCircle((float) position.x, (float) position.y, button.getRadius() / 3, paint);
        }
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

    public MathVector getVector() {
        return vector;
    }

    public void setVector(MathVector vector) {
        this.vector = vector;
    }
}
