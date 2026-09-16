package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;

import java.util.HashMap;

/**
 * The level and health in the top left corner, and the equipped power-up in the top right one.
 */
public class HUDStatus extends HUDElement {

    private static final int MARGIN = MyActivity.TILE_WIDTH / 4, TEXT_SIZE = MyActivity.TILE_WIDTH / 3,
            SHADOW = MyActivity.TILE_WIDTH / 40, BAR_WIDTH = MyActivity.TILE_WIDTH * 3,
            BAR_HEIGHT = MyActivity.TILE_WIDTH / 8, ICON_SIZE = (int) (MyActivity.TILE_WIDTH * 0.4);
    // Where the level and health end, for what's drawn below them
    public static final int BOTTOM = MARGIN + TEXT_SIZE + MARGIN / 2 + BAR_HEIGHT;

    private HashMap<Integer, GamePowerUp> icons = new HashMap<Integer, GamePowerUp>();
    private Rect bar = new Rect();

    public HUDStatus() {
        super(0, 0, 0, 0);
        getPaint().setTypeface(MyActivity.canvas.joystickMonospace);
        getPaint().setTextSize(TEXT_SIZE);
        // Half the size they have in the pause menu
        icons.put(GamePowerUp.PORTAL, new PortalPowerUp(0, 0, ICON_SIZE * 5 / 8, false, false));
        icons.put(GamePowerUp.COMPASS, new CompassPowerUp(0, 0, ICON_SIZE, false, false));
        icons.put(GamePowerUp.BOMB, new BombPowerUp(0, 0, ICON_SIZE, false, false));
        icons.put(GamePowerUp.INFINITE_JUMPS, new InfiniteJumpsPowerUp(0, 0, ICON_SIZE * 9 / 8, ICON_SIZE, false, false));
    }

    @Override
    public void draw(Canvas canvas) {
        // The pause menu shows the same, bigger
        if (MyActivity.paused) {
            return;
        }
        drawText(canvas, "LEVEL " + MyActivity.canvas.myActivity.level, MARGIN, MARGIN + TEXT_SIZE);
        drawHealth(canvas);
        drawPowerUp(canvas);
    }

    private void drawHealth(Canvas canvas) {
        double fill = MyActivity.character.getHealth() / MyActivity.character.getMaxHealth();
        bar.set(MARGIN, BOTTOM - BAR_HEIGHT, MARGIN + BAR_WIDTH, BOTTOM);
        setColor(Color.BLACK);
        canvas.drawRect(bar, getPaint());
        bar.right = (int) (MARGIN + BAR_WIDTH * Math.max(fill, 0));
        setColor(HUDBar.getHealthColor(fill));
        canvas.drawRect(bar, getPaint());
    }

    private void drawPowerUp(Canvas canvas) {
        int type = MyActivity.character.getCurrentPowerUp();
        GamePowerUp icon = icons.get(type);
        if (icon == null) {
            return;
        }
        int right = MyActivity.screenWidth - MARGIN;
        int iconCenterY = MARGIN + ICON_SIZE / 2;
        // Like the pause menu, the quantity only shows when there are several
        Integer quantity = MyActivity.character.getPowerUps().get(type);
        if (quantity != null && quantity > 1) {
            String text = "x " + quantity;
            float textWidth = getPaint().measureText(text);
            drawText(canvas, text, right - textWidth, iconCenterY + TEXT_SIZE / 3);
            right -= textWidth + MARGIN / 2;
        }
        // Icons are placed in the cave, so they follow the camera to stay in the corner
        icon.setPositionInRoom(right - ICON_SIZE / 2 - MyActivity.canvas.dx, iconCenterY - MyActivity.canvas.dy);
        icon.draw(canvas);
    }

    private void drawText(Canvas canvas, String text, float x, float y) {
        setColor(Color.BLACK);
        canvas.drawText(text, x + SHADOW, y + SHADOW, getPaint());
        setColor(Color.WHITE);
        canvas.drawText(text, x, y, getPaint());
    }
}
