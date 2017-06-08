package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;

import com.htss.hookshot.executions.LaunchGame;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Execution;

import java.util.Vector;

/**
 * Created by Sergio on 03/06/2017.
 */
public class HUDMenu extends HUDElement {

    public static final int MENU_ALPHA = 150;

    private RectF background;
    private Vector<HUDMenuButton> buttons = new Vector<HUDMenuButton>();
    private int buttonHeight, buttonSeparation;

    public HUDMenu(int xCenter, int yCenter, int width, int height, int buttonHeight, int buttonSeparation) {
        super(xCenter, yCenter, width, height);
        this.buttonHeight = buttonHeight;
        this.buttonSeparation = buttonSeparation;
        this.background = new RectF(getxCenter() - getWidth() / 2, getyCenter() - getHeight() / 2, getxCenter() + getWidth() / 2, getyCenter() + getHeight() / 2);
    }

    @Override
    public void draw(Canvas canvas) {
        setColor(Color.CYAN);
        setAlpha(MENU_ALPHA);
        canvas.drawRoundRect(this.background, MyActivity.TILE_WIDTH / 2, MyActivity.TILE_WIDTH / 2, getPaint());
    }

    public void addMenuButtons() {
        buttons.add(new HUDMenuButton(getxCenter(), getyCenter() - getHeight() / 2 + buttonSeparation + buttonHeight / 2, (int) (getWidth() * 0.9), buttonHeight, "RESET", new LaunchGame()));
        buttons.add(new HUDMenuButton(getxCenter(), getyCenter() - getHeight() / 2 + 2*buttonSeparation + 3* buttonHeight / 2, (int) (getWidth() * 0.9), buttonHeight, "DEBUG", new Execution() {
            @Override
            public double execute() {
                return 0;
            }
        }));
        MyActivity.hudElements.addAll(buttons);
    }

    public void removeButtons(){
        MyActivity.hudElements.removeAll(buttons);
    }
}
