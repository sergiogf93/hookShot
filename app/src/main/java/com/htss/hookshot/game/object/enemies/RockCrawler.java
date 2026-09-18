package com.htss.hookshot.game.object.enemies;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.math.MathVector;

/**
 * Walks an enemy along rock, over floors, walls and ceilings, like a ball of its height following the rock. It keeps
 * the way it last moved, and on every step tries turning towards the rock by a right angle, then less, then away from
 * it, and takes the first way with room for its body. So it hugs the rock, swings round outer corners and tips in a
 * step or two, goes up inner corners, and passes over gaps too narrow for it. It only walks while touching rock, and
 * falls until it touches some. It turns round at the cave's edges, where exits lead out.
 */
public class RockCrawler {

    // How far the ways it tries are apart, and how far towards the rock the first one turns, in degrees
    private static final int SWEEP_STEP = 15, SWEEP_START = 90, SWEEP_END = -180;
    // How many points around its body are checked for rock, and around it for rock it touches
    private static final int BODY_POINTS = 24, NEAR_POINTS = 32;

    // From its middle to the rock it walks on
    private final double height;
    // Straight out of the rock it walks on, eased so it turns smoothly when drawn
    private MathVector normal = new MathVector(0, -1);
    // The way it last moved, and whether it was walking on rock then
    private MathVector heading;
    private boolean onRock = false;
    // 1 or -1, which way along the rock it walks: 1 keeps the rock on its clockwise side, so it walks right on a floor
    private int direction;
    private double walked = 0;

    public RockCrawler(double height, int direction) {
        this.height = height;
        this.direction = direction;
    }

    // Moves the enemy a step along the rock. Tells whether it was touching rock to walk on
    public boolean crawl(GameEnemy enemy, double speed) {
        MathVector position = enemy.getPositionInRoom();
        if (hitsRock(position, height)) {
            // Partly inside rock, like where it's placed in the cave, whose rock is drawn closer than its height to the
            // middle of the cave's squares. Every step would hit rock, so it first gets out to the closest free spot
            MathVector free = getWayOut(position);
            if (free != null) {
                enemy.setPositionInRoom(free);
                position = free;
            }
        }
        if (!isTouchingRock(position, speed)) {
            onRock = false;
            return false;
        }
        if (!onRock || heading == null) {
            // Just landed, so it sets off along the rock it landed on
            MathVector away = getWayFromRock(position);
            if (away == null) {
                away = new MathVector(0, -1);
            }
            heading = new MathVector(-away.y * direction, away.x * direction);
            onRock = true;
        }
        for (int angle = SWEEP_START; angle >= SWEEP_END; angle -= SWEEP_STEP) {
            MathVector way = heading.rotatedDeg(angle * direction);
            MathVector next = way.scaled(speed).applyTo(position);
            if (hitsRock(next, height)) {
                continue;
            }
            if (leavesCave(next)) {
                // An exit, or the cave's edge: it turns round
                turnRound();
                return true;
            }
            enemy.setPositionInRoom(next);
            heading = way;
            walked += speed;
            // The rock is on the side it turned away from
            MathVector out = way.rotatedDeg(-90 * direction);
            MathVector eased = new MathVector(normal.x * 0.7 + out.x * 0.3, normal.y * 0.7 + out.y * 0.3);
            normal = eased.isNull() ? out : eased.getUnitVector();
            return true;
        }
        // Boxed in, so it tries the other way
        turnRound();
        return true;
    }

    private void turnRound() {
        direction = -direction;
        heading = heading.scaled(-1);
    }

    // Falls up to the given distance, stopping where its body touches rock
    public void fall(GameEnemy enemy, double distance) {
        MathVector position = enemy.getPositionInRoom();
        for (double fallen = 1; fallen <= distance; fallen++) {
            MathVector lower = new MathVector(position.x, position.y + 1);
            if (hitsRock(lower, height)) {
                break;
            }
            position = lower;
        }
        enemy.setPositionInRoom(position);
        standUp();
    }

    // Rock right against its body, or so close it can't fall even a little, like a thin spike's tip under it
    private boolean isTouchingRock(MathVector center, double speed) {
        return hitsRock(new MathVector(center.x, center.y + 1), height) || hitsRock(center, height + 2, NEAR_POINTS)
                || hitsRock(center, height + speed + 1, NEAR_POINTS);
    }

    private boolean hitsRock(MathVector center, double radius) {
        return hitsRock(center, radius, BODY_POINTS);
    }

    // Rock at any of the given number of points in a circle of the given radius. Outside the cave isn't rock: it's
    // where exits lead
    private boolean hitsRock(MathVector center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = i * 2 * Math.PI / points;
            double x = center.x + Math.cos(angle) * radius, y = center.y + Math.sin(angle) * radius;
            if (MyActivity.isInRoom(x, y) && GameEnemy.isRock(x, y)) {
                return true;
            }
        }
        return false;
    }

    // The closest point, up to twice its height away, where its body is clear of rock
    private MathVector getWayOut(MathVector center) {
        for (double distance = 2; distance <= height * 2; distance += 2) {
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI / 8;
                MathVector point = new MathVector(center.x + Math.cos(angle) * distance, center.y + Math.sin(angle) * distance);
                if (!hitsRock(point, height)) {
                    return point;
                }
            }
        }
        return null;
    }

    // Away from the rock around it, from the points in a circle a bit wider than its body that aren't rock. Null with no
    // rock around
    private MathVector getWayFromRock(MathVector center) {
        double reach = height * 1.6, x = 0, y = 0;
        int rock = 0;
        for (int i = 0; i < NEAR_POINTS; i++) {
            double angle = i * 2 * Math.PI / NEAR_POINTS, dx = Math.cos(angle), dy = Math.sin(angle);
            if (MyActivity.isInRoom(center.x + dx * reach, center.y + dy * reach) && GameEnemy.isRock(center.x + dx * reach, center.y + dy * reach)) {
                rock++;
            } else {
                x += dx;
                y += dy;
            }
        }
        if (rock == 0 || rock == NEAR_POINTS || (x == 0 && y == 0)) {
            return null;
        }
        return new MathVector(x, y).getUnitVector();
    }

    private boolean leavesCave(MathVector center) {
        return !MyActivity.isInRoom(center.x - height, center.y - height) || !MyActivity.isInRoom(center.x + height, center.y + height);
    }

    // Standing upright on a floor, as when it lands, from where it sets off along the rock it landed on
    public void standUp() {
        normal = new MathVector(0, -1);
        onRock = false;
    }

    public MathVector getNormal() {
        return normal;
    }

    // The angle in degrees that turns something drawn upright to stand on the rock
    public float getAngle() {
        return (float) Math.toDegrees(Math.atan2(normal.x, -normal.y));
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        if (direction != this.direction && heading != null) {
            heading = heading.scaled(-1);
        }
        this.direction = direction;
    }

    // How far it has walked, for animating legs
    public double getWalked() {
        return walked;
    }
}
