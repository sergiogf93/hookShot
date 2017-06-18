package com.htss.hookshot.game.hud.advices;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.persistence.GameStrings;

/**
 * Created by Sergio on 12/06/2017.
 */
public class HUDBombsAdvice extends HUDAdvice {

    public HUDBombsAdvice(int state) {
        super(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2, (int) (MyActivity.screenWidth * 0.7), GameStrings.getBombsAdvice1(), (int) (MyActivity.TILE_WIDTH * 0.3),state);
        MyActivity.canvas.myActivity.bombAdvice = getState();
    }

    @Override
    public void setState(int state) {
        super.setState(state);
        MyActivity.canvas.myActivity.bombAdvice = getState();
        MyActivity.canvas.myActivity.saveAdvices();
    }

    @Override
    public void check() {
        if (!MyActivity.hudElements.contains(this)) {
            switch (getState()) {
                case 0:
                    if (MyActivity.character.getPowerUps().get(GamePowerUp.BOMB) == 1) {
                        start();
                    }
                    break;
                case 1:
                    if (MyActivity.character.getCurrentPowerUp() == GamePowerUp.BOMB) {
                        setText(GameStrings.getBombsAdvice2());
                        start();
                    }
                    break;
                case 2:
                    finish();
                    break;
            }
        }
    }
}
