package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;

import com.htss.hookshot.executions.LaunchGame;
import com.htss.hookshot.executions.MainMenu;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Execution;

import java.util.Vector;

/**
 * Created by Sergio on 03/06/2017.
 */
public class HUDMenu extends HUDElement {

    public static final int MENU_ALPHA = 150;

    private RectF background;
    private Vector<HUDButton> buttons = new Vector<HUDButton>();
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
        buttons.clear();
        buttons.add(new HUDButton(getxCenter(), getyCenter() - getHeight() / 2 + buttonSeparation + buttonHeight / 2, (int) (getWidth() * 0.9), buttonHeight, "RESET", new LaunchGame()));
        buttons.add(new HUDButton(getxCenter(), getyCenter() - getHeight() / 2 + 2*buttonSeparation + 3* buttonHeight / 2, (int) (getWidth() * 0.9), buttonHeight, "DEBUGGER " + MyActivity.debugging, new Execution() {
            @Override
            public double execute() {
                MyActivity.debugging = !MyActivity.debugging;
                MyActivity.unpause();
                return 0;
            }
        }));
        buttons.add(new HUDButton(getxCenter(), getyCenter() - getHeight() / 2 + 3*buttonSeparation + 5* buttonHeight / 2, (int) (getWidth() * 0.9), buttonHeight, "MAIN MENU", new Execution() {
            @Override
            public double execute() {
                MyActivity.canvas.myActivity.load();
                (new MainMenu()).execute();
                return 0;
            }
        }));
        buttons.add(new HUDButton(getxCenter(), getyCenter() - getHeight() / 2 + 4*buttonSeparation + 7* buttonHeight / 2, (int) (getWidth() * 0.9), buttonHeight, "EXIT GAME", new Execution() {
            @Override
            public double execute() {
                System.exit(0);
                return 0;
            }
        }));
        MyActivity.hudElements.addAll(buttons);
    }

    public void removeButtons(){
        MyActivity.hudElements.removeAll(buttons);
    }
}
