package com.htss.hookshot.game.hud.advices;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.HUDButton;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.persistence.GameStrings;

/**
 * Created by Sergio on 12/06/2017.
 */
public class HUDNewGameAdvice extends HUDAdvice {

    private HUDButton yesButton, noButton;

    public HUDNewGameAdvice(int size) {
        super(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2, (int) (MyActivity.screenWidth * 0.7), GameStrings.getNewGameStringAdvice(), (int) (MyActivity.TILE_WIDTH * 0.3),0);
        yesButton = new HUDButton(0, 0, 0, size * 2, "YES", new Execution() {
            @Override
            public double execute() {
                finish();
                resume();
                return 0;
            }
        });
        noButton = new HUDButton(0, 0, 0, size * 2, "NO", new Execution() {
            @Override
            public double execute() {
                MyActivity.advices.add(new HUDBasicControlsAdvice());
                MyActivity.advices.add(new HUDPortalsAdvice(0));
                MyActivity.advices.add(new HUDCompassAdvice(0));
                MyActivity.advices.add(new HUDBombsAdvice(0));
                MyActivity.advices.add(new HUDJumpsAdvice(0));
                MyActivity.canvas.myActivity.saveAdvices();
                finish();
                resume();
                return 0;
            }
        });
        placeButtons();
        MyActivity.hudElements.add(yesButton);
        MyActivity.hudElements.add(noButton);
    }

    private void placeButtons() {
        yesButton.setCenter(getxCenter() - getWidth() / 2, getyCenter() + getHeight());
        yesButton.setWidth(getWidth() / 4);
        noButton.setCenter(getxCenter() + getWidth() / 2, getyCenter() + getHeight());
        noButton.setWidth(getWidth() / 4);
    }

    @Override
    public void layoutForScreen() {
        super.layoutForScreen();
        placeButtons();
    }

    @Override
    public void reset() {
        setTouchIndex(-1);
        setTouchId(-1);
        setOn(false);
    }

    @Override
    public void resume() {
        super.resume();
        MyActivity.hudElements.remove(yesButton);
        MyActivity.hudElements.remove(noButton);
    }

    @Override
    public void check() {
        if (!MyActivity.hudElements.contains(this)) {
            start();
        }
    }
}
