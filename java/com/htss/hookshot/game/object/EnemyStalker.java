package com.htss.hookshot.game.object;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.ResourceCursorAdapter;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.animation.StalkerAnimation;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

import java.util.Random;

/**
 * Created by Sergio on 31/08/2016.
 */
public class EnemyStalker extends GameEnemy {

    private static final double THRESHOLD_DISTANCE = MyActivity.tileWidth * 3, SEARCHING_DISTANCE = MyActivity.tileWidth * 20;
    private static final int MAX_SEARCHING_TIME = (int) TimeUtil.convertSecondToGameSecond(10), MAX_HEALTH = 10, MAX_VELOCITY = 10, COLLISION_PRIORITY = 0, MASS = 0;
    private static final float maxRadius = MyActivity.tileWidth / 2, minRadius = MyActivity.tileWidth / 10;

    private MathVector targetPositionInRoom, currentDirection;
    private int frameWhenLost = 0;


    public EnemyStalker(double xPos, double yPos) {
        super(xPos, yPos, MASS, COLLISION_PRIORITY, MAX_VELOCITY, MAX_HEALTH);
        randomNewDirection();
    }

    @Override
    public void update() {
        manageActivateDeactivate();
        if (isInState(STATE_FOLLOWING)) {
            follow();
        } else if (isInState(STATE_SEARCHING)) {
            search();
        } else if (isInState(STATE_REST)){
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
            setState(STATE_SEARCHING);
            frameWhenLost = getFrame();
            setCurrentDirection(new MathVector(getPositionInRoom(),sight));
            setGhost(false);
        }
    }

    private void manageActivateDeactivate() {
        if (!isInState(STATE_FOLLOWING)) {
            if (distanceTo(MyActivity.character) < getSearchingDistance()) {
                MathVector sight = firstInSight(MyActivity.character);
                if (MyActivity.character.containsPoint(sight.x, sight.y)) {
                    setTargetPositionInRoom(MyActivity.character.getPositionInRoom());
                    setState(STATE_FOLLOWING);
                    setGhost(true);
                }
            }
        } else if (isInState(STATE_SEARCHING)) {
            if (getFrame() - frameWhenLost > MAX_SEARCHING_TIME) {
                setState(STATE_REST);
                randomNewDirection();
                setGhost(false);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAlpha(StalkerAnimation.getAlphaAnimated(getFrame()));
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), getRadius(), paint);
        paint.setAlpha(255);
        if (isInState(STATE_REST)) {
            paint.setColor(Color.YELLOW);
        } else {
            paint.setColor(Color.RED);
        }
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), getMinRadius(), paint);
        if (getTargetPositionInRoom() != null){
            paint.setColor(Color.MAGENTA);
            paint.setAlpha(255);
            MathVector p = getTargetPositionInRoom().roomToScreen();
            canvas.drawCircle((float)p.x, (float) p.y,10,paint);
        }
        if(!isInState(STATE_FOLLOWING)) {
            paint.setColor(Color.GREEN);
            canvas.drawLine((float) getPositionInScreen().x, (float) getPositionInScreen().y, (float) getCurrentDirection().rescaled(getMaxRadius() * 1.5).applyTo(getPositionInScreen()).x, (float) getCurrentDirection().rescaled(getMaxRadius() * 1.5).applyTo(getPositionInScreen()).y, paint);
        }
    }

    @Override
    public int getWidth() {
        return (int) (2 * getRadius());
    }

    @Override
    public int getHeight() {
        return (int) (2 * getRadius());
    }

    public float getRadius() {
        int freq = 18;
        int x = (getFrame() / MyActivity.FRAME_RATE) % freq;
        return (float) ((getMaxRadius() - getMinRadius()) * Math.sin(Math.PI * x / (2 * freq)) + getMinRadius());
    }

    public float getMaxRadius() {
        return maxRadius;
    }

    public float getMinRadius() {
        return minRadius;
    }

    public MathVector getTargetPositionInRoom() {
        return targetPositionInRoom;
    }

    public void setTargetPositionInRoom(MathVector targetPosition) {
        this.targetPositionInRoom = targetPosition;
    }

    public double getSearchingDistance() {
        if (isInState(STATE_REST)) {
            return THRESHOLD_DISTANCE;
        } else if (isInState(STATE_SEARCHING)) {
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
        return minRadius;
    }

    @Override
    public int getDamageDone() {
        return 1;
    }
}

