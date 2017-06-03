package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.shapes.RoundRectShape;

import com.htss.hookshot.game.MyActivity;

import java.util.Vector;

/**
 * Created by Sergio on 03/06/2017.
 */
public class HUDMenu extends HUDElement {

    private RectF background;
    private int alpha = 150;
    private Vector<HUDMenuButton> buttons = new Vector<HUDMenuButton>();

    public HUDMenu(int xCenter, int yCenter, int width, int height) {
        super(xCenter, yCenter, width, height);
        this.background = new RectF(getxCenter() - getWidth() / 2, getyCenter() - getHeight() / 2, getxCenter() + getWidth() / 2, getyCenter() + getHeight() / 2);
    }

    @Override
    public void draw(Canvas canvas) {
        setColor(Color.CYAN);
        setAlpha(alpha);
        canvas.drawRoundRect(this.background, MyActivity.TILE_WIDTH / 2, MyActivity.TILE_WIDTH / 2, getPaint());
    }

    public void addMenuButtons() {
        buttons.add(new HUDMenuButton(getxCenter(), getyCenter(), (int) (getWidth() * 0.9), (int) (getHeight() * 0.15), "DEBUG"));
        MyActivity.hudElements.addAll(buttons);
    }

    public void removeButtons(){
        MyActivity.hudElements.removeAll(buttons);
    }
}
