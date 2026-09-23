package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.interactables.Coin;
import com.htss.hookshot.game.object.interactables.HealthDrop;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;
import com.htss.hookshot.interfaces.Execution;

import java.util.Vector;

/**
 * What a stall in the cave sells, opened from it with the game paused: each power and some health, with what it costs
 * and how many the character has, and the coins it has to spend. What it can't buy, for want of coins or because its
 * health is full, is shown faded. Buying keeps the menu open, to buy more.
 */
public class HUDShopMenu extends HUDElement {

    // The powers' kinds, and health, which isn't one
    private static final int HEALTH = -1;
    private static final int[] KINDS = {GamePowerUp.BOMB, GamePowerUp.COMPASS, GamePowerUp.INFINITE_JUMPS, GamePowerUp.PORTAL, HEALTH};
    private static final String[] NAMES = {"BOMB", "COMPASS", "SWIFTNESS", "PORTALS", "HEALTH"};
    private static final int[] PRICES = {12, 10, 15, 20, 15};
    private static final double HEALTH_BOUGHT = 50;
    private static final int FADED = Color.argb(150, 0, 0, 0);

    private final Vector<HUDButton> buttons = new Vector<HUDButton>();
    private final GameDynamicObject[] icons = new GameDynamicObject[KINDS.length];
    private final RectF panel = new RectF(), shade = new RectF();
    private int cellWidth, cellHeight, gap, buttonHeight;

    public HUDShopMenu() {
        super(0, 0, 0, 0);
        getPaint().setTypeface(MyActivity.canvas.joystickMonospace);
    }

    public void open() {
        layout();
        makeIcons();
        buttons.clear();
        for (int i = 0; i < KINDS.length; i++) {
            final int item = i;
            HUDButton buy = new HUDButton(getCellX(i), (int) (panel.top + gap + cellHeight - buttonHeight / 2), cellWidth - gap, buttonHeight, "BUY", null) {
                @Override
                public void draw(Canvas canvas) {
                    super.draw(canvas);
                    if (!canBuy(item)) {
                        shade.set(getxCenter() - getWidth() / 2f, getyCenter() - getHeight() / 2f, getxCenter() + getWidth() / 2f, getyCenter() + getHeight() / 2f);
                        getPaint().setColor(FADED);
                        canvas.drawRoundRect(shade, getHeight() / 2f, getHeight() / 2f, getPaint());
                    }
                }
            };
            buy.setExecOff(new Execution() {
                @Override
                public double execute() {
                    buy(item);
                    return 0;
                }
            });
            buttons.add(buy);
        }
        buttons.add(new HUDButton(MyActivity.screenWidth / 2, (int) (panel.bottom + gap + buttonHeight / 2), cellWidth * 2, buttonHeight, "LEAVE", new Execution() {
            @Override
            public double execute() {
                MyActivity.unpause();
                return 0;
            }
        }));
        MyActivity.hudElements.add(this);
        MyActivity.hudElements.addAll(buttons);
    }

    public void close() {
        MyActivity.hudElements.remove(this);
        MyActivity.hudElements.removeAll(buttons);
    }

    // A row of items in the middle of the screen, as wide as fits, and the way out under it
    private void layout() {
        gap = MyActivity.TILE_WIDTH / 5;
        buttonHeight = (int) (MyActivity.TILE_WIDTH * 0.75);
        cellWidth = Math.min((int) (MyActivity.TILE_WIDTH * 2.4), (MyActivity.screenWidth - MyActivity.TILE_WIDTH - gap * (KINDS.length + 1)) / KINDS.length);
        cellHeight = (int) (MyActivity.TILE_WIDTH * 3.3);
        int width = cellWidth * KINDS.length + gap * (KINDS.length + 1), height = cellHeight + gap * 2;
        setCenter(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2 - buttonHeight / 2);
        panel.set(getxCenter() - width / 2f, getyCenter() - height / 2f, getxCenter() + width / 2f, getyCenter() + height / 2f);
    }

    private int getCellX(int item) {
        return (int) (panel.left + gap + item * (cellWidth + gap) + cellWidth / 2);
    }

    // Drawn like the ones in the cave, but without their glow, as the ones in the HUD are
    private void makeIcons() {
        int size = (int) (MyActivity.TILE_WIDTH * 0.8);
        for (int i = 0; i < KINDS.length; i++) {
            switch (KINDS[i]) {
                case GamePowerUp.PORTAL:
                    icons[i] = new PortalPowerUp(0, 0, (int) (size * 0.62), false, false);
                    break;
                case GamePowerUp.COMPASS:
                    icons[i] = new CompassPowerUp(0, 0, size, false, false);
                    break;
                case GamePowerUp.BOMB:
                    icons[i] = new BombPowerUp(0, 0, size, false, false);
                    break;
                case GamePowerUp.INFINITE_JUMPS:
                    icons[i] = new InfiniteJumpsPowerUp(0, 0, (int) (size * 1.1), size, false, false);
                    break;
                default:
                    icons[i] = new HealthDrop(0, 0, false, false);
            }
        }
    }

