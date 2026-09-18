package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

import java.util.Random;

/**
 * Sleeps hanging from a ceiling, near others, and wakes when it sees the character. Awake, it flutters above the
 * character, and dives at it while it hangs from the chain or flies through the air, never while it stands. It warns
 * before diving, its eyes turning red, and one strike kills it.
 */
public class EnemyBat extends ClickableEnemy {

    private static final int MAX_HEALTH = 1, DAMAGE = 5;
    private static final float BODY_RADIUS = MyActivity.TILE_WIDTH * 0.2f;
    private static final double WAKE_DISTANCE = MyActivity.TILE_WIDTH * 7, DIVE_DISTANCE = MyActivity.TILE_WIDTH * 6,
            WAKE_OTHERS_DISTANCE = MyActivity.TILE_WIDTH * 4,
            HOVER_HEIGHT = MyActivity.TILE_WIDTH * 2.5, HOVER_SIDE = MyActivity.TILE_WIDTH * 2,
            FLY_SPEED = MyActivity.TILE_WIDTH * 5.0 / MyActivity.UPDATES_PER_SECOND,
            DIVE_SPEED = MyActivity.TILE_WIDTH * 14.0 / MyActivity.UPDATES_PER_SECOND,
            DIVE_OVERSHOOT = MyActivity.TILE_WIDTH * 2;
    private static final int WIND_UP = (int) TimeUtil.secondsToUpdates(0.5), COOLDOWN = (int) TimeUtil.secondsToUpdates(1.5),
            DAZED = (int) TimeUtil.secondsToUpdates(0.5), GIVE_UP = (int) TimeUtil.secondsToUpdates(4);
    private static final int SLEEPING = 0, HOVERING = 1, WINDING_UP = 2, DIVING = 3;
    private static final int OUTLINE = Color.rgb(20, 10, 34), WING = Color.rgb(67, 36, 95), FUR = Color.rgb(106, 61, 148),
            FUR_LIGHT = Color.rgb(167, 127, 212), CALM_EYE = Color.rgb(255, 210, 63), ANGRY_EYE = Color.rgb(255, 74, 58);
    // Drawn from a design 100 units wide, in which its body is 32 units tall
    private static final float SCALE = BODY_RADIUS / 16;

    private int state = SLEEPING, stateUpdates = 0, cooldown = 0, unseenUpdates = 0, side;
    private MathVector velocity = new MathVector(0, 0), diveDirection = new MathVector(0, 1);
    private double diveLeft;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final Random random = new Random();

    public EnemyBat(double xPos, double yPos) {
        super(xPos, yPos, 0, 0, DIVE_SPEED, MAX_HEALTH, true, true);
        setGhost(true);
        side = random.nextBoolean() ? 1 : -1;
        setFrame(random.nextInt(60));
    }

    @Override
    public void update() {
        updateFrame();
        stateUpdates++;
        if (cooldown > 0) {
            cooldown--;
        }
        switch (state) {
            case SLEEPING:
                hangFromCeiling();
                if (canSeeCharacter(WAKE_DISTANCE)) {
                    wake();
                }
                break;
            case HOVERING:
                unseenUpdates = canSeeCharacter(WAKE_DISTANCE * 1.5) ? 0 : unseenUpdates + 1;
                if (unseenUpdates > GIVE_UP) {
                    // Lost it, so it flies up to sleep again
                    changeState(SLEEPING);
                    break;
                }
                hover();
                if (cooldown == 0 && !MyActivity.character.isOnFloor() && canSeeCharacter(DIVE_DISTANCE)) {
                    changeState(WINDING_UP);
                }
                break;
            case WINDING_UP:
                if (stateUpdates >= WIND_UP) {
                    // Aims at where the character is, and goes a bit past it
                    MathVector toCharacter = new MathVector(getPositionInRoom(), MyActivity.character.getPositionInRoom());
                    diveDirection = toCharacter.isNull() ? new MathVector(0, 1) : toCharacter.getUnitVector();
                    diveLeft = toCharacter.magnitude() + DIVE_OVERSHOOT;
                    changeState(DIVING);
                }
                break;
            case DIVING:
                dive();
                break;
        }
    }

    private void changeState(int state) {
        this.state = state;
        stateUpdates = 0;
    }

