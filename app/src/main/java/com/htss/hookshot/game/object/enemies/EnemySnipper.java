package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

import java.util.Random;

/**
 * A crab that walks along rock, over floors, walls and ceilings. When the chain grips rock near it, it heads for the
 * hook, opens its claws, and snips the chain, throwing the character like letting go of it. A strike knocks it off the
 * rock onto its back for a moment.
 */
public class EnemySnipper extends ClickableEnemy {

    private static final int MAX_HEALTH = 2, DAMAGE = 5;
    // From its middle to the rock it walks on
    private static final float RADIUS = MyActivity.TILE_WIDTH * 0.3f;
    private static final double CRAWL_SPEED = MyActivity.TILE_WIDTH * 3.0 / MyActivity.UPDATES_PER_SECOND,
            WANDER_SPEED = MyActivity.TILE_WIDTH * 1.2 / MyActivity.UPDATES_PER_SECOND,
            NOTICE_DISTANCE = MyActivity.TILE_WIDTH * 6, SNIP_REACH = RADIUS * 2.6,
            KNOCK_SPEED = MyActivity.TILE_WIDTH * 0.12, GRAVITY = MyActivity.TILE_WIDTH / 100.0, MAX_FALL = MyActivity.TILE_WIDTH * 0.3;
    private static final int SNIP = (int) TimeUtil.secondsToUpdates(0.6), ON_ITS_BACK = (int) TimeUtil.secondsToUpdates(2),
            REST = (int) TimeUtil.secondsToUpdates(2), GIVE_UP = (int) TimeUtil.secondsToUpdates(4), TURN_AFTER = 10;
    private static final int CRAWLING = 0, HUNTING = 1, SNIPPING = 2, FALLING = 3, FLIPPED = 4;
    private static final int OUTLINE = Color.rgb(58, 22, 6), SHELL = Color.rgb(181, 85, 42), SHELL_LIGHT = Color.rgb(232, 147, 90),
            CLAW = Color.rgb(208, 106, 52), SHELL_LINE = Color.rgb(122, 46, 18), CALM_EYE = Color.rgb(255, 210, 63),
            ANGRY_EYE = Color.rgb(255, 74, 58), SNIP_GLOW = Color.rgb(255, 60, 40);
    // Drawn from a design in which its middle is 26 units from the rock
    private static final float SCALE = RADIUS / 26;

    private int state = CRAWLING, stateUpdates = 0, rest = 0, stalledUpdates = 0, wrongWayUpdates = 0, wanderUpdates = 0;
    private final RockCrawler crawler;
    private MathVector velocity = new MathVector(0, 0);
    private boolean stunOnLanding = false;
    private double bestDistance;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final Random random = new Random();

    public EnemySnipper(double xPos, double yPos) {
        super(xPos, yPos, 0, 0, 0, MAX_HEALTH, true, true);
        setGhost(true);
        crawler = new RockCrawler(RADIUS, random.nextBoolean() ? 1 : -1);
    }

    @Override
    public void update() {
        updateFrame();
        stateUpdates++;
        if (rest > 0) {
            rest--;
        }
        MathVector anchor = getAnchor();
        switch (state) {
            case CRAWLING:
                if (rest == 0 && anchor != null && distanceTo(anchor) < NOTICE_DISTANCE) {
                    changeState(HUNTING);
                    bestDistance = distanceTo(anchor);
                    stalledUpdates = 0;
                    break;
                }
                if (--wanderUpdates <= 0) {
                    crawler.setDirection(random.nextBoolean() ? 1 : -1);
                    wanderUpdates = (int) TimeUtil.secondsToUpdates(2 + 2 * random.nextDouble());
                }
                crawl(WANDER_SPEED);
                break;
            case HUNTING:
                if (anchor == null) {
                    changeState(CRAWLING);
                    break;
                }
                double distance = distanceTo(anchor);
                if (distance < SNIP_REACH) {
                    changeState(SNIPPING);
                    break;
                }
                // Gives up on a hook it can't get closer to, like one on other rock
                if (distance < bestDistance - 1) {
                    bestDistance = distance;
                    stalledUpdates = 0;
                } else if (++stalledUpdates > GIVE_UP) {
                    rest = REST;
                    changeState(CRAWLING);
                    break;
                }
                steerTowards(anchor);
                crawl(CRAWL_SPEED);
                break;
            case SNIPPING:
                if (anchor == null || distanceTo(anchor) > SNIP_REACH * 1.5) {
                    changeState((anchor == null) ? CRAWLING : HUNTING);
                } else if (stateUpdates >= SNIP) {
                    snip(anchor);
                }
                break;
            case FALLING:
                fall();
                break;
            case FLIPPED:
                if (stateUpdates >= ON_ITS_BACK) {
                    crawler.standUp();
                    changeState(CRAWLING);
                }
                break;
        }
    }

