package com.htss.hookshot.game.hud.advices;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.persistence.GameStrings;

/**
 * Created by Sergio on 12/06/2017.
 */
public class HUDJumpsAdvice extends HUDAdvice {

    public HUDJumpsAdvice(int state) {
        super(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2, (int) (MyActivity.screenWidth * 0.7), GameStrings.getInfiniteJumpsAdvice1(), (int) (MyActivity.TILE_WIDTH * 0.3),state);
        MyActivity.canvas.myActivity.jumpsAdvice = getState();
    }

    @Override
    public void setState(int state) {
        super.setState(state);
        MyActivity.canvas.myActivity.jumpsAdvice = getState();
        MyActivity.canvas.myActivity.saveAdvices();
    }


    @Override
    public void check() {
        if (!MyActivity.hudElements.contains(this)) {
            switch (getState()) {
                case 0:
                    if (MyActivity.character.getPowerUps().get(GamePowerUp.INFINITE_JUMPS) == 1) {
                        start();
                    }
                    break;
                case 1:
                    finish();
                    break;
            }
        }
    }
}
