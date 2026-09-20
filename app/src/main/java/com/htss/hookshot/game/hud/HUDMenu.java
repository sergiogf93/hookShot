package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;

import com.htss.hookshot.executions.LaunchGame;
import com.htss.hookshot.executions.MainMenu;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.interactables.Coin;
import com.htss.hookshot.interfaces.Execution;

import java.util.Vector;

/**
 * Created by Sergio on 03/06/2017.
 */
public class HUDMenu extends HUDElement {

    public static final int MENU_ALPHA = 150;

    private RectF background = new RectF();
    private Vector<HUDButton> buttons = new Vector<HUDButton>();
    private int buttonHeight, buttonSeparation;

    public HUDMenu(int xCenter, int yCenter, int width, int height, int buttonHeight, int buttonSeparation) {
        super(xCenter, yCenter, width, height);
        this.buttonHeight = buttonHeight;
        this.buttonSeparation = buttonSeparation;
        getPaint().setTypeface(MyActivity.canvas.joystickMonospace);
        getPaint().setTextSize(buttonHeight / 3);
    }

    @Override
    public void draw(Canvas canvas) {
        drawBackground(canvas);
        drawLevel(canvas);
        drawHealth(canvas);
    }

    private void drawBackground(Canvas canvas) {
        this.background.set(getxCenter() - getWidth() / 2, getyCenter() - getHeight() / 2, getxCenter() + getWidth() / 2, getyCenter() + getHeight() / 2);
        UiStyle.drawPanel(canvas, getPaint(), this.background, MyActivity.TILE_WIDTH / 3f);
    }

    // The level, then a coin and how many there are, together in the middle
    private void drawLevel(Canvas canvas) {
        String level = "LEVEL " + MyActivity.canvas.myActivity.level, coins = String.valueOf(MyActivity.character.getCoins());
        float size = getPaint().getTextSize(), radius = size * 0.4f, levelWidth = getPaint().measureText(level);
        float width = levelWidth + size * 0.75f + radius * 2 + size / 4 + getPaint().measureText(coins);
        float left = getxCenter() - width / 2, baseline = getyCenter() - getHeight() / 2f - size;
        UiStyle.drawText(canvas, getPaint(), level, left, baseline, UiStyle.TEXT);
        float coinX = left + levelWidth + size * 0.75f + radius;
        Coin.drawCoin(canvas, coinX, baseline - size * 0.38f, radius, 1);
        UiStyle.drawText(canvas, getPaint(), coins, coinX + radius + size / 4, baseline, UiStyle.TEXT);
    }

    private void drawHealth(Canvas canvas) {
        Rect backBar = new Rect(getxCenter() - getWidth() / 2, (int) (getyCenter() - getHeight() / 2 - 2 * getPaint().getTextSize() / 3), getxCenter() + getWidth() / 2, (int) (getyCenter() - getHeight() / 2 - getPaint().getTextSize()/3));
        double fill = MyActivity.character.getHealth() / MyActivity.character.getMaxHealth();
        Rect healthBar = new Rect(getxCenter() - getWidth() / 2, (int) (getyCenter() - getHeight() / 2 - 2 * getPaint().getTextSize() / 3), (int) (getxCenter() - getWidth() / 2 + getWidth() * fill), (int) (getyCenter() - getHeight() / 2 - getPaint().getTextSize() / 3));
        setColor(Color.BLACK);
        canvas.drawRect(backBar, getPaint());
        setColor(HUDBar.getHealthColor(fill));
        canvas.drawRect(healthBar, getPaint());
    }

    public void addMenuButtons() {
        buttons.clear();
        // Switching the controls takes effect as soon as the game carries on, so a change can be tried straight away
        final HUDButton controls = new HUDButton(getxCenter(), getyCenter() - getHeight() / 2 + buttonSeparation + buttonHeight / 2, (int) (getWidth() * 0.9), buttonHeight, controlsText(), null);
        controls.setExecOff(new Execution() {
            @Override
            public double execute() {
                MyActivity.setControls((MyActivity.controls + 1) % MyActivity.CONTROL_NAMES.length);
                controls.setText(controlsText());
                return 0;
            }
        });
        buttons.add(controls);
        buttons.add(new HUDButton(getxCenter(), getyCenter() - getHeight() / 2 + 2 * buttonSeparation + 3 * buttonHeight / 2, (int) (getWidth() * 0.9), buttonHeight, "MAIN MENU", new Execution() {
            @Override
            public double execute() {
                (new MainMenu()).execute();
                return 0;
            }
        }));
        buttons.add(new HUDButton(getxCenter(), getyCenter() - getHeight() / 2 + 3 * buttonSeparation + 5 * buttonHeight / 2, (int) (getWidth() * 0.9), buttonHeight, "EXIT GAME", new Execution() {
            @Override
            public double execute() {
                MyActivity.canvas.myActivity.finish();
                return 0;
            }
        }));
        MyActivity.hudElements.addAll(buttons);
    }

    private static String controlsText() {
        return "CONTROLS: " + MyActivity.CONTROL_NAMES[MyActivity.controls];
    }

    public void removeButtons(){
        MyActivity.hudElements.removeAll(buttons);
    }
}
