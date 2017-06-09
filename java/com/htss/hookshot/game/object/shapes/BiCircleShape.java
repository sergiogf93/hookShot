package com.htss.hookshot.game.object.shapes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;

import java.math.MathContext;

/**
 * Created by Sergio on 24/08/2016.
 */
public class BiCircleShape extends GameShape {

    private double radius, separation;
    private MathVector direction;

    public BiCircleShape(double xPos, double yPos, double separation, MathVector direction, double radius, int color) {
        super(xPos, yPos, color, false);
        this.separation = separation;
        this.direction = direction.getUnitVector();
        this.radius = radius;
    }

    @Override
    public boolean intersect(GameShape shape) {
        return false;
    }

    @Override
    public boolean contains(MathVector p) {
        return getCenter1().distanceTo(p) <= getRadius() || getCenter2().distanceTo(p) <= getRadius();
    }

    @Override
    public int getIntersectMagnitude() {
        return Math.max(getWidth(),getHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(getColor());
        if (getSeparation() > getRadius()){
            Point[] points = new Point[4];
            points[0] = getDirection().getNormal().scaled(getRadius()).applyTo(getCenter1()).toPoint();
            points[1] = getDirection().getNormal().scaled(-getRadius()).applyTo(getCenter1()).toPoint();
            points[2] = getDirection().getNormal().scaled(getRadius()).applyTo(getCenter2()).toPoint();
            points[3] = getDirection().getNormal().scaled(-getRadius()).applyTo(getCenter2()).toPoint();
            DrawUtil.drawPolygon(points,canvas,getColor());
        }
        canvas.drawCircle((float)getCenter1().x,(float)getCenter1().y,(float)getRadius(),paint);
        canvas.drawCircle((float)getCenter2().x,(float)getCenter2().y,(float)getRadius(),paint);
    }

    @Override
    public int getWidth() {
        return (int) (Math.abs(getCenter1().x - getCenter2().x) + 2*getRadius());
    }

    @Override
    public int getHeight() {
        return (int) (Math.abs(getCenter1().y - getCenter2().y) + 2*getRadius());
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getSeparation() {
        return separation;
    }

    public void setSeparation(double separation) {
        this.separation = separation;
    }

    public MathVector getDirection() {
        return direction;
    }

    public void setDirection(MathVector direction) {
        this.direction = direction;
    }

    public MathVector getCenter1 (){
        return getDirection().scaled(getSeparation()/2).applyTo(getPositionInScreen());
    }

    public MathVector getCenter2 (){
        return getDirection().scaled(-getSeparation()/2).applyTo(getPositionInScreen());
    }
}
