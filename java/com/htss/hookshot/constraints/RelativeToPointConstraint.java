package com.htss.hookshot.constraints;

import android.graphics.Point;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 29/07/2016.
 */
public class RelativeToPointConstraint extends Constraint {

    private MathVector point;
    private int maxDistance;

    public RelativeToPointConstraint(MathVector point, int maxDistance) {
        this.point = point;
        this.maxDistance = maxDistance;
    }

    @Override
    public void check(GameDynamicObject constrained) {
        MathVector futurePosInRoom = constrained.getFuturePositionInRoom();
        MathVector vector = new MathVector(point, futurePosInRoom);
        if (vector.magnitude() > maxDistance){
            constrained.setP(applyConstraintToVector(vector,maxDistance,point,constrained.getPositionInRoom()));
        }
    }

    public MathVector getPoint() {
        return point;
    }

    public void setPoint(MathVector point) {
        this.point = point;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }
}
