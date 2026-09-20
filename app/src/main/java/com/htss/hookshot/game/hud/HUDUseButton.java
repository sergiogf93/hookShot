package com.htss.hookshot.game.hud;

import android.graphics.Canvas;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;
import com.htss.hookshot.game.object.miscellaneous.PortalObject;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.util.DrawUtil;

import java.util.HashMap;

/**
 * The button that uses the power that's picked, which it shows, with how many are left, so it says what pressing it
 * will do. Standing in a portal that leads somewhere, it shows the portal it steps through. With nothing to use it's
 * drawn faint with a dashed edge.
 */
public class HUDUseButton extends HUDCircleButton {

    private final HashMap<Integer, GamePowerUp> icons = new HashMap<Integer, GamePowerUp>();
    private float iconsRadius = -1;

    public HUDUseButton(float radius, Execution execOn) {
        super(0, 0, radius, "USE", true, execOn);
    }

    // The button changes size with the controls in use, and its icons with it
    private void makeIcons() {
        iconsRadius = getRadius();
        int size = (int) (getRadius() * 0.95f);
        icons.put(GamePowerUp.PORTAL, new PortalPowerUp(0, 0, (int) (size * 0.62), false, false));
        icons.put(GamePowerUp.COMPASS, new CompassPowerUp(0, 0, size, false, false));
        icons.put(GamePowerUp.BOMB, new BombPowerUp(0, 0, size, false, false));
        icons.put(GamePowerUp.INFINITE_JUMPS, new InfiniteJumpsPowerUp(0, 0, (int) (size * 1.1), size, false, false));
    }

    @Override
    public void draw(Canvas canvas) {
        if (MyActivity.character == null) {
            return;
        }
        if (iconsRadius != getRadius()) {
            makeIcons();
        }
        int type = isInPortal() ? GamePowerUp.PORTAL : MyActivity.character.getCurrentPowerUp();
        GamePowerUp icon = icons.get(type);
        float x = getxCenter(), y = getyCenter(), r = getRadius();
        if (icon == null) {
            UiStyle.drawIdleControl(canvas, getPaint(), x, y, r);
            getPaint().setTypeface(MyActivity.canvas.arcadeClassicFont);
            getPaint().setTextSize(r * 0.6f);
            UiStyle.drawText(canvas, getPaint(), getText(), x - getPaint().measureText(getText()) / 2, y + r * 0.2f, DrawUtil.withAlpha(UiStyle.IDLE, 170));
            return;
        }
        UiStyle.drawControl(canvas, getPaint(), x, y, r, isOn(), UiStyle.GOLD);
        // Icons are placed in the cave, so they follow the camera to stay on the button
        icon.setPositionInRoom(x - MyActivity.canvas.dx, y - MyActivity.canvas.dy);
        icon.draw(canvas);
        Integer quantity = MyActivity.character.getPowerUps().get(type);
        if (quantity != null && quantity > 1 && !isInPortal()) {
            getPaint().setTypeface(MyActivity.canvas.joystickMonospace);
            getPaint().setTextSize(r * 0.42f);
            UiStyle.drawText(canvas, getPaint(), String.valueOf(quantity), x + r * 0.45f, y + r * 0.85f, UiStyle.TEXT);
        }
    }

    // Standing in a portal with a twin to come out of, which pressing steps through
    private boolean isInPortal() {
        for (PortalObject portal : MyActivity.character.getPortals()) {
            if (portal.getTwinPortal() != null && MyActivity.character.distanceTo(portal) < portal.getRadius()) {
                return true;
            }
        }
        return false;
    }
}
