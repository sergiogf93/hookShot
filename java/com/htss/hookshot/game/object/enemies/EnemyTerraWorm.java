package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

import java.util.Random;
import java.util.Vector;

/**
 * Created by Sergio on 16/06/2017.
 */
public class EnemyTerraWorm extends GameEnemy {

    private static final int COLLISION_PRIORITY = 0, MASS = 0, MAX_VELOCITY = 8 * MyActivity.TILE_WIDTH / 100;
    private static final float MAX_RADIUS = (float) (MyActivity.TILE_WIDTH * 0.8);
    private static final double DISTANCE_TO_ATTACK = MyActivity.TILE_WIDTH * 60;

    private int nParts, frameWhenChangedDirection = 0, currentRotation = 0;
    private boolean attacking = false;
    private double maxDurationToChangeDirection = TimeUtil.convertSecondToGameSecond(3);
    private Vector<TerraWormBody> bodyParts = new Vector<TerraWormBody>();

    public EnemyTerraWorm(double xPos, double yPos, int nParts, boolean addToLists, boolean addToEnemyList) {
        super(xPos, yPos, MASS, COLLISION_PRIORITY, MAX_VELOCITY, nParts, addToLists, addToEnemyList);
        this.nParts = nParts;
        for (int i = 0; i < nParts; i++) {
            bodyParts.add(new TerraWormBody(xPos, yPos, MAX_RADIUS * ((i + 1f) / nParts), this, false, addToEnemyList));
        }
        randomNewDirection();
    }

    @Override
    public void update() {
        updateFrame();
        if (getBodyParts().size() == 0) {
            die();
        } else {
            if (getBodyParts().size() > 1) {
                for (int i = 0; i < bodyParts.size() - 1; i++) {
                    TerraWormBody terraWormBody = bodyParts.get(i);
                    TerraWormBody nextTerraWormBody = bodyParts.get(i + 1);
                    if (terraWormBody.distanceTo(nextTerraWormBody) > (terraWormBody.getRadius() + nextTerraWormBody.getRadius()) * 0.7) {
                        MathVector buttPosition = nextTerraWormBody.getP().rescaled(-nextTerraWormBody.getRadius()).applyTo(nextTerraWormBody.getPositionInRoom());
                        MathVector direction = new MathVector(terraWormBody.getPositionInRoom(), buttPosition);
                        terraWormBody.setP(direction.rescaled(getMaxVelocity()));
                        terraWormBody.updatePosition();
                    }
                }
            }
            updateCurrentDirection();
            MathVector movement = getCurrentDirection().rescaled(getMaxVelocity());
            bodyParts.lastElement().setP(movement);
            bodyParts.lastElement().updatePosition();
            setPositionInRoom(bodyParts.lastElement().getPositionInRoom());
            MyActivity.canvas.clearCircle(MyActivity.canvas.mapBitmap, (float) getxPosInRoom(), (float) getyPosInRoom(), MAX_RADIUS);
        }
    }

    private void updateCurrentDirection() {
        if (getFrame() - frameWhenChangedDirection > maxDurationToChangeDirection) {
            currentRotation = changeRotation();
        }
        if (distanceTo(MyActivity.character) < DISTANCE_TO_ATTACK) {
            attacking = true;
        } else {
            attacking = false;
        }
        int r = getRotationToAvoidBorder(getCurrentDirection(), MyActivity.TILE_WIDTH * 5, 90);
        if (r != 0) {
            rotate(r);
            currentRotation = 0;
        } else {
            if (isAttacking()) {
                rotate(getRotationToAttack());
            } else {
                rotate(currentRotation);
            }
        }
    }

    private double getRotationToAttack() {
        MathVector vector = new MathVector(getPositionInRoom(), MyActivity.character.getPositionInRoom());
        double angle = getCurrentDirection().signedAngleDeg(vector);
        if (Math.abs(angle) > 5) {
            return -5 * Math.signum(getCurrentDirection().signedAngleDeg(vector));
        } else {
            return 0;
        }
    }

    private int changeRotation() {
        currentRotation = getRotationToAvoidBorder(getCurrentDirection(), MyActivity.TILE_WIDTH * 5, 90);
        if (currentRotation != 0) {
            return currentRotation;
        } else {
            frameWhenChangedDirection = getFrame();
            Random random = new Random();
            double r = random.nextDouble();
            if (r < 0.33) {
                return -5;
            } else if (r < 0.66) {
                return 5;
            } else {
                return 0;
            }
        }
    }

    protected int getRotationToAvoidBorder(MathVector direction, double distance, double angle){
        MathVector vector = direction.rescaled(distance);
        for (int i = 0 ; i < angle/2 ; i++){
            MathVector pointPositive = vector.rotatedDeg(angle/2 - i).applyTo(getPositionInRoom());
            MathVector pointNegative = vector.rotatedDeg(-angle/2 + i).applyTo(getPositionInRoom());
            if (!MyActivity.isInRoom(pointPositive.x,pointPositive.y)){
                return -5;
            }
            if (!MyActivity.isInRoom(pointNegative.x,pointNegative.y)){
                return +5;
            }
        }
        return 0;
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < bodyParts.size(); i++) {
            TerraWormBody terraWormBody = bodyParts.get(i);
            terraWormBody.draw(canvas);
        }
    }

    @Override
    public double getHurtDistance() {
        return 0;
    }

    @Override
    public int getDamageDone() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    public Vector<TerraWormBody> getBodyParts() {
        return bodyParts;
    }

    public void removeBodyPart(TerraWormBody body) {
        this.bodyParts.remove(body);
    }

    public boolean isAttacking() {
        return attacking;
    }
}
