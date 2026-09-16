package com.htss.hookshot.game.object.enemies;

import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameCharacter;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

import java.util.Random;

/**
 * Created by Sergio on 31/08/2016.
 */
public abstract class GameEnemy extends GameCharacter {

    // Hit enemies stop for a moment, so they're easier to hit again and to get away from. Once they thaw, they can't
    // be frozen for a while, so they can't be kept frozen until they die
    private static final int FREEZE_UPDATES = (int) TimeUtil.secondsToUpdates(0.5),
            FREEZE_IMMUNITY_UPDATES = (int) TimeUtil.secondsToUpdates(1);
    // Least time between two hits from taps or the hook, so tapping faster doesn't kill faster
    private static final int HIT_COOLDOWN_UPDATES = (int) TimeUtil.secondsToUpdates(0.2);

    private Paint paint = new Paint();
    private MathVector targetPositionInRoom, currentDirection;
    private int frozenUpdates = 0, updatesUntilFreezable = 0, updatesUntilHittable = 0;

    public GameEnemy(double xPos, double yPos, int mass, int collisionPriority, double maxVelocity, int maxHealth, boolean addToLists, boolean addToEnemyList) {
        super(xPos, yPos, mass, collisionPriority, maxVelocity, maxHealth, addToLists, addToLists);
        if (addToEnemyList) {
            MyActivity.enemies.add(this);
        }
    }

    @Override
    public void getHurt(int damage) {
        super.getHurt(damage);
        freeze();
    }

    public void freeze() {
        if (updatesUntilFreezable == 0) {
            frozenUpdates = FREEZE_UPDATES;
            updatesUntilFreezable = FREEZE_UPDATES + FREEZE_IMMUNITY_UPDATES;
        }
    }

    public boolean isFrozen() {
        return frozenUpdates > 0;
    }

    public boolean canBeHit() {
        return updatesUntilHittable == 0;
    }

    public void startHitCooldown() {
        updatesUntilHittable = HIT_COOLDOWN_UPDATES;
    }

    // Counts the freeze and hit timers down, and tells whether this update is still frozen
    protected boolean tickTimers() {
        if (updatesUntilFreezable > 0) {
            updatesUntilFreezable--;
        }
        if (updatesUntilHittable > 0) {
            updatesUntilHittable--;
        }
        if (frozenUpdates > 0) {
            frozenUpdates--;
            return true;
        }
        return false;
    }

    public void randomNewDirection () {
        Random random = new Random();
        MathVector v = new MathVector(1,0);
        setCurrentDirection(v.rotatedDeg(random.nextInt(360)));
    }

    public void rotate(double deg){
        setCurrentDirection(getCurrentDirection().rotatedDeg(deg));
    }

    protected MathVector firstInSight(GameObject object) {
        MathVector vector = new MathVector(this.getPositionInRoom(), object.getPositionInRoom());
        double distance = vector.magnitude();
        vector.normalize();
        for (int i = 1 ; i < distance ; i++){
            MathVector point = vector.rescaled(i).applyTo(this.getPositionInRoom());
            if (MyActivity.isInRoom(point.x,point.y)){
                int pixel = MyActivity.canvas.mapBitmap.getPixel((int) point.x, (int) point.y);
                if (Color.alpha(pixel) == 255) {
                    return vector.applyTo(getPositionInRoom());
                }
            }
        }
        return object.getPositionInRoom();
    }

    protected int frontRadar(MathVector direction, double distance, double angle){
        MathVector vector = direction.rescaled(distance);
        for (int i = 0 ; i < angle/2 ; i++){
            MathVector pointPositive = vector.rotatedDeg(angle/2 - i).applyTo(getPositionInRoom());
            MathVector pointNegative = vector.rotatedDeg(-angle/2 + i).applyTo(getPositionInRoom());
            if (MyActivity.isInRoom(pointPositive.x,pointPositive.y)){
                int pixel = MyActivity.canvas.mapBitmap.getPixel((int)pointPositive.x, (int) pointPositive.y);
                if (Color.alpha(pixel) == 255){
                    return 1;
                }
            }
            if (MyActivity.isInRoom(pointNegative.x,pointNegative.y)){
                int pixel = MyActivity.canvas.mapBitmap.getPixel((int)pointNegative.x, (int) pointNegative.y);
                if (Color.alpha(pixel) == 255){
                    return -1;
                }
            }
        }
        return 0;
    }

    public void die() {
        this.destroy();
        MyActivity.character.checkIfRemoveInterest(this);
        MyActivity.dynamicObjects.remove(this);
        MyActivity.enemies.remove(this);
    }

    public MathVector getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentDirection(MathVector currentDirection) {
        this.currentDirection = currentDirection;
    }

    public MathVector getTargetPositionInRoom() {
        return targetPositionInRoom;
    }

    public void setTargetPositionInRoom(MathVector targetPosition) {
        this.targetPositionInRoom = targetPosition;
    }

    public Paint getPaint() {
        return paint;
    }

    public abstract double getHurtDistance();
    public abstract int getDamageDone();

}
