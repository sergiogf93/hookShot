package com.htss.hookshot.constraints;

import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 04/08/2016.
 */
public class ChildOfConstraint extends Constraint {

    private GameDynamicObject parent;

    public ChildOfConstraint(GameDynamicObject parent) {
        this.parent = parent;
    }

    @Override
    public void check(GameDynamicObject constrained) {
        MathVector P = new MathVector(constrained.getPositionInRoom(),getParent().getPositionInRoom());
        constrained.setP(P);
    }

    public GameDynamicObject getParent() {
        return parent;
    }

    public void setParent(GameDynamicObject parent) {
        this.parent = parent;
    }
}
