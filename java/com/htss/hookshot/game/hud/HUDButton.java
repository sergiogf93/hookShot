package com.htss.hookshot.game.hud;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.interfaces.Execution;

/**
 * Created by Sergio on 03/08/2016.
 */
public class HUDButton extends HUDElementSprite implements Clickable {

    protected Bitmap spriteOff;
    protected Bitmap spriteOn;
    private boolean clickable, on = false;
    private int touchId = -1, touchIndex = -1;
    private int margin = 0;

    protected Execution execOn, execOff;

    public HUDButton(int xCenter, int yCenter, Bitmap spriteOff, Bitmap spriteOn, boolean clickable, Execution execOn) {
        super(xCenter,yCenter,spriteOff);
        this.spriteOff = spriteOff;
        this.spriteOn = spriteOn;
        this.clickable = clickable;
        this.execOn = execOn;
        this.execOff = null;
        if (spriteOff.getWidth() < 100) {
            this.margin = 50;
        }
    }

    public HUDButton(int xCenter, int yCenter, Bitmap spriteOff, Bitmap spriteOn, boolean clickable, Execution execOn, Execution execOff) {
        super(xCenter,yCenter,spriteOff);
        this.spriteOff = spriteOff;
        this.spriteOn = spriteOn;
        this.clickable = clickable;
        this.execOn = execOn;
        this.execOff = execOff;
        if (spriteOff.getWidth() < 100) {
            this.margin = 80;
        }
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setTouchIndex(index);
        setTouchId(id);
        setSprite(getSpriteOn());
        setOn(true);
        if (getExecOn() != null){
            getExecOn().execute();
        }
    }

    @Override
    public void reset() {
        setTouchIndex(-1);
        setTouchId(-1);
        setSprite(getSpriteOff());
        setOn(false);
        if (getExecOff() != null){
            getExecOff().execute();
        }
    }

    @Override
    public int getHeight() {
        return super.getHeight() + margin;
    }

    @Override
    public int getWidth() {
        return super.getWidth() + margin;
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

    public Bitmap getSpriteOff() {
        return spriteOff;
    }

    public void setSpriteOff(Bitmap spriteOff) {
        this.spriteOff = spriteOff;
    }

    public Bitmap getSpriteOn() {
        return spriteOn;
    }

    public void setSpriteOn(Bitmap spriteOn) {
        this.spriteOn = spriteOn;
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
}
