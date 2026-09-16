package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 16/06/2017.
 */
public class TerraWormBody extends ClickableEnemy {

    private static final int COLLISION_PRIORITY = 0, MASS = 0, MAX_HEALTH = 5;

    // A shell lit from the top left, red while hurt, with a plate line and a dark edge. Legs, fangs and glowing eyes
    private static final int SHELL_LIGHT = Color.rgb(160, 126, 70), SHELL_DARK = Color.rgb(34, 22, 12),
            HURT_LIGHT = Color.rgb(255, 110, 90), HURT_DARK = Color.rgb(110, 10, 10),
            PLATE = Color.argb(140, 20, 12, 6), EDGE = Color.rgb(18, 11, 6), LEG = Color.rgb(110, 88, 48),
            EYE_GLOW = Color.argb(200, 255, 190, 60), EYE_CORE = Color.rgb(255, 245, 190);

    private float radius;
    private EnemyTerraWorm terraWorm;
    private RadialGradient shellGradient, hurtGradient;
    private Matrix shellMatrix = new Matrix();
    private Paint shellPaint = new Paint(Paint.ANTI_ALIAS_FLAG), linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TerraWormBody(double xPos, double yPos, float radius, EnemyTerraWorm terraWorm, boolean addToLists, boolean addToEnemyList) {
        super(xPos, yPos, MASS, COLLISION_PRIORITY, 0, MAX_HEALTH, addToLists, addToEnemyList);
        this.terraWorm = terraWorm;
        this.radius = radius;
        getPaint().setStrokeWidth(radius / 7);
        shellGradient = new RadialGradient(0, 0, radius * 1.5f, SHELL_LIGHT, SHELL_DARK, Shader.TileMode.CLAMP);
        hurtGradient = new RadialGradient(0, 0, radius * 1.5f, HURT_LIGHT, HURT_DARK, Shader.TileMode.CLAMP);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas) {
        float x = (float) getxPosInScreen(), y = (float) getyPosInScreen();
        boolean head = this == terraWorm.getBodyParts().lastElement();
        // Legs and fangs first, so the shell covers where they join
        if (head) {
            drawFangs(canvas);
        } else {
            drawLegs(canvas);
        }
        drawShell(canvas, x, y, isOn() || isFrozen());
        if (head) {
            drawEyes(canvas, x, y);
        }
    }

    private void drawShell(Canvas canvas, float x, float y, boolean hurt) {
        RadialGradient gradient = hurt ? hurtGradient : shellGradient;
        shellMatrix.setTranslate(x - radius * 0.35f, y - radius * 0.35f);
        gradient.setLocalMatrix(shellMatrix);
        shellPaint.setShader(gradient);
        canvas.drawCircle(x, y, radius, shellPaint);
        linePaint.setColor(PLATE);
        linePaint.setStrokeWidth(radius / 12);
        canvas.drawCircle(x, y, radius * 0.62f, linePaint);
        linePaint.setColor(EDGE);
        linePaint.setStrokeWidth(radius / 8);
        canvas.drawCircle(x, y, radius, linePaint);
    }

    private void drawEyes(Canvas canvas, float x, float y) {
        if (getP().isNull()) {
            return;
        }
        MathVector forward = getP().rescaled(radius * 0.45);
        MathVector side = getP().getNormal().rescaled(radius * 0.4);
        for (int s = -1; s <= 1; s += 2) {
            float eyeX = (float) (x + forward.x + s * side.x), eyeY = (float) (y + forward.y + s * side.y);
            DrawUtil.drawGlow(canvas, shellPaint, eyeX, eyeY, radius * 0.35f, EYE_GLOW);
            shellPaint.setColor(EYE_CORE);
            canvas.drawCircle(eyeX, eyeY, radius * 0.1f, shellPaint);
        }
    }

    private void drawLegs(Canvas canvas) {
        Point[] points = new Point[3];
        points[0] = getP().getNormal().rotatedDeg(10).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        points[1] = getP().getNormal().rescaled(getRadius() * 1.3).applyTo(getPositionInScreen()).toPoint();
        points[2] = getP().getNormal().rotatedDeg(-10).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, LEG, Paint.Style.FILL, false, getPaint());
        points[0] = getP().getNormal().rotatedDeg(10).rescaled(-getRadius()).applyTo(getPositionInScreen()).toPoint();
        points[1] = getP().getNormal().rescaled(-getRadius() * 1.3).applyTo(getPositionInScreen()).toPoint();
        points[2] = getP().getNormal().rotatedDeg(-10).rescaled(-getRadius()).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, LEG, Paint.Style.FILL, false, getPaint());
    }

    private void drawFangs(Canvas canvas) {
        Point[] points = new Point[5];
        int angle = (terraWorm.getFrame() % TimeUtil.secondsToUpdates(0.833) < TimeUtil.secondsToUpdates(0.417)) ? 35 : 40;
        points[0] = getP().rotatedDeg(angle).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        points[1] = getP().rotatedDeg(angle - 5).rescaled(getRadius() * 1.5).applyTo(getPositionInScreen()).toPoint();
        points[2] = getP().rotatedDeg(angle - 10).rescaled(getRadius() * 1.9).applyTo(getPositionInScreen()).toPoint();
        points[3] = getP().rotatedDeg(angle - 15).rescaled(getRadius() * 1.2).applyTo(getPositionInScreen()).toPoint();
        points[4] = getP().rotatedDeg(angle - 20).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, LEG, Paint.Style.FILL, false, getPaint());
        points[0] = getP().rotatedDeg(-angle).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        points[1] = getP().rotatedDeg(-angle + 5).rescaled(getRadius() * 1.5).applyTo(getPositionInScreen()).toPoint();
        points[2] = getP().rotatedDeg(-angle + 10).rescaled(getRadius() * 1.9).applyTo(getPositionInScreen()).toPoint();
        points[3] = getP().rotatedDeg(-angle + 15).rescaled(getRadius() * 1.2).applyTo(getPositionInScreen()).toPoint();
        points[4] = getP().rotatedDeg(-angle + 20).rescaled(getRadius()).applyTo(getPositionInScreen()).toPoint();
        DrawUtil.drawPolygon(points, canvas, LEG, Paint.Style.FILL, false, getPaint());
    }

    @Override
    protected GameEnemy getHitTarget() {
        // The worm loses its tail first, wherever it's hit
        return terraWorm.getBodyParts().firstElement();
    }

    @Override
    public double getBodyRadius() {
        return radius;
    }

    @Override
    protected int getParticleColor() {
        return Color.rgb(150, 125, 50);
    }

    @Override
    public void freeze() {
        // The whole worm stops
        terraWorm.freeze();
    }

    @Override
    public boolean isFrozen() {
        return terraWorm.isFrozen();
    }

    @Override
    public boolean canBeHit() {
        return terraWorm.canBeHit();
    }

    @Override
    public void startHitCooldown() {
        terraWorm.startHitCooldown();
    }

    @Override
    public void die() {
        terraWorm.removeBodyPart(this);
        super.die();
    }

    @Override
    public double getHurtDistance() {
        return radius * 1.5;
    }

    @Override
    public int getDamageDone() {
        return 10;
    }

    @Override
    public int getWidth() {
        return (int) (radius * 2);
    }

    @Override
    public int getHeight() {
        return (int) (radius * 2);
    }

    public float getRadius() {
        return radius;
    }
}
