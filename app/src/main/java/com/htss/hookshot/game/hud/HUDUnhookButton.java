package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.util.DrawUtil;

/**
 * The button that lets go of the chain, marked with two chain links coming apart. With no chain out there's nothing
 * to let go of, and it's drawn faint with a dashed edge; with one, it lights up.
 */
public class HUDUnhookButton extends HUDCircleButton {

    private final RectF link = new RectF();

    public HUDUnhookButton(float radius, Execution execOn) {
        super(0, 0, radius, "", true, execOn);
    }

    @Override
    public void draw(Canvas canvas) {
        boolean chain = MyActivity.character != null && MyActivity.character.getHook() != null;
        float x = getxCenter(), y = getyCenter(), r = getRadius();
        if (chain) {
            UiStyle.drawControl(canvas, getPaint(), x, y, r, isOn(), UiStyle.GOLD);
        } else {
            UiStyle.drawIdleControl(canvas, getPaint(), x, y, r);
        }
        getPaint().setShader(null);
        getPaint().setStyle(Paint.Style.STROKE);
        getPaint().setStrokeWidth(r * 0.11f);
        getPaint().setColor(chain ? (isOn() ? UiStyle.GOLD : UiStyle.TEXT) : DrawUtil.withAlpha(UiStyle.IDLE, 150));
        // Two links on a slant, a gap apart
        canvas.save();
        canvas.rotate(-45, x, y);
        link.set(x - r * 0.62f, y - r * 0.17f, x - r * 0.1f, y + r * 0.17f);
        canvas.drawRoundRect(link, r * 0.17f, r * 0.17f, getPaint());
        link.set(x + r * 0.1f, y - r * 0.17f, x + r * 0.62f, y + r * 0.17f);
        canvas.drawRoundRect(link, r * 0.17f, r * 0.17f, getPaint());
        canvas.restore();
        getPaint().setStyle(Paint.Style.FILL);
    }
}
