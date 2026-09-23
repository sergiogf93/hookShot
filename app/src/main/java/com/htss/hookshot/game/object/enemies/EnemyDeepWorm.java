package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.map.World;
import com.htss.hookshot.game.hud.HUDNotification;
import com.htss.hookshot.game.object.interactables.Loot;
import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

import java.util.ArrayList;
import java.util.Random;

/**
 * The boss of every tenth level: a giant worm living in the rock around a wide cavern. Once the character comes into
 * the cavern, it rumbles in the rock, bursts out and lunges straight across at where the character was. At the far
 * side its head gets stuck for a moment and its tail glows: only then can the tail be hurt, and three strikes, or a
 * bomb, break it off. It burrows on and comes out somewhere else, faster with every part it loses. Beating it heals
 * the character fully and leaves a power-up.
 */
public class EnemyDeepWorm extends GameEnemy {

    public static final int DAMAGE = 10;
    private static final int SEGMENTS = 8, TAIL_HEALTH = 3, DEFEAT_COINS = 12;
    private static final float SEGMENT_RADIUS = MyActivity.TILE_WIDTH * 0.5f, HEAD_RADIUS = MyActivity.TILE_WIDTH * 0.62f;
    private static final double SPACING = SEGMENT_RADIUS * 1.2,
            LUNGE_SPEED = MyActivity.TILE_WIDTH * 10.0 / MyActivity.UPDATES_PER_SECOND, SPEED_UP = 0.1,
            WAKE_DISTANCE = MyActivity.TILE_WIDTH * 11, TOO_CLOSE = MyActivity.TILE_WIDTH * 4,
            OUT_OF_ITS_HOLE = MyActivity.TILE_WIDTH * 2, LONGEST_LUNGE = MyActivity.TILE_WIDTH * 30,
            // How steep a lunge it prefers at most, as the up or down share of its direction
            ACROSS = 0.5;
    private static final int RUMBLE = (int) TimeUtil.secondsToUpdates(1.1), STUCK_TIME = (int) TimeUtil.secondsToUpdates(2),
            HIDDEN_TIME = (int) TimeUtil.secondsToUpdates(1.2), BREAK_GUARD = (int) TimeUtil.secondsToUpdates(0.5),
            FLASH = (int) TimeUtil.secondsToUpdates(0.15);
    private static final int DORMANT = 0, HIDDEN = 1, RUMBLING = 2, LUNGING = 3, STUCK = 4, BURROWING = 5;

    // The middle of its cavern, and how far its walls can be from it
    private final MathVector center;
    private final double reach;
    // The head first
    private final ArrayList<DeepWormSegment> segments = new ArrayList<DeepWormSegment>();
    // Where the head has been, the oldest first, which the body follows
    private final ArrayList<MathVector> path = new ArrayList<MathVector>();
    private double pathLength = 0, traveled = 0;
    private MathVector direction = new MathVector(1, 0), emergence;
    private int state = DORMANT, stateUpdates = 0, tailHealth = TAIL_HEALTH, guard = 0, flash = 0, lost = 0;
    private final Random random = new Random();

    public EnemyDeepWorm(double xPos, double yPos, double reach) {
        super(xPos, yPos, 0, 0, LUNGE_SPEED, SEGMENTS, true, true);
        setGhost(true);
        this.center = new MathVector(xPos, yPos);
        this.reach = reach;
        for (int i = 0; i < SEGMENTS; i++) {
            segments.add(new DeepWormSegment(this, (i == 0) ? HEAD_RADIUS : SEGMENT_RADIUS * (1 - 0.04f * i)));
        }
    }

    @Override
    public void update() {
        updateFrame();
        stateUpdates++;
        if (guard > 0) {
            guard--;
        }
        if (flash > 0) {
            flash--;
        }
        switch (state) {
            case DORMANT:
                if (MyActivity.character.distanceTo(center) < WAKE_DISTANCE && isClearBetween(center, MyActivity.character.getPositionInRoom())) {
                    changeState(HIDDEN);
                }
                break;
            case HIDDEN:
                // Waits for the character to be in the cavern, where it can see it from a wall
                if (stateUpdates >= HIDDEN_TIME && stateUpdates % 10 == 0 && chooseWhereToComeOut()) {
                    changeState(RUMBLING);
                }
                break;
            case RUMBLING:
                rumble();
                if (stateUpdates >= RUMBLE) {
                    comeOut();
                }
                break;
            case LUNGING:
                moveHead();
                if (traveled > OUT_OF_ITS_HOLE && isRock(getHeadAhead().x, getHeadAhead().y)) {
                    getStuck();
                } else if (traveled > LONGEST_LUNGE) {
                    traveled = 0;
                    changeState(BURROWING);
                }
                break;
            case STUCK:
                if (stateUpdates % 20 == 0) {
                    debris(getHead(), direction.scaled(-1), 3);
                }
                if (stateUpdates >= STUCK_TIME) {
                    traveled = 0;
                    changeState(BURROWING);
                }
                break;
            case BURROWING:
                moveHead();
                // Until the tail is in the rock too
                if (traveled > getBodyLength() + HEAD_RADIUS * 2) {
                    changeState(HIDDEN);
                }
                break;
        }
        placeSegments();
        setPositionInRoom((state == RUMBLING) ? emergence : (isOut() ? getHead() : center));
    }

