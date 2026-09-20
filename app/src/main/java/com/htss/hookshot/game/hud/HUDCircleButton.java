package com.htss.hookshot.game.hud;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.StringUtil;

/**
 * Created by Sergio on 03/08/2016.
 */
public class HUDCircleButton extends HUDElement implements Clickable {

    private float radius;
    private int alpha = 99;
    private String text;
    private boolean clickable, on = false;
    private int touchId = -1, touchIndex = -1;
    private long timeWhenOn = 0;
    private int margin = MyActivity.TILE_WIDTH/2;

    protected Execution execOn, execOff, execDoubleOn;

    public HUDCircleButton(int xCenter, int yCenter, float radius, String text, boolean clickable, Execution execOn) {
        super(xCenter, yCenter, (int) radius * 2, (int) radius * 2);
        this.clickable = clickable;
        this.radius = radius;
        this.text = text;
        this.execOn = execOn;
        this.execOff = null;
        this.execDoubleOn = null;
        setAlpha(alpha);
    }

    public HUDCircleButton(int xCenter, int yCenter, float radius, String text, boolean clickable, Execution execOn, Execution execOff) {
        super(xCenter, yCenter, (int) radius * 2, (int) radius * 2);
        this.clickable = clickable;
        this.radius = radius;
        this.text = text;
        this.execOn = execOn;
        this.execOff = execOff;
        this.execDoubleOn = null;
        setAlpha(alpha);
    }

    public HUDCircleButton(int xCenter, int yCenter, float radius, String text, boolean clickable, Execution execOn, Execution execOff, Execution execDoubleOn) {
        super(xCenter, yCenter, (int) radius * 2, (int) radius * 2);
        this.clickable = clickable;
        this.radius = radius;
        this.text = text;
        this.execOn = execOn;
        this.execOff = execOff;
        this.execDoubleOn = execDoubleOn;
        setAlpha(alpha);
    }

    @Override
    public void draw(Canvas canvas) {
        UiStyle.drawControl(canvas, getPaint(), getxCenter(), getyCenter(), getRadius(), isOn());
        int textSize = (int) (2 * getRadius() / 3);
        getPaint().setTypeface(MyActivity.canvas.arcadeClassicFont);
        getPaint().setTextSize(textSize);
        // Longer labels shrink to fit inside the button
        float fit = getRadius() * 1.5f, width = getPaint().measureText(getText());
        if (width > fit) {
            textSize = (int) (textSize * fit / width);
            getPaint().setTextSize(textSize);
        }
        float textX = getxCenter() - getPaint().measureText(getText()) / 2;
        UiStyle.drawText(canvas, getPaint(), getText(), textX, getyCenter() + textSize / 3f, isOn() ? UiStyle.getAccent() : UiStyle.TEXT);
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setTouchIndex(index);
        setTouchId(id);
        setOn(true);
        if (getExecDoubleOn() != null) {
            if(SystemClock.uptimeMillis() - getTimeWhenOn() < MyActivity.DOUBLE_TAP_MILLIS) {
                setTimeWhenOn(0);
                getExecDoubleOn().execute();
            } else {
                // Remembered whether or not a single press does anything, for the second press to count as double
                setTimeWhenOn(SystemClock.uptimeMillis());
                if (getExecOn() != null){
                    getExecOn().execute();
                }
            }
        } else {
            setTimeWhenOn(SystemClock.uptimeMillis());
            if (getExecOn() != null) {
                getExecOn().execute();
            }
        }
    }

    @Override
    public void reset() {
        setTouchIndex(-1);
        setTouchId(-1);
        setOn(false);
        if (getExecOff() != null){
            getExecOff().execute();
        }
    }

    @Override
    public int getHeight() {
        return (int) (getRadius()*2 + margin);
    }

    @Override
    public int getWidth() {
        return (int) (getRadius()*2 + margin);
    }

    @Override
    public boolean isOn() {
        return on;
    }

    @Override
    public boolean isClickable() {
        return clickable;
    }

    @Override
    public int getTouchId() {
        return touchId;
    }

    @Override
    public int getTouchIndex() {
        return touchIndex;
    }

    @Override
    public void setClickable(boolean bool) {
        this.clickable = bool;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public void setTouchId(int touchId) {
        this.touchId = touchId;
    }

    public void setTouchIndex(int touchIndex) {
        this.touchIndex = touchIndex;
    }

    public Execution getExecOn() {
        return execOn;
    }

    public void setExecOn(Execution execOn) {
        this.execOn = execOn;
    }

    public Execution getExecOff() {
        return execOff;
    }

    public void setExecOff(Execution execOff) {
        this.execOff = execOff;
    }

    public long getTimeWhenOn() {
        return timeWhenOn;
    }

    public void setTimeWhenOn(long timeWhenOn) {
        this.timeWhenOn = timeWhenOn;
    }

    public Execution getExecDoubleOn() {
        return execDoubleOn;
    }

    public void setExecDoubleOn(Execution execDoubleOn) {
        this.execDoubleOn = execDoubleOn;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
