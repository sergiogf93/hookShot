package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.animation.StalkerAnimation;
import com.htss.hookshot.game.object.GameCharacter;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

import java.util.Random;

/**
 * Created by Sergio on 31/08/2016.
 */
public class EnemyStalker extends ClickableEnemy {

    private static final double THRESHOLD_DISTANCE = MyActivity.TILE_WIDTH * 3, SEARCHING_DISTANCE = MyActivity.TILE_WIDTH * 20;
    private static final int MAX_SEARCHING_TIME = (int) TimeUtil.convertSecondToGameSecond(10), MAX_HEALTH = 5, MAX_VELOCITY = 8, COLLISION_PRIORITY = 0, MASS = 0;
    private static final double MAX_RADIUS = MyActivity.TILE_WIDTH / 2, MIN_RADIUS = MyActivity.TILE_WIDTH / 10, MARGIN = MyActivity.TILE_WIDTH*0.8;

    private MathVector targetPositionInRoom, currentDirection;
    private int frameWhenLost = 0, bodyColor = Color.BLACK;
    private Paint paint = new Paint();


    public EnemyStalker(double xPos, double yPos) {
        super(xPos, yPos, MASS, COLLISION_PRIORITY, MAX_VELOCITY, MAX_HEALTH);
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

    @Override
    public void draw(Canvas canvas) {
        int animatedAlpha = StalkerAnimation.getAlphaAnimated(getFrame());
        if (animatedAlpha > 245) {
            setBodyColor(Color.BLACK);
        }
        paint.setColor(bodyColor);
        paint.setAlpha(animatedAlpha);
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), getRadius(), paint);
        paint.setAlpha(255);
        if (isInState(GameCharacter.STATE_REST)) {
            paint.setColor(Color.YELLOW);
        } else {
            paint.setColor(Color.RED);
        }
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), (float) getMinRadius(), paint);
        if (getTargetPositionInRoom() != null){
            paint.setColor(Color.MAGENTA);
            paint.setAlpha(255);
            MathVector p = getTargetPositionInRoom().roomToScreen();
            canvas.drawCircle((float)p.x, (float) p.y,10,paint);
        }
        if(!isInState(GameCharacter.STATE_FOLLOWING)) {
            paint.setColor(Color.GREEN);
            canvas.drawLine((float) getPositionInScreen().x, (float) getPositionInScreen().y, (float) getCurrentDirection().rescaled(getMaxRadius() * 1.5).applyTo(getPositionInScreen()).x, (float) getCurrentDirection().rescaled(getMaxRadius() * 1.5).applyTo(getPositionInScreen()).y, paint);
        }
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

    public MathVector getTargetPositionInRoom() {
        return targetPositionInRoom;
    }

    public void setTargetPositionInRoom(MathVector targetPosition) {
        this.targetPositionInRoom = targetPosition;
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
        return getWidth()/2;
    }

    public MathVector getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentDirection(MathVector currentDirection) {
        this.currentDirection = currentDirection;
    }

    public int getBodyColor() {
        return bodyColor;
    }

    public void setBodyColor(int bodyColor) {
        this.bodyColor = bodyColor;
    }

    public void rotate(double deg){
        setCurrentDirection(getCurrentDirection().rotatedDeg(deg));
    }

    public void randomNewDirection () {
        Random random = new Random();
        MathVector v = new MathVector(1,0);
        setCurrentDirection(v.rotatedDeg(random.nextInt(360)));
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
