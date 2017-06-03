package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Clickable;

/**
 * Created by Sergio on 03/06/2017.
 */
public class HUDPauseButton extends HUDElement implements Clickable {

    private static final int MARGIN = MyActivity.TILE_WIDTH;

    private RectF oval;
    private int alpha = 99;
    private boolean clickable = true, on = false;
    private int touchId = -1, touchIndex = -1;

    public HUDPauseButton(int xCenter, int yCenter, int width, int height) {
        super(xCenter, yCenter, width, height);
        this.oval = new RectF(getxCenter() - super.getWidth() / 2, getyCenter() - super.getHeight() / 2, getxCenter() + super.getWidth() / 2, getyCenter() + super.getHeight() / 2);
    }

    @Override
    public void draw(Canvas canvas) {
        setColor(getMainColor());
        setAlpha(alpha);
        setStyle(Paint.Style.FILL);
        canvas.drawOval(this.oval, getPaint());
        drawThreePoints(canvas);
    }

    private void drawThreePoints(Canvas canvas) {
        for (int i = -1; i <= 1; i++) {
            drawCircle(canvas, getxCenter() + i * super.getWidth() / 4, getyCenter(), Color.rgb(30, 30, 30), alpha, super.getWidth() / 15);
        }
    }

    private int getMainColor() {
        if (isOn()) {
            return Color.rgb(180,180,180);
        } else {
            return Color.WHITE;
        }
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setTouchIndex(index);
        setTouchId(id);
        setOn(true);
        if (MyActivity.paused) {
            MyActivity.unpause();
        } else {
            MyActivity.pause();
        }
    }

    @Override
    public void reset() {
        setTouchIndex(-1);
        setTouchId(-1);
        setOn(false);
    }

    @Override
    public boolean isOn() {
        return on;
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public int getTouchId() {
        return touchId;
    }

    public void setTouchId(int touchId) {
        this.touchId = touchId;
    }

    public int getTouchIndex() {
        return touchIndex;
    }

    public void setTouchIndex(int touchIndex) {
        this.touchIndex = touchIndex;
    }

    @Override
    public int getWidth() {
        return super.getWidth() + MARGIN;
    }

    @Override
    public int getHeight() {
        return super.getHeight() + MARGIN / 2;
    }
}
