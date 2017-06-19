package com.htss.hookshot.game.hud.advices;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.persistence.GameStrings;

/**
 * Created by Sergio on 12/06/2017.
 */
public class HUDBasicControlsAdvice extends HUDAdvice {

    public HUDBasicControlsAdvice() {
        super(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2, (int) (MyActivity.screenWidth * 0.7), GameStrings.getBasicControlsAdvice(), (int) (MyActivity.TILE_WIDTH * 0.3),0);
    }

    @Override
    public void check() {
        if (!MyActivity.hudElements.contains(this)) {
            switch (getState()) {
                case 0:
                    start();
                    break;
                case 1:
                    setText(GameStrings.getHookAdvice1());
                    start();
                    break;
                case 2:
                    if (MyActivity.character.isHooked()) {
                        setText(GameStrings.getHookAdvice2());
                        start();
                    }
                    break;
                case 3:
                    if (!MyActivity.character.isHooked()) {
                        setText(GameStrings.getHookAdvice3());
                        start();
                    }
                    break;
                case 4:
                    finish();
                    break;
            }
        }
    }
}
