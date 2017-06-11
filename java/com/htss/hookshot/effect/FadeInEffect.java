package com.htss.hookshot.effect;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;

/**
 * Created by Sergio on 30/05/2017.
 */
public class FadeInEffect extends GameEffect {

    public final static int END_FRAME = 500;
    private final Rect rectangle = new Rect(0, 0, MyActivity.screenWidth, MyActivity.screenHeight);
    private Paint paint;
    private int frame = 0;
    private int direction = 1;

    public FadeInEffect() {
        this.paint = new Paint();
    }

    @Override
    public void drawEffectAndUpdate(Canvas canvas) {
        paint.setColor(Color.BLACK);
        int alpha;
        if(frame <= 0) {
            alpha = 0;
        }
        else if(frame <= END_FRAME/2) {
            alpha = frame*255/(END_FRAME/2);
        } else {
            alpha = 255;
        }
        paint.setAlpha(alpha);
        canvas.drawRect(rectangle,paint);
        frame += 2*getDirection()*MyActivity.FRAME_RATE;
    }

    @Override
    public boolean isFinished() {
        return frame > END_FRAME || frame < 0;
    }

    @Override
    public void recycle() {

    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getFrame() {
        return frame;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }
}
