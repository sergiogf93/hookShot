package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 08/06/2017.
 */
public class JumpEffect extends GameDynamicObject {

    private static final double DURATION = TimeUtil.convertSecondToGameSecond(0.1);

    private int maxWidth, maxHeight;
    private Paint paint = new Paint();

    public JumpEffect(double xPos, double yPos, int maxWidth, int maxHeight, boolean addToGameObjectsList, boolean addToDynamicObjectsList) {
        super(xPos, yPos, 0, 0, 0, addToGameObjectsList, addToDynamicObjectsList);
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        paint.setStrokeWidth(maxWidth / 20);
        setGhost(true);
    }

    @Override
    public void update() {
        updateFrame();
        if (getFrame() > DURATION) {
            this.destroy();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        paint.setAlpha((int) GameMath.linealValue(0,255,DURATION,50,getFrame()));
        int w = getWidth();
        int h = getHeight();
        DrawUtil.drawArc(canvas, paint, (float) getxPosInScreen() - w / 2, (float) getyPosInScreen() - h / 2, (float) getxPosInScreen() + w / 2, (float) getyPosInScreen() + h / 2, Color.CYAN, -45, 270);
    }

    @Override
    public int getWidth() {
        return (int) GameMath.linealValue(0, 0, DURATION, maxWidth, getFrame());
    }

    @Override
    public int getHeight() {
        return (int) GameMath.linealValue(0, 0, DURATION, maxHeight, getFrame());
    }
}
