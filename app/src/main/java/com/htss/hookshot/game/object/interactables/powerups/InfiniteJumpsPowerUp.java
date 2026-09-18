package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Canvas;
import android.graphics.Color;

import com.htss.hookshot.game.object.miscellaneous.JumpEffect;
import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 08/06/2017.
 */
public class InfiniteJumpsPowerUp extends GamePowerUp {

    // Three of the rings every jump leaves under the character, stacked and shrinking upwards, with an arrow on top.
    // They light up in turn from the bottom, like jumps one after another
    private static final int OUTLINE = Color.rgb(10, 58, 71), BOTTOM = Color.rgb(43, 184, 214), MIDDLE = Color.rgb(86, 203, 228),
            TOP = Color.rgb(154, 233, 247), ARROW = Color.rgb(234, 252, 255);
    private static final int UPDATES_PER_STEP = 10, STEPS = 5;

    public InfiniteJumpsPowerUp(double xPos, double yPos, int width, int height, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, width, height, INFINITE_JUMPS, addToGameObjects, addToDynamicObjects);
    }

    @Override
    public void draw(Canvas canvas) {
        drawGlow(canvas);
        // The rings as wide as the old arcs
        beginIcon(canvas, getWidth() * 100 / 60f, 42, 0);
        int lit = (getFrame() / UPDATES_PER_STEP) % STEPS;
        drawRing(canvas, 50, 70, 30, 12, 10, 6, lightIf(BOTTOM, lit == 0));
        drawRing(canvas, 50, 50, 23, 9, 8, 4.5f, lightIf(MIDDLE, lit == 1));
        drawRing(canvas, 50, 33, 16, 6, 6, 3, lightIf(TOP, lit == 2));
        polygon(50, 7, 58, 18, 42, 18);
        fillPath(canvas, iconPath, lightIf(ARROW, lit == 3));
        strokePath(canvas, iconPath, OUTLINE, 2.5f);
        canvas.restore();
    }

    private void drawRing(Canvas canvas, float cx, float cy, float rx, float ry, float outlineWidth, float width, int color) {
        JumpEffect.drawRing(canvas, iconPaint, iconOval, cx, cy, rx, ry, outlineWidth, width, color);
    }

    private static int lightIf(int color, boolean lit) {
        return lit ? DrawUtil.blend(color, Color.WHITE, 0.55f) : color;
    }

}
