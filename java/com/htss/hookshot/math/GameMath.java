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

}
