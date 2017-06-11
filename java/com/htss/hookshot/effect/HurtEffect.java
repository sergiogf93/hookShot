package com.htss.hookshot.effect;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 02/09/2016.
 */
public class HurtEffect extends GameEffect {

    private static int DURATION = (int) TimeUtil.convertSecondToGameSecond(0.5);
    private int frame = 0;

    @Override
    public void drawEffectAndUpdate(Canvas canvas) {
        Rect rect = new Rect(0,0, MyActivity.screenWidth,MyActivity.screenHeight);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        int alpha = -200*frame/DURATION + 200;
        paint.setAlpha(alpha);
        canvas.drawRect(rect,paint);
        frame += MyActivity.FRAME_RATE;
    }

    @Override
    public boolean isFinished() {
        return frame > DURATION;
    }

    @Override
    public void recycle() {

    }
}
