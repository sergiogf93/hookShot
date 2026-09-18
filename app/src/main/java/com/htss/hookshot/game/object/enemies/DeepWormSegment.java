package com.htss.hookshot.game.object.enemies;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;

/**
 * A part of the deep worm, moved and drawn by it. Only the tail can be hurt, and only while it glows.
 */
public class DeepWormSegment extends ClickableEnemy {

    private static final int SHELL_LIGHT = Color.rgb(176, 138, 78), SHELL_DARK = Color.rgb(38, 24, 12),
            GLOW_LIGHT = Color.rgb(255, 232, 150), GLOW_DARK = Color.rgb(200, 70, 10), GLOW = Color.rgb(255, 180, 58),
            PLATE = Color.argb(140, 20, 12, 6), EDGE = Color.rgb(18, 11, 6), LEG = Color.rgb(120, 94, 52),
            FANG = Color.rgb(216, 192, 138), EYE_GLOW = Color.argb(210, 255, 180, 58), EYE_CORE = Color.rgb(255, 245, 190);

    private final EnemyDeepWorm worm;
    private final float radius;
    // Towards the head
    private MathVector forward = new MathVector(1, 0);
    private boolean visible = false;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Matrix matrix = new Matrix();
    private final RadialGradient shell, glowing;

    public DeepWormSegment(EnemyDeepWorm worm, float radius) {
        super(worm.getxPosInRoom(), worm.getyPosInRoom(), 0, 0, 0, 1, false, true);
        this.worm = worm;
        this.radius = radius;
        setGhost(true);
        shell = new RadialGradient(0, 0, radius * 1.5f, SHELL_LIGHT, SHELL_DARK, Shader.TileMode.CLAMP);
        glowing = new RadialGradient(0, 0, radius * 1.5f, GLOW_LIGHT, GLOW_DARK, Shader.TileMode.CLAMP);
    }

    public void place(MathVector position, MathVector forward, boolean visible) {
        setPositionInRoom(position);
        this.forward = forward;
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    // Moved by the worm
    @Override
    public void update() {
    }

    @Override
    public void hit(MathVector from) {
        if (!worm.hitSegment(this)) {
            deflect();
        }
    }

    // Hurt by bombs
    @Override
    public void getHurt(int damage) {
        worm.bombSegment(this);
    }

    @Override
    public void knockBack(MathVector direction) {
    }

    @Override
    public boolean isClickable() {
        return visible;
    }

    // A shell lit from the top left with a plate line and a dark edge, like the worm's, and legs on its sides. The head
    // has fangs and glowing eyes. The exposed tail glows, pulsing, and flashes white when hit
    public void draw(Canvas canvas, boolean head, boolean exposed, float flash) {
        float x = (float) getxPosInScreen(), y = (float) getyPosInScreen();
        MathVector side = forward.getNormal();
        if (exposed) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(worm.getFrame() * 0.25);
            DrawUtil.drawGlow(canvas, glowPaint, x, y, radius * (2 + 0.4f * pulse), DrawUtil.withAlpha(GLOW, (int) (120 + 80 * pulse)));
        }
        if (head) {
            drawFangs(canvas, x, y);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(radius * 0.22f);
            paint.setColor(LEG);
            for (int s = -1; s <= 1; s += 2) {
                float wiggle = (float) Math.sin(worm.getFrame() * 0.4 + s) * radius * 0.2f;
                canvas.drawLine(x + (float) side.x * s * radius * 0.7f, y + (float) side.y * s * radius * 0.7f,
                        x + (float) (side.x * s * radius * 1.35 - forward.x * wiggle), y + (float) (side.y * s * radius * 1.35 - forward.y * wiggle), paint);
            }
        }
        RadialGradient gradient = exposed ? glowing : shell;
        matrix.setTranslate(x - radius * 0.35f, y - radius * 0.35f);
        gradient.setLocalMatrix(matrix);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(gradient);
        canvas.drawCircle(x, y, radius, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(exposed ? GLOW_DARK : PLATE);
        paint.setStrokeWidth(radius / 12);
        canvas.drawCircle(x, y, radius * 0.62f, paint);
        paint.setColor(EDGE);
        paint.setStrokeWidth(radius / 8);
        canvas.drawCircle(x, y, radius, paint);
        if (flash > 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(DrawUtil.withAlpha(Color.WHITE, (int) (200 * flash)));
            canvas.drawCircle(x, y, radius, paint);
        }
        if (head) {
            for (int s = -1; s <= 1; s += 2) {
                float eyeX = (float) (x + forward.x * radius * 0.45 + s * side.x * radius * 0.4);
                float eyeY = (float) (y + forward.y * radius * 0.45 + s * side.y * radius * 0.4);
                DrawUtil.drawGlow(canvas, glowPaint, eyeX, eyeY, radius * 0.4f, EYE_GLOW);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(EYE_CORE);
                canvas.drawCircle(eyeX, eyeY, radius * 0.1f, paint);
            }
        }
    }

    // Two curved fangs reaching forward, opening and closing
    private void drawFangs(Canvas canvas, float x, float y) {
        double open = 32 + 8 * Math.sin(worm.getFrame() * 0.3);
        for (int s = -1; s <= 1; s += 2) {
            MathVector base = forward.rotatedDeg(s * open);
            MathVector tip = forward.rotatedDeg(s * (open - 12));
            path.reset();
            path.moveTo(x + (float) (base.x * radius * 0.8), y + (float) (base.y * radius * 0.8));
            path.lineTo(x + (float) (tip.x * radius * 1.9), y + (float) (tip.y * radius * 1.9));
            MathVector inner = forward.rotatedDeg(s * (open - 22));
            path.lineTo(x + (float) (inner.x * radius * 0.9), y + (float) (inner.y * radius * 0.9));
            path.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(FANG);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(radius / 10);
            paint.setColor(EDGE);
            canvas.drawPath(path, paint);
        }
    }

    @Override
    public void draw(Canvas canvas) {
    }

    @Override
    public double getHurtDistance() {
        return worm.hurtsWith(this) ? radius * 0.9 + MainCharacter.BODY_RADIUS * 0.5 : 0;
    }

    @Override
    public int getDamageDone() {
        return EnemyDeepWorm.DAMAGE;
    }

    @Override
    public double getBodyRadius() {
        return radius;
    }

    @Override
    protected int getParticleColor() {
        return SHELL_LIGHT;
    }

    @Override
    protected int getCoinsDropped() {
        return 2;
    }

    @Override
    protected double getHealthDropChance() {
        return 0.2;
    }

    @Override
    protected double getPowerUpDropChance() {
        return 0;
    }

    public float getRadius() {
        return radius;
    }

    @Override
    public int getWidth() {
        return (int) (radius * 2);
    }

    @Override
    public int getHeight() {
        return (int) (radius * 2);
    }
}
