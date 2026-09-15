package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.animation.StalkerAnimation;
import com.htss.hookshot.game.object.GameCharacter;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 31/08/2016.
 */
public class EnemyStalker extends ClickableEnemy {

    private static final int COLLISION_PRIORITY = 0, MASS = 0, MAX_HEALTH = 5, MAX_VELOCITY = 10 * MyActivity.TILE_WIDTH / 100;

    private static final double THRESHOLD_DISTANCE = MyActivity.TILE_WIDTH * 6, SEARCHING_DISTANCE = MyActivity.TILE_WIDTH * 20;
    private static final int MAX_SEARCHING_TIME = (int) TimeUtil.convertSecondToGameSecond(10);
    private static final double MAX_RADIUS = MyActivity.TILE_WIDTH / 2, MIN_RADIUS = MyActivity.TILE_WIDTH / 10, MARGIN = MyActivity.TILE_WIDTH*0.8;

    private int frameWhenLost = 0, bodyColor = Color.BLACK;

    public EnemyStalker(double xPos, double yPos, boolean addToLists) {
        super(xPos, yPos, MASS, COLLISION_PRIORITY, MAX_VELOCITY, MAX_HEALTH, addToLists, addToLists);
        randomNewDirection();
    }

    @Override
    public void update() {
        manageActivateDeactivate();
        if (isInState(GameCharacter.STATE_FOLLOWING)) {
            follow();
        } else if (isInState(GameCharacter.STATE_SEARCHING)) {
            search();
        } else if (isInState(GameCharacter.STATE_REST)){
            wander();
        }
        super.update();
    }

    private void wander() {
        int detection = frontRadar(getCurrentDirection(),getMaxRadius()*1.5,70);
        rotate(-detection*3);
        setP(getCurrentDirection().rescaled(getMaxVelocity()/4));
    }

    private void search() {
        int detection = frontRadar(getCurrentDirection(),getMaxRadius()*1.5,180);
        rotate(-detection*3);
        setP(getCurrentDirection().rescaled(getMaxVelocity()*2/3));
    }

    private void follow() {
        MathVector sight = firstInSight(MyActivity.character);
        if (MyActivity.character.containsPoint(sight.x, sight.y)) {
            setTargetPositionInRoom(MyActivity.character.getPositionInRoom());
            MathVector p = new MathVector(getPositionInRoom(), sight);
            p.rescale(getMaxVelocity());
            setP(p);
        } else {
            setState(GameCharacter.STATE_SEARCHING);
            frameWhenLost = getFrame();
            setCurrentDirection(new MathVector(getPositionInRoom(),sight));
            setGhost(false);
        }
    }

    private void manageActivateDeactivate() {
        if (MyActivity.canvas.gameObjects.contains(MyActivity.character)) {
            if (!isInState(GameCharacter.STATE_FOLLOWING)) {
                if (distanceTo(MyActivity.character) < getSearchingDistance()) {
                    MathVector sight = firstInSight(MyActivity.character);
                    if (MyActivity.character.containsPoint(sight.x, sight.y)) {
                        setTargetPositionInRoom(MyActivity.character.getPositionInRoom());
                        setState(GameCharacter.STATE_FOLLOWING);
                        setGhost(true);
                    }
                }
            } else if (isInState(GameCharacter.STATE_SEARCHING)) {
                if (getFrame() - frameWhenLost > MAX_SEARCHING_TIME) {
                    setState(GameCharacter.STATE_REST);
                    randomNewDirection();
                    setGhost(false);
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int animatedAlpha = StalkerAnimation.getAlphaAnimated(getFrame());
        if (animatedAlpha > 245) {
            setBodyColor(Color.BLACK);
        }
        getPaint().setColor(bodyColor);
        getPaint().setAlpha(animatedAlpha);
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), getRadius(), getPaint());
        getPaint().setAlpha(255);
        if (isInState(GameCharacter.STATE_REST)) {
            getPaint().setColor(Color.YELLOW);
        } else {
            getPaint().setColor(Color.RED);
        }
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), (float) getMinRadius(), getPaint());
    }

    @Override
    public int getWidth() {
        return 2* (int) MAX_RADIUS;
    }

    @Override
    public int getHeight() {
        return 2* (int) MAX_RADIUS;
    }

    public float getRadius() {
        int freq = 18;
        int x = (getFrame() / MyActivity.FRAME_RATE) % freq;
        return (float) ((getMaxRadius() - getMinRadius()) * Math.sin(Math.PI * x / (2 * freq)) + getMinRadius());
    }

    public double getMaxRadius() {
        return MAX_RADIUS;
    }

    public double getMinRadius() {
        return MIN_RADIUS;
    }

    public double getSearchingDistance() {
        if (isInState(GameCharacter.STATE_REST)) {
            return THRESHOLD_DISTANCE;
        } else if (isInState(GameCharacter.STATE_SEARCHING)) {
            return SEARCHING_DISTANCE;
        } else {
            return 0;
        }
    }

    public int getMargin(){
//        return getWidth()/10;
        return 1;
    }

    public void setBodyColor(int bodyColor) {
        this.bodyColor = bodyColor;
    }

    @Override
    public double getHurtDistance() {
        return MAX_RADIUS*0.5;
    }

    @Override
    public int getDamageDone() {
        return 1;
    }

    @Override
    public void getHurt(int damage) {
        super.getHurt(damage);
        setBodyColor(Color.RED);
        setFrame(0);
        frameWhenLost = 0;
        setState(STATE_SEARCHING);
    }

    @Override
    public boolean pressed(double xScreen, double yScreen) {
        return getPositionInScreen().distanceTo(new MathVector(xScreen, yScreen)) <= MAX_RADIUS + MARGIN;
    }
}