    private void changeState(int state) {
        this.state = state;
        stateUpdates = 0;
    }

    private boolean isOut() {
        return state == LUNGING || state == STUCK || state == BURROWING;
    }

    // Faster with every part it loses
    private double getSpeed() {
        return LUNGE_SPEED * (1 + SPEED_UP * lost);
    }

    private double getBodyLength() {
        return (segments.size() - 1) * SPACING;
    }

    private MathVector getHead() {
        return path.isEmpty() ? center : path.get(path.size() - 1);
    }

    private MathVector getHeadAhead() {
        return direction.scaled(HEAD_RADIUS).applyTo(getHead());
    }

    // A point on the cavern's wall it can see the character from, not too close to it, and the way to lunge from there.
    // It prefers lunging across rather than up or down, so it doesn't bury its head in the floor with its tail out of
    // sight, and the character can jump or swing over it
    private boolean chooseWhereToComeOut() {
        MathVector target = MyActivity.character.getPositionInRoom();
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            MathVector out = new MathVector(Math.cos(angle), Math.sin(angle));
            double toWall = getDistanceToRock(center, out, reach);
            if (toWall < 0) {
                continue;
            }
            MathVector wall = out.scaled(toWall).applyTo(center);
            MathVector inFront = out.scaled(-HEAD_RADIUS * 1.5).applyTo(wall);
            MathVector toTarget = new MathVector(wall, target);
            if (inFront.distanceTo(target) < TOO_CLOSE || !isClearBetween(inFront, target) || toTarget.getUnitVector().dotProduct(out) > -0.2
                    || (attempt < 36 && Math.abs(toTarget.getUnitVector().y) > ACROSS)) {
                continue;
            }
            emergence = wall;
            direction = toTarget.getUnitVector();
            return true;
        }
        return false;
    }

    // Dust and bits of rock fall where it's about to come out, and the cave shakes
    private void rumble() {
        if (stateUpdates % 4 == 0) {
            debris(emergence, direction, 2);
        }
        if (stateUpdates % 15 == 0) {
            ScreenShake.shake(0.03f);
        }
    }

    private void debris(MathVector at, MathVector towards, int amount) {
        float angle = (float) Math.toDegrees(Math.atan2(towards.y, towards.x));
        int rock = CavePalette.forLevel(MyActivity.canvas.myActivity.level).rock;
        Particles.burst(at.x, at.y, amount, rock, 0.06f, 0.04f, 0.6, 0.006f, angle - 50, angle + 50);
        Particles.burst(at.x, at.y, amount, CavePalette.forLevel(MyActivity.canvas.myActivity.level).dust, 0.03f, 0.03f, 0.8, 0.001f, angle - 70, angle + 70);
    }

    // Bursts out of the rock, the body behind it still inside
    private void comeOut() {
        World.dig((float) emergence.x, (float) emergence.y, HEAD_RADIUS * 1.35f);
        debris(emergence, direction, 14);
        ScreenShake.shake(0.1f);
        path.clear();
        path.add(direction.scaled(-(getBodyLength() + MyActivity.TILE_WIDTH * 2)).applyTo(emergence));
        path.add(emergence);
        pathLength = getBodyLength() + MyActivity.TILE_WIDTH * 2;
        traveled = 0;
        changeState(LUNGING);
    }

    // Its head digs into the far wall and gets stuck there
    private void getStuck() {
        MathVector head = getHead();
        World.dig((float) head.x, (float) head.y, HEAD_RADIUS * 1.15f);
        debris(getHeadAhead(), direction.scaled(-1), 12);
        ScreenShake.shake(0.12f);
        changeState(STUCK);
    }

    private void moveHead() {
        double speed = getSpeed();
        MathVector head = direction.scaled(speed).applyTo(getHead());
        path.add(head);
        pathLength += speed;
        traveled += speed;
        // Forgets where the head was once the tail is past it
        while (path.size() > 2 && pathLength - path.get(0).distanceTo(path.get(1)) > getBodyLength() + MyActivity.TILE_WIDTH * 2) {
            pathLength -= path.get(0).distanceTo(path.get(1));
            path.remove(0);
        }
    }

    // Every part the same distance behind the one before it, along the way the head went. Only the parts out of the
    // rock show
    private void placeSegments() {
        for (int i = 0; i < segments.size(); i++) {
            double behind = i * SPACING;
            MathVector position = getPointBehindHead(behind), ahead = getPointBehindHead(Math.max(0, behind - SPACING * 0.5));
            MathVector forward = new MathVector(position, ahead);
            boolean visible = isOut() && !path.isEmpty() && !isRock(position.x, position.y);
            segments.get(i).place(position, forward.isNull() ? direction : forward.getUnitVector(), visible);
        }
    }

    private MathVector getPointBehindHead(double distance) {
        if (path.isEmpty()) {
            return center;
        }
        double left = distance;
        for (int i = path.size() - 1; i > 0; i--) {
            MathVector a = path.get(i), b = path.get(i - 1);
            double length = a.distanceTo(b);
            if (left <= length && length > 0) {
                return new MathVector(a, b).scaled(left / length).applyTo(a);
            }
            left -= length;
        }
        return path.get(0);
    }

    private DeepWormSegment getTail() {
        return segments.get(segments.size() - 1);
    }

    // Only its head hurts, and only while it lunges, so the character can go up to it to hit its tail
    public boolean hurtsWith(DeepWormSegment segment) {
        return state == LUNGING && !segments.isEmpty() && segment == segments.get(0) && segment.isVisible();
    }

    // Only the tail, while its head is stuck. Tells whether the strike hurt it
    public boolean hitSegment(DeepWormSegment segment) {
        if (segment != getTail() || state != STUCK || guard > 0) {
            return false;
        }
        flash = FLASH;
        Particles.burst(segment.getxPosInRoom(), segment.getyPosInRoom(), 8, segment.getParticleColor(), 0.07f, 0.03f, 0.4, 0.003f);
        if (--tailHealth <= 0) {
            breakTail();
        }
        return true;
    }

    // A bomb breaks off the tail wherever it hits the worm, but only one part at a time
    public void bombSegment(DeepWormSegment segment) {
        if (segment.isVisible() && guard == 0) {
            breakTail();
        }
    }

    private void breakTail() {
        DeepWormSegment tail = getTail();
        segments.remove(tail);
        tail.die();
        lost++;
        tailHealth = TAIL_HEALTH;
        guard = BREAK_GUARD;
        ScreenShake.shake(0.1f);
        if (segments.isEmpty()) {
            defeat(tail.getPositionInRoom());
        }
    }

    // Heals the character fully and leaves a pile of coins and a power-up where it died. Its gate opens as it's gone
    private void defeat(MathVector at) {
        die();
        ScreenShake.shake(0.2f);
        Particles.burst(at.x, at.y, 30, CavePalette.forLevel(MyActivity.canvas.myActivity.level).rock, 0.12f, 0.06f, 0.9, 0.006f);
        MyActivity.character.addHealth(MyActivity.character.getMaxHealth());
        MyActivity.notifications.add(new HUDNotification("DEEP WORM DEFEATED!", TimeUtil.secondsToUpdates(2)));
        Loot.drop(at.x, at.y, DEFEAT_COINS, 0, 1);
    }

    // How much of it is left, for its bar at the top of the screen
    public float getHealthShare() {
        return (segments.size() - 1 + tailHealth / (float) TAIL_HEALTH) / SEGMENTS;
    }

    // Once the character met it
    public boolean isAwake() {
        return state != DORMANT;
    }

    // The tail first, so the head is on top
    @Override
    public void draw(Canvas canvas) {
        for (int i = segments.size() - 1; i >= 0; i--) {
            DeepWormSegment segment = segments.get(i);
            if (segment.isVisible()) {
                boolean tail = i == segments.size() - 1;
                segment.draw(canvas, i == 0, tail && state == STUCK, tail ? flash / (float) FLASH : 0);
            }
        }
    }

    @Override
    public double getHurtDistance() {
        return 0;
    }

    @Override
    public int getDamageDone() {
        return DAMAGE;
    }

    @Override
    public int getWidth() {
        return (int) (HEAD_RADIUS * 2);
    }

    @Override
    public int getHeight() {
        return (int) (HEAD_RADIUS * 2);
    }
}
