package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;

import com.htss.hookshot.game.object.miscellaneous.PortalArt;

/**
 * Created by Sergio on 05/06/2017.
 */
public class PortalPowerUp extends GamePowerUp {

    // Looks like the portals it places
    private final PortalArt art = new PortalArt();

    public PortalPowerUp(double xPos, double yPos, int width, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, width, width*2, GamePowerUp.PORTAL, addToGameObjects, addToDynamicObjects);
    }

    @Override
    public void draw(Canvas canvas) {
        drawGlow(canvas);
        // As tall as it was when it was an oval, and bobbing up and down like then
        float dy = (float) getDy(getFrame(), getHeight() / 4);
        art.draw(canvas, (float) getxPosInScreen(), (float) getyPosInScreen() + dy, getHeight() / 2f, getFrame(), true);
    }

}
