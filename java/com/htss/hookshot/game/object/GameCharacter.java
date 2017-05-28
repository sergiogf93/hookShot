package com.htss.hookshot.game.object;

/**
 * Created by Sergio on 03/08/2016.
 */
public abstract class GameCharacter extends GameDynamicObject {

    public static final int STATE_REST = 0,
            STATE_MOVING = 1,
            STATE_FOLLOWING = 2,
            STATE_SEARCHING = 3;

    private int state = STATE_REST;

    public GameCharacter(double xPos, double yPos, int mass, int collisionPriority, double maxVelocity) {
        super(xPos, yPos, mass, collisionPriority, maxVelocity);
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean isInState(int state) {
        return getState() == state;
    }

}
