package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.math.MathVector;

import java.util.Random;

/**
 * A heavy beetle with a stone shell that walks slowly round the cave, over floors, walls and ceilings. It's too heavy
 * to be knocked back and takes several strikes, and running into it hurts more than other enemies.
 */
public class EnemyBeetle extends ClickableEnemy {

    private static final int MAX_HEALTH = 4, DAMAGE = 10;
    private static final float WIDTH = MyActivity.TILE_WIDTH * 0.9f, HEIGHT = MyActivity.TILE_WIDTH * 0.6f;
    private static final double WALK_SPEED = MyActivity.TILE_WIDTH * 2.0 / MyActivity.UPDATES_PER_SECOND,
            GRAVITY = MyActivity.TILE_WIDTH / 100.0, MAX_FALL = MyActivity.TILE_WIDTH * 0.25;
    private static final int OUTLINE = Color.rgb(21, 24, 29), SHELL = Color.rgb(91, 98, 112), SHELL_LIGHT = Color.rgb(163, 171, 186),
            SEAM = Color.rgb(58, 63, 74), HORN = Color.rgb(138, 145, 160), EYE = Color.rgb(255, 90, 58);
    // Drawn from a design in which its body is 84 units wide, facing right, with its feet at the bottom
    private static final float SCALE = WIDTH / 84;

    private final RockCrawler crawler;
    private double fallSpeed = 0;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public EnemyBeetle(double xPos, double yPos) {
        super(xPos, yPos, 0, 0, 0, MAX_HEALTH, true, true);
        setGhost(true);
        crawler = new RockCrawler(HEIGHT / 2, new Random().nextBoolean() ? 1 : -1);
    }

    // Walks along the rock, or falls until it reaches some
    @Override
    public void update() {
        updateFrame();
        if (crawler.crawl(this, WALK_SPEED)) {
            fallSpeed = 0;
        } else {
            fallSpeed = Math.min(fallSpeed + GRAVITY, MAX_FALL);
            crawler.fall(this, fallSpeed);
        }
    }

    // Too heavy to be knocked back
    @Override
    public void knockBack(MathVector direction) {
    }

    // A stone dome on legs, lit from the top left, its head and horn in front, standing on the rock it walks on
    @Override
    public void draw(Canvas canvas) {
        beginHurtFlash(canvas);
        canvas.save();
        canvas.translate((float) getxPosInScreen(), (float) getyPosInScreen());
        canvas.rotate(crawler.getAngle());
        canvas.translate(0, HEIGHT / 2);
        int facing = crawler.getDirection();
        canvas.scale(facing * SCALE, SCALE);
        canvas.translate(-54, -80);
        int step = (int) (crawler.getWalked() / (WIDTH * 0.12)) % 2;
        stroke(OUTLINE, 4);
        float[][] legs = {{28, 64, 22, 78}, {44, 66, 42, 80}, {60, 66, 62, 80}, {74, 64, 80, 78}};
        for (int i = 0; i < legs.length; i++) {
            float offset = ((i + step) % 2 == 0) ? 3 : -3;
            canvas.drawLine(legs[i][0], legs[i][1], legs[i][2] + offset, legs[i][3], paint);
        }
        path.reset();
        path.moveTo(12, 66);
        path.quadTo(12, 26, 50, 22);
        path.quadTo(84, 24, 88, 60);
        path.lineTo(88, 66);
        path.close();
        fillAndOutline(canvas, SHELL, 3.5f);
        path.reset();
        path.moveTo(50, 24);
        path.quadTo(46, 44, 48, 66);
        path.moveTo(30, 32);
        path.quadTo(26, 48, 28, 66);
        path.moveTo(70, 30);
        path.quadTo(74, 46, 72, 66);
        stroke(SEAM, 2.5f);
        canvas.drawPath(path, paint);
        // Lit from the top left whichever way it faces
        canvas.save();
        if (facing < 0) {
            canvas.scale(-1, 1, 50, 0);
        }
        path.reset();
        path.moveTo(20, 50);
        path.quadTo(28, 30, 50, 28);
        stroke(SHELL_LIGHT, 4);
        canvas.drawPath(path, paint);
        canvas.restore();
        stroke(OUTLINE, 3);
        canvas.drawLine(10, 66, 90, 66, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(SEAM);
        canvas.drawCircle(86, 60, 10, paint);
        stroke(OUTLINE, 3);
        canvas.drawCircle(86, 60, 10, paint);
        path.reset();
        path.moveTo(92, 52);
        path.lineTo(99, 43);
        path.lineTo(96, 56);
        path.close();
        fillAndOutline(canvas, HORN, 2);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(EYE);
        canvas.drawCircle(89, 58, 2.8f, paint);
        canvas.restore();
        endHurtFlash(canvas);
    }

    private void fillAndOutline(Canvas canvas, int color, float width) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(path, paint);
        stroke(OUTLINE, width);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(path, paint);
    }

    private void stroke(int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setColor(color);
    }

    @Override
    public double getHurtDistance() {
        return WIDTH * 0.45 + MainCharacter.BODY_RADIUS * 0.5;
    }

    @Override
    public int getDamageDone() {
        return DAMAGE;
    }

    @Override
    public double getBodyRadius() {
        return WIDTH * 0.5;
    }

    @Override
    protected int getParticleColor() {
        return SHELL;
    }

    @Override
    protected int getCoinsDropped() {
        return 5;
    }

    @Override
    protected double getHealthDropChance() {
        return 0.3;
    }

    @Override
    protected double getPowerUpDropChance() {
        return 0.12;
    }

    @Override
    public int getWidth() {
        return (int) WIDTH;
    }

    @Override
    public int getHeight() {
        return (int) HEIGHT;
    }
}
