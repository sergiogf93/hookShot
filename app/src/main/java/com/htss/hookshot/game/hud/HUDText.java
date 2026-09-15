package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.util.StringUtil;

/**
 * Created by Sergio on 30/05/2017.
 */
public class HUDText extends HUDElement implements Clickable {

    private int size;
    private String text;
    private boolean clickable, on = false;
    protected Execution execOff;
    private int touchId = -1, touchIndex = -1;

    private static final int DEPTH = MyActivity.TILE_WIDTH /20;

    public HUDText(int xPos, int yPos, boolean clickable, String text, int size, Execution execOff) {
        super(xPos, yPos, 0, size);
        this.text = text;
        this.execOff = execOff;
        this.size = size;
        this.clickable = clickable;
        getPaint().setTypeface(GameBoard.paint.getTypeface());
        getPaint().setTextSize(getSize());
        setWidth((int) getPaint().measureText(text));
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
        if (getExecOff() != null){
            getExecOff().execute();
        }
    }

    @Override
    public int getHeight() {
        return getSize();
    }

    @Override
    public void draw(Canvas canvas) {
        setColor(Color.BLACK);
        canvas.drawText(getText(),getxCenter()-getWidth()/2,getyCenter()+getHeight()/4,getPaint());
        if(!isOn()) {
            setColor(Color.WHITE);
            canvas.drawText(getText(), getxCenter() - getWidth() / 2 + DEPTH, getyCenter() + getHeight() / 4 + DEPTH, getPaint());
        } else {
            setColor(Color.GRAY);
            canvas.drawText(getText(), getxCenter() - getWidth() / 2 + DEPTH / 2, getyCenter() + getHeight() / 4 + DEPTH / 2, getPaint());
        }
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

    public Execution getExecOff() {
        return execOff;
    }

    public void setExecOff(Execution execOff) {
        this.execOff = execOff;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
