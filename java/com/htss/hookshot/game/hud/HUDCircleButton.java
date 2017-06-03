package com.htss.hookshot.game.hud;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.interfaces.Execution;
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
        super(xCenter,yCenter);
        this.clickable = clickable;
        this.radius = radius;
        this.text = text;
        this.execOn = execOn;
        this.execOff = null;
        this.execDoubleOn = null;
    }

    public HUDCircleButton(int xCenter, int yCenter, float radius, String text, boolean clickable, Execution execOn, Execution execOff) {
        super(xCenter,yCenter);
        this.clickable = clickable;
        this.radius = radius;
        this.text = text;
        this.execOn = execOn;
        this.execOff = execOff;
        this.execDoubleOn = null;
    }

    public HUDCircleButton(int xCenter, int yCenter, float radius, String text, boolean clickable, Execution execOn, Execution execOff, Execution execDoubleOn) {
        super(xCenter,yCenter);
        this.clickable = clickable;
        this.radius = radius;
        this.text = text;
        this.execOn = execOn;
        this.execOff = execOff;
        this.execDoubleOn = execDoubleOn;
    }

    @Override
    public void draw(Canvas canvas) {
        drawCircle(canvas, getxCenter(), getyCenter(), Color.rgb(30, 30, 30), alpha, getRadius());
        drawCircle(canvas, getxCenter(), getyCenter(), Color.WHITE, alpha, (float) (0.95 * getRadius()));
        drawCircle(canvas, getxCenter(), getyCenter(), Color.rgb(30, 30, 30), alpha, (float) (0.9 * getRadius()));
        drawCircle(canvas, getxCenter(), getyCenter(), getMainColor(), alpha, (float) (0.85 * getRadius()));
        getPaint().setColor(Color.rgb(30, 30, 30));
        int textSize = (int) (2 * getRadius() / 3);
        getPaint().setTextSize(textSize);
        canvas.drawText(getText(), (float) (getxCenter() - StringUtil.sizeOfString(getText(), textSize) / 1.5), getyCenter() + textSize / 4, getPaint());
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
        if (getExecDoubleOn() != null) {
            if(System.currentTimeMillis() - getTimeWhenOn() < 500) {
                setTimeWhenOn(0);
                getExecDoubleOn().execute();
            } else {
                if (getExecOn() != null){
                    setTimeWhenOn(System.currentTimeMillis());
                    getExecOn().execute();
                }
            }
        } else {
            setTimeWhenOn(System.currentTimeMillis());
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
