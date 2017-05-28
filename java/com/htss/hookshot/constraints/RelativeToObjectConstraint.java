package com.htss.hookshot.constraints;

import android.graphics.Color;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.Circle;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.Line;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 29/07/2016.
 */
public class RelativeToObjectConstraint extends Constraint{

    private GameDynamicObject object;
    private int maxDistance;

    public RelativeToObjectConstraint(GameDynamicObject object, int maxDistance) {
        this.object = object;
        this.maxDistance = maxDistance;
    }

    @Override
    public void check(GameDynamicObject constrained) {
        double newX = constrained.getxPosInRoom()+constrained.getP().x;
        double newY = constrained.getyPosInRoom()+constrained.getP().y;
        MathVector point = object.getP().applyTo(object.getPositionInRoom());
        MathVector vector = new MathVector(newX-point.x,newY-point.y);
        if (vector.magnitude() > maxDistance){
            object.setP(object.getP().add(constrained.getP()));
            object.update();
            if(object.getP().isNull()){
                constrained.setP(applyConstraintToVector(vector,maxDistance,point,constrained.getPositionInRoom()));
            } else {
                constrained.setP(applyConstraintToVector(vector,maxDistance,object.getP().applyTo(point),constrained.getPositionInRoom()));
            }
        }
        MyActivity.canvas.debugObjects.add(new Circle((int)newX,(int)newY,0,1,5, Color.YELLOW));
        MyActivity.canvas.debugObjects.add(new Line(object.getxPosInRoom(),object.getyPosInRoom(),constrained.getxPosInRoom(),constrained.getyPosInRoom()));
    }

    public GameDynamicObject getObject() {
        return object;
    }

    public void setObject(GameDynamicObject object) {
        this.object = object;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }
}