    // Wakes the bats sleeping around it too
    private void wake() {
        changeState(HOVERING);
        unseenUpdates = 0;
        cooldown = COOLDOWN / 2;
        for (GameEnemy enemy : MyActivity.enemies) {
            if (enemy instanceof EnemyBat && ((EnemyBat) enemy).state == SLEEPING && distanceTo(enemy) < WAKE_OTHERS_DISTANCE) {
                ((EnemyBat) enemy).wake();
            }
        }
    }

    // Asleep, it hangs right under the ceiling, flying up to it when it gave up on the character
    private void hangFromCeiling() {
        velocity = new MathVector(0, 0);
        if (!isRock(getxPosInRoom(), getyPosInRoom() - BODY_RADIUS * 2.6)) {
            move(0, -FLY_SPEED * 0.6);
        }
    }

    // Flutters to a spot above the character, off to one side, bobbing, and sometimes changes sides
    private void hover() {
        if (random.nextInt((int) TimeUtil.secondsToUpdates(3)) == 0) {
            side = -side;
        }
        MathVector character = MyActivity.character.getPositionInRoom();
        double bob = Math.sin(getFrame() * 0.08) * BODY_RADIUS * 2;
        MathVector toSpot = new MathVector(getPositionInRoom(), new MathVector(character.x + side * HOVER_SIDE, character.y - HOVER_HEIGHT + bob));
        velocity = velocity.scaled(0.9).add(toSpot.scaled(0.012));
        if (velocity.magnitude() > FLY_SPEED) {
            velocity = velocity.rescaled(FLY_SPEED);
        }
        move(velocity.x, velocity.y);
    }

    // Straight on, until it has gone past where the character was, or it hits rock, which dazes it for a moment
    private void dive() {
        boolean moved = move(diveDirection.x * DIVE_SPEED, diveDirection.y * DIVE_SPEED);
        diveLeft -= DIVE_SPEED;
        if (!moved) {
            Particles.burst(getxPosInRoom() + diveDirection.x * BODY_RADIUS, getyPosInRoom() + diveDirection.y * BODY_RADIUS, 6,
                    Color.rgb(200, 180, 150), 0.05f, 0.02f, 0.3, 0.003f);
            cooldown = COOLDOWN + DAZED;
            changeState(HOVERING);
        } else if (diveLeft <= 0) {
            cooldown = COOLDOWN;
            changeState(HOVERING);
        }
        if (state == HOVERING) {
            velocity = diveDirection.scaled(FLY_SPEED);
        }
    }

    // Moves unless rock is in the way, sliding along it. Tells whether it moved all the way
    private boolean move(double dx, double dy) {
        double x = getxPosInRoom(), y = getyPosInRoom();
        if (!isBlocked(x + dx, y + dy)) {
            setPositionInRoom(x + dx, y + dy);
            return true;
        }
        if (!isBlocked(x + dx, y)) {
            setPositionInRoom(x + dx, y);
            velocity.y = 0;
        } else if (!isBlocked(x, y + dy)) {
            setPositionInRoom(x, y + dy);
            velocity.x = 0;
        } else {
            velocity = new MathVector(0, 0);
        }
        return false;
    }

    private static boolean isBlocked(double x, double y) {
        return isRock(x, y) || isRock(x + BODY_RADIUS, y) || isRock(x - BODY_RADIUS, y) || isRock(x, y + BODY_RADIUS) || isRock(x, y - BODY_RADIUS);
    }

    // Lit from the top left, with glowing eyes, yellow and red once it's about to dive. Asleep, it hangs wrapped in its
    // wings with its eyes shut. Diving, its wings are swept back
    @Override
    public void draw(Canvas canvas) {
        beginHurtFlash(canvas);
        float x = (float) getxPosInScreen(), y = (float) getyPosInScreen();
        boolean angry = state == WINDING_UP || state == DIVING;
        if (state != SLEEPING) {
            DrawUtil.drawGlow(canvas, glowPaint, x, y, BODY_RADIUS * 3, DrawUtil.withAlpha(angry ? ANGRY_EYE : CALM_EYE, 60));
        }
        canvas.save();
        canvas.translate(x, y);
        if (state == WINDING_UP) {
            canvas.translate((float) Math.sin(stateUpdates * 2.1) * BODY_RADIUS * 0.12f, 0);
        } else if (state == DIVING) {
            canvas.rotate((float) Math.toDegrees(Math.atan2(diveDirection.y, diveDirection.x)) - 90);
        }
        canvas.scale(SCALE, SCALE);
        canvas.translate(-50, -52);
        if (state == SLEEPING) {
            drawAsleep(canvas);
        } else {
            drawAwake(canvas, angry);
        }
        canvas.restore();
        endHurtFlash(canvas);
    }

