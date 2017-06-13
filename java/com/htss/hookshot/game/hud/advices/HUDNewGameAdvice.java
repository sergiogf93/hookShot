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

    public HUDNewGameAdvice(int xCenter, int yCenter, int width, int size) {
        super(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2, (int) (MyActivity.screenWidth * 0.7), GameStrings.getNewGameStringAdvice(), (int) (MyActivity.TILE_WIDTH * 0.3));
        yesButton = new HUDButton(getxCenter() - getWidth()/2, getyCenter() + getHeight(), getWidth() / 4, size * 2, "YES", new Execution() {
            @Override
            public double execute() {
                finish();
                resume();
                return 0;
            }
        });
        noButton = new HUDButton(getxCenter() + getWidth()/2, getyCenter() + getHeight(), getWidth() / 4, size * 2, "NO", new Execution() {
            @Override
            public double execute() {
                MyActivity.advices.add(new HUDBasicControlsAdvice());
                MyActivity.advices.add(new HUDPortalsAdvice());
                MyActivity.advices.add(new HUDCompassAdvice());
                MyActivity.advices.add(new HUDBombsAdvice());
                MyActivity.advices.add(new HUDJumpsAdvice());
                finish();
                resume();
                return 0;
            }
        });
        MyActivity.hudElements.add(yesButton);
        MyActivity.hudElements.add(noButton);
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
