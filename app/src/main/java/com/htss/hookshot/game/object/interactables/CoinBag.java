package com.htss.hookshot.game.object.interactables;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.util.DrawUtil;

/**
 * A big gold coin, kept in vaults. It floats where it was put, slowly turning, and bursts into coins when the character
 * reaches it, which fly to it like the ones enemies drop. Nothing bumps into it: it's only ever picked up.
 */
public class CoinBag extends GameDynamicObject implements Interactable {

    private static final float RADIUS = MyActivity.TILE_WIDTH / 4;
    private static final int COINS = 10;
    private static final int GLOW = Color.argb(140, 255, 214, 90), SHINE = Color.rgb(255, 244, 196);

    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final double restY;

    public CoinBag(double xPos, double yPos) {
        super(xPos, yPos, 0, 0, 0, true, false);
        setGhost(true);
        restY = yPos;
    }

    // Bobbing gently where it was put, as it was never meant to fall
    @Override
    public void update() {
        updateFrame();
        setyPosInRoom(restY + RADIUS * 0.15 * Math.sin(2 * Math.PI * getFrame() / 90));
    }

    // Turning slowly, so it's seen as a coin rather than a ball, in a glow that stands out in the dark of a vault
    @Override
    public void draw(Canvas canvas) {
        float x = (float) getxPosInScreen(), y = (float) getyPosInScreen();
        float pulse = 1 + 0.1f * (float) Math.sin(2 * Math.PI * getFrame() / 60);
        DrawUtil.drawGlow(canvas, glowPaint, x, y, RADIUS * 2.4f * pulse, GLOW);
        float spin = Math.abs((float) Math.cos(getFrame() * 0.04));
        Coin.drawCoin(canvas, x, y, RADIUS, 0.3f + 0.7f * spin);
    }

    @Override
    public int getWidth() {
        return (int) (RADIUS * 2);
    }

    @Override
    public int getHeight() {
        return getWidth();
    }

    @Override
    public void detect() {
        if (distanceTo(MyActivity.character) < MyActivity.TILE_WIDTH / 2) {
            destroy();
            MyActivity.character.checkIfRemoveInterest(this);
            for (int i = 0; i < COINS; i++) {
                new Coin(getxPosInRoom(), getyPosInRoom());
            }
            Particles.burst(getxPosInRoom(), getyPosInRoom(), 16, SHINE, 0.07f, 0.03f, 0.5, 0);
        }
    }
}
