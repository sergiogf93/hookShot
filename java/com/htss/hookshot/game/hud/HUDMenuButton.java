package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.util.StringUtil;

/**
 * Created by Sergio on 03/06/2017.
 */
public class HUDMenuButton extends HUDElement implements Clickable {

    private static final int TEXT_SIZE = (int) (MyActivity.TILE_WIDTH *0.35);

    private String text;
    private RectF background;
    private boolean clickable = true, on = false;
    private int touchId = -1, touchIndex = -1;
    private int alpha = 150;
    private Execution execOff;

    public HUDMenuButton(int xCenter, int yCenter, int width, int height, String text, Execution execOff) {
        super(xCenter, yCenter, width, height);
        this.text = text;
        this.background = new RectF(getxCenter() - getWidth() / 2, getyCenter() - getHeight() / 2, getxCenter() + getWidth() / 2, getyCenter() + getHeight() / 2);
        this.execOff = execOff;
    }

    @Override
    public void draw(Canvas canvas) {
        setColor(Color.rgb(15,35,45));
        setAlpha(alpha);
        setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(this.background, getWidth() / 20, getHeight() / 5, getPaint());
        setColor(getMainColor());
        setAlpha(alpha);
        setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(this.background, getWidth() / 20, getHeight() / 5, getPaint());
        setColor(Color.rgb(11,22,30));
        getPaint().setTextSize(TEXT_SIZE);
        canvas.drawText(getText(), getxCenter() - getTextWidth() / 2, getyCenter() + TEXT_SIZE / 4, getPaint());
    }

    private int getMainColor() {
        if (isOn()) {
            return Color.rgb(45, 85, 110);
        } else {
            return Color.rgb(90, 170, 220);
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getTextWidth() {
        return StringUtil.sizeOfString(getText(), TEXT_SIZE);
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setTouchIndex(index);
        setTouchId(id);
        setOn(true);
    }

    @Override
    public void reset() {
        setTouchIndex(-1);
        setTouchId(-1);
        setOn(false);
        if (this.execOff != null) {
            this.execOff.execute();
        }
    }

    public boolean isOn() {
        return on;
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
}
