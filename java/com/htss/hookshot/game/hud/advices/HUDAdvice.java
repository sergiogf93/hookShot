package com.htss.hookshot.game.hud.advices;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.HUDElement;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.util.StringUtil;

import java.util.LinkedList;

/**
 * Created by Sergio on 12/06/2017.
 */
public abstract class HUDAdvice extends HUDElement implements Clickable{

    public static final int ALPHA = 150;

    private String text;
    private LinkedList<String> lines = new LinkedList<String>();
    private RectF background;
    private boolean clickable = true, on = false;
    private int touchId = -1, touchIndex = -1, size;
    private int state = 0;

    public HUDAdvice(int xCenter, int yCenter, int width, String text, int size) {
        super(xCenter, yCenter, width, 0);
        this.text = text;
        this.size = size;
        getPaint().setTypeface(MyActivity.canvas.joystickMonospace);
        getPaint().setTextSize(size);
        generateLinesAndPrepareBackground(text);
    }

    protected void start() {
        MyActivity.hideControls();
        MyActivity.pauseButton.setClickable(false);
        MyActivity.hudElements.add(this);
        MyActivity.paused = true;
    }

    public void generateLinesAndPrepareBackground(String text) {
        lines.clear();
        int i = 1;
        while (i < text.length()) {
            String substring = text.substring(0, i);
            if (getPaint().measureText(substring) >= getWidth() * 0.8) {
                substring = StringUtil.shortenUntilSpace(substring);
                lines.add(substring);
                text = text.substring(text.indexOf(substring) + substring.length(), text.length());
                i = 0;
            }
            i++;
        }
        if (text.length() > 0) {
            lines.add(text);
        }
        this.background = new RectF(getxCenter() - getWidth() / 2, getyCenter() - getHeight() / 2, getxCenter() + getWidth() / 2, getyCenter() + getHeight() / 2);
    }

    @Override
    public int getHeight() {
        return lines.size() * size  + size;
    }

    @Override
    public void draw(Canvas canvas) {
        setColor(Color.CYAN);
        setAlpha(ALPHA);
        canvas.drawRoundRect(this.background, MyActivity.TILE_WIDTH / 2, MyActivity.TILE_WIDTH / 2, getPaint());
        setColor(Color.BLACK);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            canvas.drawText(line, (float) (getxCenter() - getWidth() / 2 + getWidth() * 0.1), getyCenter() - getHeight() / 2 + (i + 1) * size + getHeight() / 10, getPaint());
        }
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setTouchIndex(index);
        setTouchId(id);
        setOn(true);
    }

    public void finish() {
        MyActivity.advices.remove(this);
    }

    public void resume() {
        MyActivity.hudElements.remove(this);
        MyActivity.addControls();
        MyActivity.pauseButton.setClickable(true);
        MyActivity.paused = false;
        this.state += 1;
    }

    @Override
    public void reset() {
        setTouchIndex(-1);
        setTouchId(-1);
        setOn(false);
        resume();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        generateLinesAndPrepareBackground(text);
    }

    public int getState() {
        return state;
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
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

    public abstract void check();

}
