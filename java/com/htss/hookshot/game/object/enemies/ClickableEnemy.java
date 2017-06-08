package com.htss.hookshot.game.object.enemies;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Clickable;

/**
 * Created by Sergio on 31/05/2017.
 */
public abstract class ClickableEnemy extends GameEnemy implements Clickable {

    private boolean clickable = true, on = false;
    private int touchId = -1, touchIndex = -1;

    public ClickableEnemy(double xPos, double yPos, int mass, int collisionPriority, double maxVelocity, int maxHealth, boolean addToLists, boolean addToEnemyList) {
        super(xPos, yPos, mass, collisionPriority, maxVelocity, maxHealth, addToLists, addToEnemyList);
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setTouchIndex(index);
        setTouchId(id);
        setOn(true);
        this.getHurt(1);
    }

    @Override
    public void reset() {
        setTouchIndex(-1);
        setTouchId(-1);
        setOn(false);
    }

    @Override
    public boolean isOn() {
        return on;
    }

    @Override
    public boolean isClickable() {
        return clickable;
    }

    @Override
    public void setClickable(boolean bool) {
        this.clickable = bool;
    }

    @Override
    public int getTouchId() {
        return touchId;
    }

    @Override
    public int getTouchIndex() {
        return touchIndex;
    }

    public void setTouchId(int touchId) {
        this.touchId = touchId;
    }

    public void setTouchIndex(int touchIndex) {
        this.touchIndex = touchIndex;
    }

    public void setOn(boolean on) {
        this.on = on;
    }
}
