package com.htss.hookshot.effect;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Execution;

/**
 * Created by Sergio on 30/05/2017.
 */
public class FadeEffect extends GameEffect {

    private int frame = 0;
    private Execution exec;
    private FadeInEffect fadeIn;

    public FadeEffect(Execution exec) {
       this.exec = exec;
        fadeIn = new FadeInEffect();
    }

    @Override
    public void drawEffectAndUpdate(Canvas canvas) {
        if (!fadeIn.isFinished()){
            fadeIn.drawEffectAndUpdate(canvas);
        } else {
            if (fadeIn.getDirection() == 1) {
                this.exec.execute();
                fadeIn.setFrame(FadeInEffect.END_FRAME);
                fadeIn.setDirection(-1);
                canvas.drawRect(new Rect(0, 0, MyActivity.screenWidth, MyActivity.screenHeight),fadeIn.getPaint());
            }
        }
    }

    @Override
    public boolean isFinished() {
        return fadeIn.getFrame() < 0;
    }
}
