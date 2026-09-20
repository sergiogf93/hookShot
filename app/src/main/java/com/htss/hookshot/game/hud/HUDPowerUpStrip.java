package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.util.DrawUtil;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * The powers the character is carrying, in a row beside the button that uses them, or a column down the edge of the
 * screen. Tapping one picks it up for that button, and tapping the one already picked puts it away, all without
 * stopping the game.
 */
public class HUDPowerUpStrip extends HUDElement implements Clickable {

    private static final int[] ORDER = {GamePowerUp.PORTAL, GamePowerUp.COMPASS, GamePowerUp.BOMB, GamePowerUp.INFINITE_JUMPS};

    private final int slot = (int) (MyActivity.TILE_WIDTH * 0.62), spacing = (int) (MyActivity.TILE_WIDTH * 0.74);
    private final HashMap<Integer, GamePowerUp> icons = new HashMap<Integer, GamePowerUp>();
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean clickable = true, on = false, vertical = false;
    private int touchId = -1, touchIndex = -1, pressedType = -1;

    public HUDPowerUpStrip() {
        super(0, 0, 0, (int) (MyActivity.TILE_WIDTH * 0.74));
        int size = (int) (MyActivity.TILE_WIDTH * 0.36);
        icons.put(GamePowerUp.PORTAL, new PortalPowerUp(0, 0, (int) (size * 0.62), false, false));
        icons.put(GamePowerUp.COMPASS, new CompassPowerUp(0, 0, size, false, false));
        icons.put(GamePowerUp.BOMB, new BombPowerUp(0, 0, size, false, false));
        icons.put(GamePowerUp.INFINITE_JUMPS, new InfiniteJumpsPowerUp(0, 0, (int) (size * 1.1), size, false, false));
    }

    // The powers being carried, in a fixed order, so a slot doesn't move under the thumb when another is picked up
    private LinkedList<Integer> carried() {
        LinkedList<Integer> types = new LinkedList<Integer>();
        if (MyActivity.character == null) {
            return types;
        }
        for (int i = 0; i < ORDER.length; i++) {
            Integer quantity = MyActivity.character.getPowerUps().get(ORDER[i]);
            if (quantity != null && quantity > 0) {
                types.add(ORDER[i]);
            }
        }
        return types;
    }

    // In a row, slots run leftwards from the strip's centre, which sits beside the button that uses them. In a column
    // they run down from it
    private int slotX(int index) {
        return vertical ? getxCenter() : getxCenter() - index * spacing;
    }

    private int slotY(int index) {
        return vertical ? getyCenter() + index * spacing : getyCenter();
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    @Override
    public void draw(Canvas canvas) {
        LinkedList<Integer> types = carried();
        for (int i = 0; i < types.size(); i++) {
            int type = types.get(i);
            boolean picked = MyActivity.character.getCurrentPowerUp() == type;
            int x = slotX(i), y = slotY(i);
            if (picked) {
                DrawUtil.drawGlow(canvas, glowPaint, x, y, slot * 0.75f, DrawUtil.withAlpha(UiStyle.getAccent(), 130));
            }
            UiStyle.drawCircle(canvas, getPaint(), x, y, slot / 2, picked || type == pressedType);
            GamePowerUp icon = icons.get(type);
            // Icons are placed in the cave, so they follow the camera to stay in the corner
            icon.setPositionInRoom(x - MyActivity.canvas.dx, y - MyActivity.canvas.dy);
            icon.draw(canvas);
            icon.updateFrame();
            Integer quantity = MyActivity.character.getPowerUps().get(type);
            if (quantity != null && quantity > 1) {
                getPaint().setTypeface(MyActivity.canvas.joystickMonospace);
                getPaint().setTextSize(MyActivity.TILE_WIDTH * 0.18f);
                UiStyle.drawText(canvas, getPaint(), String.valueOf(quantity), x + slot * 0.2f, y + slot * 0.55f, UiStyle.TEXT);
            }
        }
    }

    private int typeAt(double x, double y) {
        LinkedList<Integer> types = carried();
        for (int i = 0; i < types.size(); i++) {
            if (Math.abs(x - slotX(i)) <= slot * 0.6 && Math.abs(y - slotY(i)) <= slot * 0.6) {
                return types.get(i);
            }
        }
        return -1;
    }

    @Override
    public boolean pressed(double x, double y) {
        return typeAt(x, y) >= 0;
    }

    @Override
    public void press(double x, double y, int id, int index) {
        touchId = id;
        touchIndex = index;
        on = true;
        pressedType = typeAt(x, y);
    }

    // Picked up when the finger lifts, so a slip off the slot doesn't change the power
    @Override
    public void reset() {
        if (pressedType >= 0 && MyActivity.character != null) {
            boolean picked = MyActivity.character.getCurrentPowerUp() == pressedType;
            MyActivity.character.equipPowerUp(picked ? -1 : pressedType);
        }
        touchId = -1;
        touchIndex = -1;
        on = false;
        pressedType = -1;
    }

    @Override
    public boolean isOn() {
        return on;
    }

    @Override
    public boolean isClickable() {
        return clickable;
    }

    @Override
    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    @Override
    public int getTouchId() {
        return touchId;
    }

    @Override
    public int getTouchIndex() {
        return touchIndex;
    }
}
