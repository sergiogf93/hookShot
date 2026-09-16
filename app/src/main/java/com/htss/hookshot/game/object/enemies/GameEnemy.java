package com.htss.hookshot.game.object.enemies;

import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameCharacter;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.math.MathVector;

import java.util.Random;

/**
 * Created by Sergio on 31/08/2016.
 */
public abstract class GameEnemy extends GameCharacter {

    // A hit knocks the enemy back, and it slows down to a halt before moving on its own again. Every hit knocks it
    // back again, so hitting it keeps it away
    private static final double KNOCKBACK_SPEED = MyActivity.TILE_WIDTH * 0.2, KNOCKBACK_END_SPEED = MyActivity.TILE_WIDTH * 0.01,
            KNOCKBACK_DECAY = 0.88;

    private Paint paint = new Paint();
    private MathVector targetPositionInRoom, currentDirection;
    // Its speed while knocked back
    private MathVector knockback;

    public GameEnemy(double xPos, double yPos, int mass, int collisionPriority, double maxVelocity, int maxHealth, boolean addToLists, boolean addToEnemyList) {
        super(xPos, yPos, mass, collisionPriority, maxVelocity, maxHealth, addToLists, addToLists);
        if (addToEnemyList) {
            MyActivity.enemies.add(this);
        }
    }

    // Pushed in the given direction, away from what hit it
    public void knockBack(MathVector direction) {
        if (!direction.isNull()) {
            knockback = direction.rescaled(KNOCKBACK_SPEED);
        }
    }

    public boolean isKnockedBack() {
        return knockback != null;
    }

    public MathVector getKnockback() {
        return knockback;
    }

    // Slows the knockback. Tells whether the enemy is still knocked back, and so shouldn't move on its own this update
    protected boolean updateKnockback() {
        if (knockback == null) {
            return false;
        }
        knockback.scale(KNOCKBACK_DECAY);
        if (knockback.magnitude() < KNOCKBACK_END_SPEED) {
            knockback = null;
            return false;
        }
        return true;
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
