package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.enemies.EnemyDeepWorm;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.interactables.Coin;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;
import com.htss.hookshot.util.TimeUtil;

import java.util.HashMap;

/**
 * The level, coins and health in the top left corner, and the equipped power-up in the top right one.
 */
public class HUDStatus extends HUDElement {

    private static final int MARGIN = MyActivity.TILE_WIDTH / 4, TEXT_SIZE = MyActivity.TILE_WIDTH / 3,
            SHADOW = MyActivity.TILE_WIDTH / 40, BAR_WIDTH = MyActivity.TILE_WIDTH * 3,
            BAR_HEIGHT = MyActivity.TILE_WIDTH / 8, ICON_SIZE = (int) (MyActivity.TILE_WIDTH * 0.4);
    // Where the level and health end, for what's drawn below them
    public static final int BOTTOM = MARGIN + TEXT_SIZE + MARGIN / 2 + BAR_HEIGHT;

    // The bar slides to the health, and a lighter trail shows what was just lost
    private static final double BAR_EASING = 0.2, TRAIL_DRAIN = 0.006;
    private static final int BOSS_BAR = Color.rgb(255, 150, 40);
    // The coins swell for a moment when some are picked up
    private static final int COIN_POP = (int) TimeUtil.secondsToUpdates(0.25);
    private static final float COIN_RADIUS = TEXT_SIZE * 0.4f;

    private HashMap<Integer, GamePowerUp> icons = new HashMap<Integer, GamePowerUp>();
    private Rect bar = new Rect();
    private double shownFill = -1, trailFill = -1;
    private int shownCoins = -1, coinPop = 0;

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
        String level = MyActivity.getPlaceName();
        drawText(canvas, level, MARGIN, MARGIN + TEXT_SIZE);
        drawHealth(canvas);
        drawCoins(canvas, MARGIN * 2 + getPaint().measureText(level));
        // With the other controls, the button that uses the power shows which one is picked
        if (MyActivity.controls == MyActivity.CONTROLS_CLASSIC) {
            drawPowerUp(canvas);
        }
        drawBoss(canvas);
    }

    // The deep worm's name and what's left of it, at the top in the middle, once the character met it
    private void drawBoss(Canvas canvas) {
        for (GameEnemy enemy : MyActivity.enemies) {
            if (enemy instanceof EnemyDeepWorm && ((EnemyDeepWorm) enemy).isAwake()) {
                String name = "DEEP WORM";
                int middle = MyActivity.screenWidth / 2;
                drawText(canvas, name, middle - getPaint().measureText(name) / 2, MARGIN + TEXT_SIZE);
                bar.set(middle - BAR_WIDTH / 2, BOTTOM - BAR_HEIGHT, middle + BAR_WIDTH / 2, BOTTOM);
                setColor(Color.BLACK);
                canvas.drawRect(bar, getPaint());
                bar.right = (int) (bar.left + BAR_WIDTH * ((EnemyDeepWorm) enemy).getHealthShare());
                setColor(BOSS_BAR);
                canvas.drawRect(bar, getPaint());
                return;
            }
        }
    }

    private void drawHealth(Canvas canvas) {
        double fill = Math.max(0, MyActivity.character.getHealth() / MyActivity.character.getMaxHealth());
        if (shownFill < 0) {
            shownFill = fill;
            trailFill = fill;
        }
        shownFill += (fill - shownFill) * BAR_EASING;
        trailFill = Math.max(shownFill, trailFill - TRAIL_DRAIN);
        bar.set(MARGIN, BOTTOM - BAR_HEIGHT, MARGIN + BAR_WIDTH, BOTTOM);
        setColor(Color.BLACK);
        canvas.drawRect(bar, getPaint());
        bar.right = (int) (MARGIN + BAR_WIDTH * trailFill);
        setColor(Color.rgb(255, 240, 200));
        canvas.drawRect(bar, getPaint());
        bar.right = (int) (MARGIN + BAR_WIDTH * shownFill);
        setColor(HUDBar.getHealthColor(fill));
        canvas.drawRect(bar, getPaint());
    }

    // A coin and how many there are, on the level's line from the given point
    private void drawCoins(Canvas canvas, float left) {
        int coins = MyActivity.character.getCoins();
        if (shownCoins >= 0 && coins > shownCoins) {
            coinPop = COIN_POP;
        }
        shownCoins = coins;
        float middle = MARGIN + TEXT_SIZE * 0.62f, coinX = left + COIN_RADIUS;
        canvas.save();
        if (coinPop > 0) {
            float swell = 1 + 0.3f * coinPop / COIN_POP;
            canvas.scale(swell, swell, coinX, middle);
            coinPop--;
        }
        Coin.drawCoin(canvas, coinX, middle, COIN_RADIUS, 1);
        canvas.restore();
        drawText(canvas, String.valueOf(coins), coinX + COIN_RADIUS + MARGIN / 3f, MARGIN + TEXT_SIZE);
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
