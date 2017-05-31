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
    private double health, maxHealth;

    public GameCharacter(double xPos, double yPos, int mass, int collisionPriority, double maxVelocity, int maxHealth) {
        super(xPos, yPos, mass, collisionPriority, maxVelocity);
        this.health = maxHealth;
        this.maxHealth = maxHealth;
    }

    public void getHurt(int damage) {
        setHealth(getHealth() - damage);
        if (getHealth() < 0) {
            this.die();
        }
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

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public abstract void die();
}