    private boolean canBuy(int item) {
        if (MyActivity.character.getCoins() < PRICES[item]) {
            return false;
        }
        return KINDS[item] != HEALTH || MyActivity.character.getHealth() < MyActivity.character.getMaxHealth();
    }

    private void buy(int item) {
        if (!canBuy(item)) {
            return;
        }
        MyActivity.character.setCoins(MyActivity.character.getCoins() - PRICES[item]);
        if (KINDS[item] == HEALTH) {
            MyActivity.character.addHealth(HEALTH_BOUGHT);
        } else {
            MyActivity.character.addPowerUp(KINDS[item]);
        }
        Particles.burst(MyActivity.character.getxPosInRoom(), MyActivity.character.getyPosInRoom(), 10, UiStyle.GOLD, 0.05f, 0.02f, 0.4, 0);
        // The open world is saved as it goes. A game of caves one after another keeps what it had as a cave began,
        // as it always has
        if (MyActivity.openWorld) {
            MyActivity.canvas.myActivity.saveOpenWorld();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        float corner = MyActivity.TILE_WIDTH / 3f;
        UiStyle.drawPanel(canvas, getPaint(), panel, corner);
        getPaint().setTextSize(MyActivity.TILE_WIDTH * 0.45f);
        String title = "SHOP";
        float titleY = panel.top - MyActivity.TILE_WIDTH * 0.25f;
        UiStyle.drawText(canvas, getPaint(), title, getxCenter() - getPaint().measureText(title) / 2, titleY, UiStyle.TEXT);
        drawPrice(canvas, String.valueOf(MyActivity.character.getCoins()), panel.right - corner, titleY, MyActivity.TILE_WIDTH * 0.4f, true);

        // From the top of each cell: the icon, the name, how many it has, and the price over the button
        float nameSize = MyActivity.TILE_WIDTH * 0.24f, priceSize = MyActivity.TILE_WIDTH * 0.3f;
        float iconY = panel.top + gap + MyActivity.TILE_WIDTH * 0.6f, nameY = iconY + MyActivity.TILE_WIDTH * 0.85f;
        float priceY = panel.top + gap + cellHeight - buttonHeight - gap * 0.9f;
        for (int i = 0; i < KINDS.length; i++) {
            float x = getCellX(i);
            // Icons are placed in the cave, so they follow the camera to stay in the menu. The health drop is small
            // there, so it's drawn bigger
            icons[i].setPositionInRoom(x - MyActivity.canvas.dx, iconY - MyActivity.canvas.dy);
            canvas.save();
            if (KINDS[i] == HEALTH) {
                canvas.scale(2, 2, x, iconY);
            }
            icons[i].draw(canvas);
            canvas.restore();
            getPaint().setTextSize(nameSize);
            UiStyle.drawText(canvas, getPaint(), NAMES[i], x - getPaint().measureText(NAMES[i]) / 2, nameY, UiStyle.TEXT);
            String owned = getOwned(i);
            UiStyle.drawText(canvas, getPaint(), owned, x - getPaint().measureText(owned) / 2, nameY + nameSize * 1.5f, UiStyle.IDLE);
            drawPrice(canvas, String.valueOf(PRICES[i]), x, priceY, priceSize, false);
        }
    }

    // How many of a power the character has, or its health
    private String getOwned(int item) {
        if (KINDS[item] == HEALTH) {
            return (int) Math.ceil(MyActivity.character.getHealth()) + "/" + (int) MyActivity.character.getMaxHealth();
        }
        Integer count = MyActivity.character.getPowerUps().get(KINDS[item]);
        return "HAVE " + ((count == null) ? 0 : count);
    }

    // A coin and a number, centered on the given point or ending at it
    private void drawPrice(Canvas canvas, String amount, float x, float baseline, float size, boolean toTheLeft) {
        getPaint().setTextSize(size);
        float radius = size * 0.45f, width = radius * 2 + size * 0.25f + getPaint().measureText(amount);
        float left = toTheLeft ? x - width : x - width / 2;
        Coin.drawCoin(canvas, left + radius, baseline - size * 0.38f, radius, 1);
        UiStyle.drawText(canvas, getPaint(), amount, left + radius * 2 + size * 0.25f, baseline, UiStyle.GOLD);
    }

    @Override
    public boolean pressed(double x, double y) {
        return false;
    }
}
