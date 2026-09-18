package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

/**
 * A stone cone growing out of the rock that never moves. When it sees the character, its mouth glows for a second and
 * it spits acid at where the character is, not where it's going, so moving dodges it. Its shell shrugs off strikes,
 * except while its mouth glows. Bombs always hurt it.
 */
public class EnemySpitter extends ClickableEnemy {

    private static final int MAX_HEALTH = 2, DAMAGE = 5;
    // From the rock to its mouth
    private static final float SIZE = MyActivity.TILE_WIDTH * 0.75f;
    private static final double SIGHT = MyActivity.TILE_WIDTH * 10;
    private static final int CHARGE = (int) TimeUtil.secondsToUpdates(1), COOLDOWN = (int) TimeUtil.secondsToUpdates(1.2);
    private static final int IDLE = 0, CHARGING = 1;
    private static final int STONE = Color.rgb(122, 112, 104), STONE_DARK = Color.rgb(42, 37, 33), STONE_LIGHT = Color.rgb(163, 154, 144),
            RIDGE = Color.rgb(82, 74, 67), MOUTH = Color.rgb(26, 20, 16), ACID_DIM = Color.rgb(46, 110, 34);
    // Drawn from a design in which it's 52 units from the rock to its mouth, pointing left
    private static final float SCALE = SIZE / 52;

    private int state = IDLE, stateUpdates = COOLDOWN;
    // Straight out of the rock
    private MathVector facing;
    private boolean settled = false;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();

    // Somewhere in front of rock, facing away from it
    public EnemySpitter(double xPos, double yPos, MathVector facing) {
        super(xPos, yPos, 0, 0, 0, MAX_HEALTH, true, true);
        setGhost(true);
        this.facing = facing.getUnitVector();
    }

    @Override
    public void update() {
        updateFrame();
        stateUpdates++;
        if (!settled) {
            settle();
        }
        if (state == IDLE && stateUpdates >= COOLDOWN && seesCharacter()) {
            changeState(CHARGING);
        } else if (state == CHARGING && stateUpdates >= CHARGE) {
            MathVector mouth = getMouth();
            new AcidGlob(mouth, MyActivity.character.getPositionInRoom());
            Particles.burst(mouth.x, mouth.y, 5, AcidGlob.ACID, 0.04f, 0.02f, 0.25, 0.002f);
            changeState(IDLE);
        }
    }

    private void changeState(int state) {
        this.state = state;
        stateUpdates = 0;
    }

    // Takes its place on the rock behind it, facing straight out of it, as the rock is only drawn once the cave is
    // built
    private void settle() {
        settled = true;
        MathVector back = facing.scaled(-1);
        double toRock = getDistanceToRock(getPositionInRoom(), back, MyActivity.TILE_WIDTH * 2);
        if (toRock < 0) {
            return;
        }
        MathVector surface = back.scaled(toRock).applyTo(getPositionInRoom());
        MathVector normal = getSurfaceNormal(surface, SIZE * 0.4);
        if (normal != null) {
            facing = normal;
        }
        setPositionInRoom(facing.scaled(SIZE * 0.5).applyTo(surface));
    }

    private MathVector getMouth() {
        return facing.scaled(SIZE * 0.5).applyTo(getPositionInRoom());
    }

    // In front of it, close enough, with no rock in between
    private boolean seesCharacter() {
        MathVector mouth = getMouth(), character = MyActivity.character.getPositionInRoom();
        MathVector toCharacter = new MathVector(mouth, character);
        return toCharacter.magnitude() < SIGHT && toCharacter.dotProduct(facing) > 0
                && isClearBetween(facing.scaled(SIZE * 0.2).applyTo(mouth), character);
    }

    @Override
    public void hit(MathVector from) {
        if (state == CHARGING) {
            super.hit(from);
        } else {
            deflect();
        }
    }

    // Stuck in the rock
    @Override
    public void knockBack(MathVector direction) {
    }

    // A stone cone lit from the top left, its mouth glowing brighter and opening wider as it gets ready to spit
    @Override
    public void draw(Canvas canvas) {
        beginHurtFlash(canvas);
        float x = (float) getxPosInScreen(), y = (float) getyPosInScreen();
        float charge = (state == CHARGING) ? Math.min(1, stateUpdates / (float) CHARGE) : 0;
        MathVector mouth = facing.scaled(SIZE * 0.5);
        DrawUtil.drawGlow(canvas, glowPaint, x + (float) mouth.x, y + (float) mouth.y, SIZE * (0.45f + 0.5f * charge),
                DrawUtil.withAlpha(AcidGlob.ACID, (int) (45 + 150 * charge)));
        double angle = Math.atan2(facing.y, facing.x) + Math.PI;
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate((float) Math.toDegrees(angle));
        // Keeps its lit side up, whichever way it faces
        if (Math.cos(angle) < 0) {
            canvas.scale(1, -1);
        }
        canvas.scale(SCALE, SCALE);
        canvas.translate(-58, -50);
        path.reset();
        path.moveTo(86, 18);
        path.quadTo(46, 22, 28, 50);
        path.quadTo(46, 78, 86, 82);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(STONE);
        canvas.drawPath(path, paint);
        stroke(STONE_DARK, 3);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(70, 22);
        path.quadTo(60, 50, 70, 78);
        path.moveTo(56, 28);
        path.quadTo(46, 50, 56, 72);
        stroke(RIDGE, 2.5f);
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(40, 36);
        path.quadTo(50, 26, 66, 24);
        stroke(STONE_LIGHT, 3);
        canvas.drawPath(path, paint);
        float open = 1 + 0.2f * charge;
        oval.set(32 - 9 * open, 50 - 14 * open, 32 + 9 * open, 50 + 14 * open);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(MOUTH);
        canvas.drawOval(oval, paint);
        stroke(STONE_DARK, 2.5f);
        canvas.drawOval(oval, paint);
        float acid = 0.6f + 0.6f * charge;
        oval.set(32 - 5 * acid, 50 - 8 * acid, 32 + 5 * acid, 50 + 8 * acid);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(DrawUtil.blend(ACID_DIM, AcidGlob.ACID, charge));
        canvas.drawOval(oval, paint);
        paint.setColor(AcidGlob.ACID_LIGHT);
        canvas.drawCircle(30, 46, 2, paint);
        canvas.restore();
        endHurtFlash(canvas);
    }

    private void stroke(int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setColor(color);
    }

    @Override
    public double getHurtDistance() {
        return SIZE * 0.5;
    }

    @Override
    public int getDamageDone() {
        return DAMAGE;
    }

    @Override
    public double getBodyRadius() {
        return SIZE * 0.5;
    }

    @Override
    protected int getParticleColor() {
        return STONE;
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
        return (int) SIZE;
    }

    @Override
    public int getHeight() {
        return (int) SIZE;
    }
}
