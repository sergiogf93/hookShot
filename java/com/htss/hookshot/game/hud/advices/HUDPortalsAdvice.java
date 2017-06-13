package com.htss.hookshot.game.hud.advices;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.persistence.GameStrings;

/**
 * Created by Sergio on 12/06/2017.
 */
public class HUDPortalsAdvice extends HUDAdvice {

    public HUDPortalsAdvice() {
        super(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2, (int) (MyActivity.screenWidth * 0.7), GameStrings.getPortalAdvice1(), (int) (MyActivity.TILE_WIDTH * 0.3));
    }

    @Override
    public void check() {
        if (!MyActivity.hudElements.contains(this)) {
            switch (getState()) {
                case 0:
                    if (MyActivity.character.getPowerUps().get(GamePowerUp.PORTAL) == 1) {
                        start();
                    }
                    break;
                case 1:
                    if (MyActivity.character.getCurrentPowerUp() == GamePowerUp.PORTAL) {
                        setText(GameStrings.getPortalAdvice2());
                        start();
                    }
                    break;
                case 2:
                    if (MyActivity.character.getPortals().size() == 2) {
                        setText(GameStrings.getPortalAdvice3());
                        start();
                    }
                    break;
                case 3:
                    finish();
                    break;
            }
        }
    }
}