    private void changeState(int state) {
        this.state = state;
        stateUpdates = 0;
    }

    // Where the character's chain grips rock, if it does
    private static MathVector getAnchor() {
        Hook hook = MyActivity.character.getHook();
        if (hook == null || !hook.isHooked() || hook.getHookedObject() != null) {
            return null;
        }
        return hook.getHookedPoint();
    }

    // Walks the way along the rock that gets it closer, only turning round once it's sure, so it doesn't dither
    private void steerTowards(MathVector target) {
        MathVector tangent = new MathVector(-crawler.getNormal().y, crawler.getNormal().x);
        int wanted = (tangent.dotProduct(new MathVector(getPositionInRoom(), target)) >= 0) ? 1 : -1;
        if (wanted == crawler.getDirection()) {
            wrongWayUpdates = 0;
        } else if (++wrongWayUpdates > TURN_AFTER) {
            crawler.setDirection(wanted);
            wrongWayUpdates = 0;
        }
    }

    // Along the rock. With no rock around, it falls
    private void crawl(double speed) {
        if (!crawler.crawl(this, speed)) {
            startFalling(new MathVector(0, 0), false);
        }
    }

    private void snip(MathVector anchor) {
        MyActivity.character.removeHook();
        Particles.burst(anchor.x, anchor.y, 12, Color.rgb(255, 214, 120), 0.09f, 0.025f, 0.35, 0.003f);
        ScreenShake.shake(0.04f);
        rest = REST;
        changeState(CRAWLING);
    }

    private void startFalling(MathVector velocity, boolean stunOnLanding) {
        this.velocity = velocity;
        this.stunOnLanding = stunOnLanding;
        changeState(FALLING);
    }

    // Stopped by walls and ceilings, and landing on the floor, on its back if it was knocked off
    private void fall() {
        velocity.y = Math.min(velocity.y + GRAVITY, MAX_FALL);
        double x = getxPosInRoom(), y = getyPosInRoom();
        if (!isRock(x + velocity.x + Math.signum(velocity.x) * RADIUS, y)) {
            x += velocity.x;
        } else {
            velocity.x = 0;
        }
        if (velocity.y > 0 && isRock(x, y + velocity.y + RADIUS)) {
            double toRock = getDistanceToRock(new MathVector(x, y), new MathVector(0, 1), RADIUS + velocity.y + 2);
            if (toRock >= 0) {
                y += toRock - RADIUS;
            }
            setPositionInRoom(x, y);
            crawler.standUp();
            changeState(stunOnLanding ? FLIPPED : CRAWLING);
            return;
        }
        if (velocity.y < 0 && isRock(x, y + velocity.y - RADIUS)) {
            velocity.y = 0;
        } else {
            y += velocity.y;
        }
        setPositionInRoom(x, y);
    }

    // Off the rock and into the air
    @Override
    public void knockBack(MathVector direction) {
        MathVector push = direction.isNull() ? new MathVector(0, -1) : direction.getUnitVector();
        startFalling(new MathVector(push.x * KNOCK_SPEED, push.y * KNOCK_SPEED - GRAVITY * 4), true);
    }

