package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 08/06/2017.
 */
public class TimerObject extends GameDynamicObject {

    private GameObject parent;
    private double duration;
    private int radius, color;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF oval = new RectF();
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

    // A faint track around the character, and a band on it for the time left, shrinking clockwise from the top.
    // Outlined and with a lighter line along it, like the power-up icons
    @Override
    public void draw(Canvas canvas) {
        float left = 1 - (float) Math.min(1, getFrame() / duration);
        float x = (float) parent.getxPosInScreen(), y = (float) parent.getyPosInScreen(), width = Math.max(2, radius * 0.13f);
        oval.set(x - radius, y - radius, x + radius, y + radius);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(width);
        paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawOval(oval, paint);
        if (left <= 0) {
            return;
        }
        float start = -90 + 360 * (1 - left), sweep = 360 * left;
        paint.setStrokeWidth(width + 3);
        paint.setColor(DrawUtil.blend(color, Color.BLACK, 0.65f));
        canvas.drawArc(oval, start, sweep, false, paint);
        paint.setStrokeWidth(width);
        paint.setColor(color);
        canvas.drawArc(oval, start, sweep, false, paint);
        paint.setStrokeWidth(width * 0.3f);
        paint.setColor(DrawUtil.blend(color, Color.WHITE, 0.55f));
        canvas.drawArc(oval, start, sweep, false, paint);
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
