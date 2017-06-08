package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 08/06/2017.
 */
public class TimerObject extends GameDynamicObject {

    private GameObject parent;
    private double duration;
    private int radius, color;
    private Paint paint = new Paint();
    private Execution execution;

    public TimerObject(GameObject parent, int radius, double duration, int color, boolean addToGameObjectsList, boolean addToDynamicObjectsList, Execution execution) {
        super(parent.getxPosInRoom(), parent.getyPosInRoom(), 0, 0, 0, addToGameObjectsList, addToDynamicObjectsList);
        this.parent = parent;
        this.radius = radius;
        this.duration = duration;
        this.color = color;
        this.execution = execution;
        setGhost(true);
    }

    @Override
    public void update() {
        updateFrame();
        if (getFrame() > duration) {
            this.destroy();
            execution.execute();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int start = (int) GameMath.linealValue(0, 0, duration, 360, getFrame());
        DrawUtil.drawArc(canvas, paint, (float) parent.getxPosInScreen(), (float) parent.getyPosInScreen(), (float) (getWidth() / 2), color, start - 90, 360 - start);
    }

    @Override
    public int getWidth() {
        return radius * 2;
    }

    @Override
    public int getHeight() {
        return radius * 2;
    }
}
