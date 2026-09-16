package com.htss.hookshot.game.object.hook;

import android.graphics.Canvas;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

/**
 * The chain lashing out at an enemy in its way and coming back, instead of hooking.
 */
public class HookStrike extends GameDynamicObject {

    // Out to the target and back
    private static final double DURATION = TimeUtil.secondsToUpdates(0.2);

    private GameObject target;

    public HookStrike(GameObject target) {
        super(target.getxPosInRoom(), target.getyPosInRoom(), 0, 0, 0, true, false);
        this.target = target;
        setGhost(true);
    }

    @Override
    public void update() {
        updateFrame();
        if (getFrame() >= DURATION) {
            this.destroy();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        // Drawn from where the character and the target are now, as both keep moving
        MathVector start = MyActivity.character.getPositionInScreen();
        MathVector toTarget = new MathVector(start, target.getPositionInScreen());
        if (toTarget.isNull()) {
            return;
        }
        double reached = 1 - Math.abs(2 * getFrame() / DURATION - 1);
        MathVector tip = toTarget.scaled(reached).applyTo(start);
        Chain.drawCable(canvas, (float) start.x, (float) start.y, (float) tip.x, (float) tip.y, Hook.RADIUS);
        MathVector direction = toTarget.getUnitVector();
        for (double distance = Hook.SEPARATION; distance < toTarget.magnitude() * reached; distance += Hook.SEPARATION) {
            MathVector link = direction.scaled(distance).applyTo(start);
            Chain.drawLink(canvas, (float) link.x, (float) link.y, Hook.RADIUS);
        }
        Chain.drawClaw(canvas, (float) tip.x, (float) tip.y, (float) direction.x, (float) direction.y, Hook.RADIUS);
    }

    @Override
    public int getWidth() {
        return Hook.RADIUS * 2;
    }

    @Override
    public int getHeight() {
        return Hook.RADIUS * 2;
    }
}
