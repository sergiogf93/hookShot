package com.htss.hookshot.game.object.shapes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 03/08/2016.
 */
public class RectShape extends GameShape{

    private int width, height;
    private MathVector vector;
    private boolean fill;

    public RectShape(double xPos, double yPos, int width, int height, boolean fill) {
        super(xPos, yPos, Color.YELLOW);
        this.width = width;
        this.height = height;
        this.fill = fill;
        this.vector = new MathVector(1, 0);
    }

    public RectShape(double xPos, double yPos, int width, int height, boolean fill, int color) {
        super(xPos, yPos, color);
        this.width = width;
        this.height = height;
        this.vector = new MathVector(1, 0);
        this.fill = fill;
    }

    public RectShape (Rect r, boolean fill, int color){
        super(r.centerX(),r.centerY(),color);
        this.width = r.width();
        this.height = r.height();
        this.vector = new MathVector(1, 0);
        this.fill = fill;
    }

    public RectShape(double xPos, double yPos, int width, int height, MathVector vector, boolean fill) {
        super(xPos, yPos, Color.YELLOW);
        this.width = width;
        this.height = height;
        this.vector = vector;
        this.fill = fill;
    }

    @Override
    public int getIntersectMagnitude() {
        return Math.max(getWidth(), getHeight())/2;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(color);
        if (!fill) {
            paint.setStyle(Paint.Style.STROKE);
        }
        canvas.drawRect((float) getxPosInScreen() - getWidth() / 2, (float) getyPosInScreen() - getHeight() / 2, (float) getxPosInScreen() + getWidth() / 2, (float) getyPosInScreen() + getHeight() / 2, paint);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }


    @Override
    public boolean intersect(GameShape shape) {
        Rect r1 = new Rect((int)getxPosInRoom()-getWidth()/2,(int)getyPosInRoom()-getHeight()/2,(int)getxPosInRoom()+getWidth()/2,(int)getyPosInRoom()+getHeight()/2);
        Rect r2;
        if (shape instanceof CircleShape){
            r2 = new Rect((int) shape.getxPosInRoom() - ((CircleShape) shape).getRadius(), (int) shape.getyPosInRoom() - ((CircleShape) shape).getRadius(), (int) shape.getxPosInRoom() + ((CircleShape) shape).getRadius(), (int) shape.getyPosInRoom() + ((CircleShape) shape).getRadius());
        } else {
            r2 = new Rect((int) shape.getxPosInRoom() - shape.getWidth() / 2, (int) shape.getyPosInRoom() - shape.getHeight() / 2, (int) shape.getxPosInRoom() + shape.getWidth() / 2, (int) shape.getyPosInRoom() + shape.getHeight() / 2);
        }
        return r1.intersect(r2);
    }

    @Override
    public boolean contains(MathVector p) {
        boolean contained;
        if (getVector().x == 1.0 && getVector().y == 0.0) {
            Rect r1 = new Rect((int) getxPosInRoom() - getWidth() / 2, (int) getyPosInRoom() - getHeight() / 2, (int) getxPosInRoom() + getWidth() / 2, (int) getyPosInRoom() + getHeight() / 2);
            contained = r1.contains((int) p.x, (int) p.y);
        } else {
            Point[] points = getCorners();
            double areaRectangle = GameMath.areaRectangle(points);
            double addedAreas = 0;
            for (int i = 0; i < 4; i++) {
                addedAreas += GameMath.areaTriangle(points[i],points[(i+1)%4],p.toPoint());
            }
            contained = Math.abs(addedAreas - areaRectangle) < 4;
        }
        return contained;
    }

    private Point[] getCorners() {
        Point[] points = new Point[4];
        points[0] = getVector().getNormal().scaled(getHeight() / 2).applyTo(getVector().scaled(-1 * getWidth() / 2).applyTo(getPositionInRoom())).toPoint();
        points[1] = getVector().getNormal().scaled(-1 * getHeight() / 2).applyTo(getVector().scaled(-1 * getWidth() / 2).applyTo(getPositionInRoom())).toPoint();
        points[2] = getVector().getNormal().scaled(-1 * getHeight() / 2).applyTo(getVector().scaled(getWidth() / 2).applyTo(getPositionInRoom())).toPoint();
        points[3] = getVector().getNormal().scaled(getHeight() / 2).applyTo(getVector().scaled(getWidth() / 2).applyTo(getPositionInRoom())).toPoint();
        return points;
    }

    public MathVector getVector() {
        return vector;
    }

    public void setVector(MathVector vector) {
        this.vector = vector;
    }
}
