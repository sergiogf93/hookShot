package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;

import com.htss.hookshot.game.object.miscellaneous.BurstArt;

/**
 * Created by Sergio on 07/06/2017.
 */
public class BombPowerUp extends GamePowerUp {

    // Looks like the explosions it makes
    private final BurstArt art = new BurstArt();

    public BombPowerUp(double xPos, double yPos, int width, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, width, width, GamePowerUp.BOMB, addToGameObjects, addToDynamicObjects);
    }

    @Override
    public void draw(Canvas canvas) {
        drawGlow(canvas);
        // As wide as the old ball with its dark rim
        art.draw(canvas, (float) getxPosInScreen(), (float) getyPosInScreen(), getWidth() * 0.65f, getFrame(), 0);
    }

}
