package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.math.MathVector;

/**
 * A round button with an arrow on it, pointing up or down, like the pair that reels the chain in and lets it out. It's
 * only pressed on the button itself, so two of them can sit close together without a thumb pressing both.
 */
public class HUDArrowButton extends HUDCircleButton {

    private static final double HIT = 1.15;

    private final boolean up;
    private final Path arrow = new Path();

    public HUDArrowButton(float radius, boolean up, Execution execOn, Execution execOff, Execution execDoubleOn) {
        super(0, 0, radius, "", true, execOn, execOff, execDoubleOn);
        this.up = up;
    }

    @Override
    public void draw(Canvas canvas) {
        UiStyle.drawControl(canvas, getPaint(), getxCenter(), getyCenter(), getRadius(), isOn());
        float r = getRadius(), x = getxCenter(), y = getyCenter(), tip = up ? -1 : 1;
        getPaint().setShader(null);
        getPaint().setStyle(Paint.Style.STROKE);
        getPaint().setStrokeWidth(r * 0.16f);
        getPaint().setStrokeCap(Paint.Cap.ROUND);
        getPaint().setStrokeJoin(Paint.Join.ROUND);
        getPaint().setColor(isOn() ? UiStyle.getAccent() : UiStyle.TEXT);
        arrow.reset();
        arrow.moveTo(x - r * 0.4f, y - tip * r * 0.18f);
        arrow.lineTo(x, y + tip * r * 0.26f);
        arrow.lineTo(x + r * 0.4f, y - tip * r * 0.18f);
        canvas.drawPath(arrow, getPaint());
        getPaint().setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean pressed(double x, double y) {
        return new MathVector(x - getxCenter(), y - getyCenter()).magnitude() <= getRadius() * HIT;
    }
}
