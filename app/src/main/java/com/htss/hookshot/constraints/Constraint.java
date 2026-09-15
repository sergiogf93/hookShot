package com.htss.hookshot.constraints;

import android.graphics.Point;

import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 29/07/2016.
 */
public abstract class Constraint {

    public abstract void check(GameDynamicObject constrained);

    protected MathVector applyConstraintToVector(MathVector vector, int maxDistance, MathVector pivotPoint, MathVector constrainedPoint){
        vector.rescale(maxDistance);
        MathVector newPos = vector.applyTo(pivotPoint);
        return new MathVector(constrainedPoint,newPos);
    }

}