    private void drawAwake(Canvas canvas, boolean angry) {
        float flap = angry ? 1 : (float) Math.cos(getFrame() * 0.55);
        for (int s = -1; s <= 1; s += 2) {
            canvas.save();
            canvas.scale(-s, 1, 50, 0);
            canvas.scale(1, flap, 46, 50);
            path.reset();
            path.moveTo(46, 50);
            if (angry) {
                path.quadTo(40, 34, 24, 20);
                path.quadTo(26, 32, 22, 38);
                path.quadTo(30, 40, 30, 48);
                path.quadTo(36, 46, 38, 56);
            } else {
                path.quadTo(34, 30, 8, 28);
                path.quadTo(14, 36, 12, 42);
                path.quadTo(20, 40, 22, 48);
                path.quadTo(30, 44, 32, 54);
            }
            path.quadTo(40, 50, 46, 58);
            path.close();
            fillAndOutline(canvas, WING, 3);
            canvas.restore();
        }
        path.reset();
        path.moveTo(40, 40);
        path.lineTo(38, 26);
        path.lineTo(47, 36);
        path.close();
        path.moveTo(60, 40);
        path.lineTo(62, 26);
        path.lineTo(53, 36);
        path.close();
        fillAndOutline(canvas, FUR, 2.5f);
        path.reset();
        oval.set(36, 36, 64, 68);
        path.addOval(oval, Path.Direction.CW);
        fillAndOutline(canvas, FUR, 3);
        stroke(canvas, FUR_LIGHT, 3);
        path.reset();
        path.moveTo(40, 46);
        path.quadTo(43, 38, 51, 37);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(angry ? ANGRY_EYE : CALM_EYE);
        canvas.drawCircle(45, 50, angry ? 3.5f : 3, paint);
        canvas.drawCircle(55, 50, angry ? 3.5f : 3, paint);
        path.reset();
        path.moveTo(46, 60);
        path.lineTo(47.5f, 64);
        path.lineTo(49, 60);
        path.close();
        path.moveTo(51, 60);
        path.lineTo(52.5f, 64);
        path.lineTo(54, 60);
        path.close();
        paint.setColor(Color.WHITE);
        canvas.drawPath(path, paint);
    }

    private void drawAsleep(Canvas canvas) {
        stroke(canvas, OUTLINE, 3);
        canvas.drawLine(44, 10, 46, 20, paint);
        canvas.drawLine(56, 10, 54, 20, paint);
        path.reset();
        path.moveTo(50, 18);
        path.quadTo(30, 22, 30, 50);
        path.quadTo(32, 76, 50, 84);
        path.quadTo(68, 76, 70, 50);
        path.quadTo(70, 22, 50, 18);
        path.close();
        fillAndOutline(canvas, WING, 3);
        stroke(canvas, FUR, 3);
        path.reset();
        path.moveTo(36, 36);
        path.quadTo(40, 26, 50, 24);
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(42, 80);
        path.lineTo(40, 92);
        path.lineTo(47, 84);
        path.close();
        path.moveTo(58, 80);
        path.lineTo(60, 92);
        path.lineTo(53, 84);
        path.close();
        fillAndOutline(canvas, FUR, 2);
        stroke(canvas, CALM_EYE, 2);
        path.reset();
        path.moveTo(42, 68);
        path.quadTo(45, 71, 48, 68);
        path.moveTo(52, 68);
        path.quadTo(55, 71, 58, 68);
        canvas.drawPath(path, paint);
    }

    private void fillAndOutline(Canvas canvas, int color, float width) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(path, paint);
        stroke(canvas, OUTLINE, width);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(path, paint);
    }

    private void stroke(Canvas canvas, int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setColor(color);
    }

    @Override
    public double getHurtDistance() {
        return (state == SLEEPING) ? 0 : BODY_RADIUS + MainCharacter.BODY_RADIUS * 0.7;
    }

    @Override
    public int getDamageDone() {
        return DAMAGE;
    }

    @Override
    public double getBodyRadius() {
        return BODY_RADIUS * 1.5;
    }

    @Override
    protected int getParticleColor() {
        return FUR;
    }

    @Override
    public int getWidth() {
        return (int) (BODY_RADIUS * 5);
    }

    @Override
    public int getHeight() {
        return (int) (BODY_RADIUS * 3);
    }
}
