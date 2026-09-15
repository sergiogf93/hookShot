package com.htss.hookshot.effect;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Execution;

/**
 * Created by Sergio on 30/05/2017.
 */
public class FadeEffect extends GameEffect {

    private Execution execMiddle, execEnd;
    private FadeInEffect fadeIn;

    public FadeEffect(int color, Execution exec) {
       this.execMiddle = exec;
       fadeIn = new FadeInEffect(color);
    }

    public FadeEffect(int color, Execution execMiddle, Execution execEnd) {
        this.execMiddle = execMiddle;
        this.execEnd = execEnd;
        fadeIn = new FadeInEffect(color);
    }

    @Override
    public void drawEffectAndUpdate(Canvas canvas) {
        if (!fadeIn.isFinished()) {
            fadeIn.drawEffectAndUpdate(canvas);
        } else {
            if (fadeIn.getDirection() == 1) {
                this.execMiddle.execute();
                fadeIn.setFrame(FadeInEffect.END_FRAME);
                fadeIn.setDirection(-1);
                canvas.drawRect(new Rect(0, 0, MyActivity.screenWidth, MyActivity.screenHeight), fadeIn.getPaint());
            }
        }
    }

    @Override
    public boolean isFinished() {
        return fadeIn.getFrame() < 0;
    }

    @Override
    public void recycle() {
        fadeIn = null;
        if (execEnd != null) {
            execEnd.execute();
        }
    }
}
