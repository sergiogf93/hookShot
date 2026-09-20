package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;

/**
 * The stick on the right of the twin stick controls, which only aims the chain. Dragged out of its middle it shows an
 * arrow on the character, and the hook is thrown that way when it's let go. Dragged back to the middle first, nothing
 * is thrown. Reeling and letting go have their own buttons around it. With the second twin stick controls, which have
 * no button to reel with, tapping it twice zips up the chain.
 *
 * It rests in its corner, but centres itself under the thumb wherever that lands on the right side of the screen, so
 * a flick aims from wherever it starts and the stick never has to be found by feel. The buttons and enemies under a
 * thumb come first, so the activity hands it the touches that are left.
 */
public class HUDHookStick extends Joystick {

    // The share of the handle's reach past which it aims. Small, as aiming is all it does, so a quick flick counts
    private static final double AIM = 0.25;
    // The share of the screen, from the left, where its side starts
    private static final double SIDE = 0.5;

    private int homeX, homeY;

    // Whether this press has aimed at all, and when the last one that didn't was let go, for the double tap
    private boolean aimed = false;
    private long lastTap = 0;
    private final Path mark = new Path();

    public HUDHookStick(int xCenter, int yCenter, int width, int height) {
        super(xCenter, yCenter, width, height);
    }

    // A gold edge tells it apart from the stick that moves the character
    @Override
    protected void drawBase(Canvas canvas) {
        UiStyle.drawControl(canvas, getPaint(), getxCenter(), getyCenter(), getRadius(), false, DrawUtil.withAlpha(UiStyle.GOLD, 170));
    }

    @Override
    protected void drawMarks(Canvas canvas) {
    }

    // The handle lights up gold once it's dragged far enough to aim: lit, letting go throws the hook; unlit, nothing
    // is thrown. On it, a crosshair, or while there's a chain to zip up by tapping twice, a pair of arrows
    @Override
    protected void drawHandle(Canvas canvas) {
        float x = getxCenter() + getxJ(), y = getyCenter() + getyJ(), r = getHandleRadius();
        boolean armed = isAiming();
        UiStyle.drawControl(canvas, getPaint(), x, y, r, true, armed ? UiStyle.GOLD : DrawUtil.withAlpha(UiStyle.GOLD, 150));
        getPaint().setShader(null);
        getPaint().setStyle(Paint.Style.STROKE);
        getPaint().setStrokeCap(Paint.Cap.ROUND);
        getPaint().setStrokeJoin(Paint.Join.ROUND);
        getPaint().setStrokeWidth(r * 0.09f);
        getPaint().setColor(armed ? UiStyle.GOLD : DrawUtil.withAlpha(UiStyle.TEXT, 210));
        boolean zips = MyActivity.controls == MyActivity.CONTROLS_TWIN_2 && !isOn() && MyActivity.character != null && MyActivity.character.isHooked();
        if (zips) {
            mark.reset();
            mark.moveTo(x - r * 0.34f, y - r * 0.02f);
            mark.lineTo(x, y - r * 0.36f);
            mark.lineTo(x + r * 0.34f, y - r * 0.02f);
            mark.moveTo(x - r * 0.34f, y + r * 0.34f);
            mark.lineTo(x, y);
            mark.lineTo(x + r * 0.34f, y + r * 0.34f);
            canvas.drawPath(mark, getPaint());
        } else {
            canvas.drawCircle(x, y, r * 0.22f, getPaint());
            canvas.drawLine(x, y - r * 0.5f, x, y - r * 0.32f, getPaint());
            canvas.drawLine(x, y + r * 0.5f, x, y + r * 0.32f, getPaint());
            canvas.drawLine(x - r * 0.5f, y, x - r * 0.32f, y, getPaint());
            canvas.drawLine(x + r * 0.5f, y, x + r * 0.32f, y, getPaint());
        }
        getPaint().setStyle(Paint.Style.FILL);
    }

    // Where it rests while no thumb is on it
    public void setHome(int x, int y) {
        homeX = x;
        homeY = y;
        if (!isOn()) {
            setCenter(x, y);
        }
    }

    public int getHomeX() {
        return homeX;
    }

    public int getHomeY() {
        return homeY;
    }

    // Never pressed like the other controls, which would take the touch before an enemy under it got it
    @Override
    public boolean pressed(double x, double y) {
        return false;
    }

    // On its side of the screen, where a touch nothing else took is its own
    public boolean isOnItsSide(double x, double y) {
        return x >= MyActivity.screenWidth * SIDE;
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setCenter((int) x, (int) y);
        setOn(true);
        setTouchId(id);
        setTouchIndex(index);
        aimed = false;
        moveHandle(x, y);
        if (MyActivity.controls == MyActivity.CONTROLS_TWIN_2 && !aimed && MyActivity.character != null && MyActivity.character.isHooked()
                && SystemClock.uptimeMillis() - lastTap < MyActivity.DOUBLE_TAP_MILLIS) {
            lastTap = 0;
            if (MyActivity.character.getHook().getNodesNumber() > Hook.MIN_RELOADING_NODES) {
                MyActivity.character.getHook().setFastReloading(true);
            }
        }
    }

    public void moveHandle(double x, double y) {
        moveJoystick(x, y);
        aimed = aimed || isAiming();
    }

    // Let go: throws the hook the way it's aimed, unless the handle was brought back to the middle
    @Override
    public void reset() {
        // Also called when the controls are put away, with no finger on the stick, which changes nothing
        if (!isOn()) {
            return;
        }
        boolean shoot = isAiming();
        double dx = getxJ(), dy = getyJ();
        if (!aimed) {
            lastTap = SystemClock.uptimeMillis();
        }
        release();
        if (shoot) {
            MyActivity.shootToward(dx, dy);
        }
    }

    // The controls were put away with a finger on the stick, like for a tip or the pause menu: nothing is thrown
    public void cancel() {
        if (isOn()) {
            release();
        }
    }

    private void release() {
        setOn(false);
        setxJ(0);
        setyJ(0);
        setTouchId(-1);
        setTouchIndex(-1);
        setCenter(homeX, homeY);
    }

    public boolean isAiming() {
        return isOn() && new MathVector(getxJ(), getyJ()).magnitude() / (getHeight() / 3.0) > AIM;
    }
}
