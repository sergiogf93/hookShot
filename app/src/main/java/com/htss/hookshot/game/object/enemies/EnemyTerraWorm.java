package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.map.Cave;
import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.map.World;
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
    // A worm hunts the character while it's on the worm's own ground, and heads back there once it has left. In a game
    // that's the cave the worm was made in. The open world has no such bounds, so there it's this far round where the
    // worm was made, which keeps worms from trailing after the character from piece to piece, and from being on the
    // screen when their piece goes. When it isn't hunting it stays within the second distance of there
    private static final double TERRITORY = MyActivity.TILE_WIDTH * 12, ROAMING = MyActivity.TILE_WIDTH * 6;
    // In the open world only so many hunt at once, one more every so many levels down. The rest bide their time
    private static final int MOST_HUNTING = 3, LEVELS_PER_HUNTER = 10;

    private int frameWhenChangedDirection = 0;
    private int currentRotation = 0;
    private boolean attacking = false;
    private double maxDurationToChangeDirection = TimeUtil.secondsToUpdates(5);
    private Vector<TerraWormBody> bodyParts = new Vector<TerraWormBody>();
    private final MathVector home;
    // The way it's turning to head back home, which it keeps to until it faces there, or the other way if a border is
    // in the way. Taking the shorter turn each time left it going back and forth between the two, straight along the border
    private int turnHome = 0;

    public EnemyTerraWorm(double xPos, double yPos, int nParts, boolean addToLists, boolean addToEnemyList) {
        super(xPos, yPos, MASS, COLLISION_PRIORITY, MAX_VELOCITY, nParts, addToLists, addToEnemyList);
        home = new MathVector(xPos, yPos);
        for (int i = 0; i < nParts; i++) {
            bodyParts.add(new TerraWormBody(xPos, yPos, MAX_RADIUS * ((i + 1f) / nParts), this, false, addToEnemyList));
        }
        randomNewDirection();
    }

    @Override
    public void update() {
        if (getBodyParts().size() == 0) {
            die();
        } else if (updateKnockback()) {
            // Knocked back as a whole, slowing down to a halt
            for (TerraWormBody part : bodyParts) {
                part.setPositionInRoom(getKnockback().applyTo(part.getPositionInRoom()));
            }
            setPositionInRoom(bodyParts.lastElement().getPositionInRoom());
        } else {
            updateFrame();
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
            World.dig((float) getxPosInRoom(), (float) getyPosInRoom(), MAX_RADIUS);
            throwDebris();
        }
    }

    // Bits of rock thrown ahead of the head as it digs, every few updates, while it can be seen
    private void throwDebris() {
        if (getFrame() % 3 == 0 && MyActivity.isInScreen(getPositionInScreen())) {
            MathVector front = getCurrentDirection().rescaled(MAX_RADIUS).applyTo(getPositionInRoom());
            float angle = (float) Math.toDegrees(Math.atan2(getCurrentDirection().y, getCurrentDirection().x));
            int rock = CavePalette.forLevel(MyActivity.canvas.myActivity.level).rock;
            Particles.burst(front.x, front.y, 2, rock, 0.05f, 0.04f, 0.6, 0.004f, angle - 60, angle + 60);
        }
    }

    private void updateCurrentDirection() {
        if (getFrame() - frameWhenChangedDirection > maxDurationToChangeDirection) {
            currentRotation = changeRotation();
        }
        // One that's hunting already keeps its place among the hunters
        attacking = distanceTo(MyActivity.character) < DISTANCE_TO_ATTACK && isOnOwnGround(MyActivity.character.getPositionInRoom(), TERRITORY)
                && (attacking || canJoinTheHunt());
        int r = getRotationToAvoidBorder(getCurrentDirection(), MyActivity.TILE_WIDTH * 5, 90);
        if (r != 0) {
            rotate(r);
            currentRotation = 0;
            if (turnHome != 0) {
                turnHome = r;
            }
        } else {
            if (isAttacking()) {
                turnHome = 0;
                rotate(getRotationTowards(MyActivity.character.getPositionInRoom()));
            } else if (!isOnOwnGround(getPositionInRoom(), ROAMING)) {
                double towardsHome = getRotationTowards(home);
                if (towardsHome == 0 || turnHome == 0) {
                    turnHome = (int) towardsHome;
                }
                rotate(turnHome);
            } else {
                turnHome = 0;
                rotate(currentRotation);
            }
        }
    }

    // In the cave the worm was made in or, in the open world, within the given distance of where it was made
    private boolean isOnOwnGround(MathVector point, double distance) {
        if (World.isOpen()) {
            return point.distanceTo(home) < distance;
        }
        Cave cave = World.getCaveAt(home.x, home.y);
        return cave == null || cave.contains(point.x, point.y);
    }

    // Worms don't leave the cave they were made in, even with others next to it. The open world is all theirs, as far
    // as it has been made
    private boolean isBeyondBorder(MathVector point) {
        Cave cave = World.isOpen() ? null : World.getCaveAt(home.x, home.y);
        return (cave != null) ? !cave.contains(point.x, point.y) : !World.contains(point.x, point.y);
    }

    private boolean canJoinTheHunt() {
        if (!World.isOpen()) {
            return true;
        }
        int hunting = 0;
        for (GameEnemy enemy : MyActivity.enemies) {
            if (enemy != this && enemy instanceof EnemyTerraWorm && ((EnemyTerraWorm) enemy).isAttacking()) {
                hunting++;
            }
        }
        return hunting < Math.min(MOST_HUNTING, 1 + MyActivity.canvas.myActivity.level / LEVELS_PER_HUNTER);
    }

    private double getRotationTowards(MathVector point) {
        MathVector vector = new MathVector(getPositionInRoom(), point);
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
            if (isBeyondBorder(pointPositive)){
                return -5;
            }
            if (isBeyondBorder(pointNegative)){
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
