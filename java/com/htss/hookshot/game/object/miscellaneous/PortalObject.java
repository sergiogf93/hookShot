package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 06/06/2017.
 */
public class PortalObject extends GameDynamicObject {

    private int radius;
    private Paint paint = new Paint();
    private PortalObject twinPortal;
    private float dx, dy;
    private double xPortal, yPortal;

    public PortalObject(double xPos, double yPos, double xPortal, double yPortal, float dx, float dy,  int radius) {
        super(xPos, yPos, 0, 0, 0, true, true);
        this.radius = radius;
        this.xPortal = xPortal;
        this.yPortal = yPortal;
        this.dx = dx;
        this.dy = dy;
        setGhost(true);
        getPaint().setStrokeWidth(radius/10);
    }

    public void use() {
        if (getTwinPortal() != null) {
            MyActivity.character.setPositionInRoom(getTwinPortal().getxPortal(), getTwinPortal().getyPortal());
            MyActivity.canvas.dx = getTwinPortal().getDx();
            MyActivity.canvas.dy = getTwinPortal().getDy();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int startAngle = getStartAngle();
        drawArc(canvas, getRadius(), Color.RED, startAngle, 180);
        drawArc(canvas, getRadius(), Color.BLUE, startAngle + 180, 180);
        if (getTwinPortal() != null) {
            drawBody(canvas);
        }
    }

    private void drawBody(Canvas canvas) {
        getPaint().setColor(Color.BLACK);
        getPaint().setStyle(Paint.Style.FILL);
        canvas.drawCircle((float) getxPosInScreen(), (float) getyPosInScreen(), getRadius(), getPaint());
    }

    private void drawArc(Canvas canvas, float radius, int color, int start, int sweep) {
        getPaint().setColor(color);
        getPaint().setStyle(Paint.Style.STROKE);
        canvas.drawArc((float) getxPosInScreen() - radius, (float) getyPosInScreen() - radius, (float) getxPosInScreen() + radius, (float) getyPosInScreen() + radius, start, sweep, false, getPaint());
    }

    public int getRadius() {
        if (getFrame() < TimeUtil.convertSecondToGameSecond(1)){
            return (int) (radius*getFrame()/TimeUtil.convertSecondToGameSecond(1));
        } else {
            return radius;
        }
    }

    public int getStartAngle() {
        return (int) (180 * Math.sin(2 * Math.PI * getFrame() / TimeUtil.convertSecondToGameSecond(1)) + 25);
    }
    @Override
    public int getWidth() {
        return radius*2;
    }

    @Override
    public int getHeight() {
        return radius*2;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public Paint getPaint() {
        return paint;
    }

    public PortalObject getTwinPortal() {
        return twinPortal;
    }

    public void setTwinPortal(PortalObject twinPortal) {
        this.twinPortal = twinPortal;
    }

    public float getDx() {
        return dx;
    }

    public float getDy() {
        return dy;
    }

    public double getxPortal() {
        return xPortal;
    }

    public double getyPortal() {
        return yPortal;
    }
}
