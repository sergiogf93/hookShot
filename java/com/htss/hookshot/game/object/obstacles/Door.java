package com.htss.hookshot.game.object.obstacles;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.game.object.shapes.RectShape;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;

import java.util.Vector;

/**
 * Created by Sergio on 03/09/2016.
 */
public class Door extends GameDynamicObject {

    private int width, height;
    private Vector<WallButton> buttons;
    private MathVector vector;
    private Paint paint = new Paint();

    public Door(double xPos, double yPos, int width, int height, MathVector vector, Vector<WallButton> buttons) {
        super(xPos, yPos, 0, 0, 0);
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
        }
    }

    @Override
    public void draw(Canvas canvas) {
        DrawUtil.drawPolygon(getCorners(), canvas, Color.GRAY);
        drawButtons(canvas, getVector().scaled(-1 * (getWidth() / 2 - buttons.get(0).getRadius())).applyTo(getPositionInScreen()));
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
        return new RectShape(getxPosInRoom(), getyPosInRoom(), getWidth(), getHeight(), getVector(), false);
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
