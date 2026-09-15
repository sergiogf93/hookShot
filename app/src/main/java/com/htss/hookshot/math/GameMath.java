package com.htss.hookshot.math;

import android.graphics.Point;

/**
 * Created by Sergio on 15/03/2016.
 */
public abstract class GameMath {

    public static boolean ins(double min,double val,double max){
        return val >= min && val <= max;
    }

    public static Point positionReferenceTo(Point p1, Point p2){
        return new Point(p1.x-p2.x,p1.y-p2.y);
    }

    public static double linealValue (double min, double minVal, double max, double maxVal, double val){
        return minVal*(max-val)/(max-min) + maxVal*(min-val)/(min-max);
    }

    public static double areaTriangle(Point p1, Point p2, Point p3) {
        double a = (new MathVector(p1,p2)).magnitude();
        double b = (new MathVector(p2,p3)).magnitude();
        double c = (new MathVector(p3,p1)).magnitude();
        double s = (a + b + c) / 2;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }


    public static double areaRectangle(Point[] corners) {
        MathVector base = new MathVector(corners[1], corners[0]);
        MathVector height = new MathVector(corners[1], corners[2]);
        return base.magnitude() * height.magnitude();
    }

}
