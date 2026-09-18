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

    // Rock, or outside the cave, which enemies don't leave
    public static boolean isRock(double x, double y) {
        return !MyActivity.isInRoom(x, y) || Color.alpha(MyActivity.canvas.mapBitmap.getPixel((int) x, (int) y)) == 255;
    }

    // No rock on the straight line between two points in the room
    public static boolean isClearBetween(MathVector from, MathVector to) {
        MathVector line = new MathVector(from, to);
        double length = line.magnitude(), step = Math.max(2, MyActivity.TILE_WIDTH / 12.0);
        for (double along = step; along < length; along += step) {
            double t = along / length;
            if (isRock(from.x + line.x * t, from.y + line.y * t)) {
                return false;
            }
        }
        return true;
    }

    // The way out of the rock at a point, from which points in a circle around it are free. Null with no rock around,
    // or nothing but rock
    public static MathVector getSurfaceNormal(MathVector point, double radius) {
        double x = 0, y = 0;
        int free = 0;
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8, dx = Math.cos(angle), dy = Math.sin(angle);
            if (!isRock(point.x + dx * radius, point.y + dy * radius)) {
                x += dx;
                y += dy;
                free++;
            }
        }
        if (free == 0 || free == 16 || (x == 0 && y == 0)) {
            return null;
        }
        return new MathVector(x, y).getUnitVector();
    }

    // How far rock is from a point, looking the given way, or -1 if it's further than the given distance
    public static double getDistanceToRock(MathVector point, MathVector direction, double max) {
        MathVector unit = direction.getUnitVector();
        for (double along = 0; along <= max; along += 1) {
            if (isRock(point.x + unit.x * along, point.y + unit.y * along)) {
                return along;
            }
        }
        return -1;
    }

    // Seen by the enemy, closer than the given distance and with no rock in between
    protected boolean canSeeCharacter(double distance) {
        return distanceTo(MyActivity.character) < distance && isClearBetween(getPositionInRoom(), MyActivity.character.getPositionInRoom());
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