    // Legs towards the rock and claws out, lit from the top left. Its eyes turn red while it goes for the chain, and
    // the link it's about to snip glows. On its back, its legs kick
    @Override
    public void draw(Canvas canvas) {
        MathVector anchor = getAnchor();
        if (state == SNIPPING && anchor != null) {
            MathVector link = anchor.roomToScreen();
            float pulse = 0.5f + 0.5f * (float) Math.sin(stateUpdates * 0.5);
            DrawUtil.drawGlow(canvas, glowPaint, (float) link.x, (float) link.y, RADIUS * 1.6f, DrawUtil.withAlpha(SNIP_GLOW, (int) (90 + 90 * pulse)));
        }
        beginHurtFlash(canvas);
        canvas.save();
        canvas.translate((float) getxPosInScreen(), (float) getyPosInScreen());
        canvas.rotate(crawler.getAngle());
        if (state == FLIPPED) {
            canvas.rotate(180);
        }
        canvas.scale(SCALE, SCALE);
        canvas.translate(-50, -60);
        drawLegs(canvas);
        float open = (state == SNIPPING) ? 22 * (float) Math.sin(Math.PI * Math.min(1, stateUpdates / (float) SNIP)) : 0;
        drawClaw(canvas, 27, 39, -1, open);
        drawClaw(canvas, 73, 39, 1, open);
        oval.set(26, 45, 74, 75);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(SHELL);
        canvas.drawOval(oval, paint);
        stroke(OUTLINE, 3);
        canvas.drawOval(oval, paint);
        path.reset();
        path.moveTo(32, 56);
        path.quadTo(38, 47, 52, 46);
        stroke(SHELL_LIGHT, 3.5f);
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(36, 64);
        path.quadTo(50, 70, 64, 64);
        stroke(SHELL_LINE, 2);
        canvas.drawPath(path, paint);
        stroke(OUTLINE, 3);
        canvas.drawLine(44, 48, 42, 38, paint);
        canvas.drawLine(56, 48, 58, 38, paint);
        boolean angry = state == HUNTING || state == SNIPPING;
        for (int s = -1; s <= 1; s += 2) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(angry ? ANGRY_EYE : CALM_EYE);
            canvas.drawCircle(50 + s * 8, 36, 3.5f, paint);
            stroke(OUTLINE, 1.5f);
            canvas.drawCircle(50 + s * 8, 36, 3.5f, paint);
        }
        canvas.restore();
        endHurtFlash(canvas);
    }

    // Walking, every other leg steps forward. On its back, they kick
    private void drawLegs(Canvas canvas) {
        int phase = (state == FLIPPED) ? (getFrame() / 4) % 2 : (int) (crawler.getWalked() / (RADIUS * 0.35)) % 2;
        stroke(OUTLINE, 3.5f);
        float[][] legs = {{34, 64, 20, 76}, {38, 68, 28, 84}, {44, 70, 40, 86}};
        for (int i = 0; i < legs.length; i++) {
            float step = ((i + phase) % 2 == 0) ? 4 : -4;
            float[] leg = legs[i];
            canvas.drawLine(leg[0], leg[1], leg[2] + step, leg[3], paint);
            canvas.drawLine(100 - leg[0], leg[1], 100 - leg[2] + step, leg[3], paint);
        }
    }

    // An arm up to a round hand with two jaws, which open by the given angle
    private void drawClaw(Canvas canvas, float x, float y, int side, float open) {
        stroke(OUTLINE, 3.5f);
        canvas.drawLine(50 + side * 12, 54, x, y + 3, paint);
        for (int jaw = 0; jaw < 2; jaw++) {
            canvas.save();
            canvas.rotate(((jaw == 0) ? side : -side) * open, x, y);
            path.reset();
            if (jaw == 0) {
                path.moveTo(x + side * 6, y - 3);
                path.lineTo(x + side * 17, y - 21);
                path.lineTo(x, y - 7);
            } else {
                path.moveTo(x - side * 4, y - 6);
                path.lineTo(x - side * 9, y - 25);
                path.lineTo(x - side * 7, y - 4);
            }
            path.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(CLAW);
            canvas.drawPath(path, paint);
            stroke(OUTLINE, 2.5f);
            paint.setStrokeJoin(Paint.Join.ROUND);
            canvas.drawPath(path, paint);
            canvas.restore();
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(CLAW);
        canvas.drawCircle(x, y, 8, paint);
        stroke(OUTLINE, 3);
        canvas.drawCircle(x, y, 8, paint);
    }

    private void stroke(int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setColor(color);
    }

    @Override
    public double getHurtDistance() {
        return (state == FLIPPED) ? 0 : RADIUS + MainCharacter.BODY_RADIUS * 0.6;
    }

    @Override
    public int getDamageDone() {
        return DAMAGE;
    }

    @Override
    public double getBodyRadius() {
        return RADIUS * 1.4;
    }

    @Override
    protected int getParticleColor() {
        return SHELL;
    }

    @Override
    protected int getCoinsDropped() {
        return 3;
    }

    @Override
    protected double getHealthDropChance() {
        return 0.15;
    }

    @Override
    protected double getPowerUpDropChance() {
        return 0.06;
    }

    @Override
    public int getWidth() {
        return (int) (RADIUS * 3);
    }

    @Override
    public int getHeight() {
        return (int) (RADIUS * 2);
    }
}
