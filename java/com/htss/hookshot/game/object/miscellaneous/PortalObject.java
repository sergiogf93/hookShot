package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 06/06/2017.
 */
public class PortalObject extends GameDynamicObject {

    private static int STATE_REST = 0, STATE_MOVING = 1, STATE_FINISH = 2;

    private int state = STATE_REST;
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
            MyActivity.handleTouch = false;
            MyActivity.canvas.gameObjects.remove(MyActivity.character);
            MyActivity.character.setPositionInRoom(getTwinPortal().getxPortal(), getTwinPortal().getyPortal());
            state = STATE_MOVING;
        }
    }

    @Override
    public void update() {
        updateFrame();
        if (state == STATE_MOVING) {
            MyActivity.hideControls();
            MathVector currentD = new MathVector(MyActivity.canvas.dx, MyActivity.canvas.dy);
            MathVector objectiveD = new MathVector(getTwinPortal().getDx(), getTwinPortal().getDy());
            MathVector direction = new MathVector(currentD, objectiveD);
            if (direction.magnitude() > MyActivity.TILE_WIDTH) {
                direction = direction.getUnitVector().rescaled(MyActivity.TILE_WIDTH);
                MyActivity.canvas.dx += direction.x;
                MyActivity.canvas.dy += direction.y;
            } else {
                state = STATE_FINISH;
            }
        } else if (state == STATE_FINISH) {
            MyActivity.handleTouch = true;
            MyActivity.addControls();
            MyActivity.canvas.gameObjects.add(MyActivity.canvas.gameObjects.size(),MyActivity.character);
            MyActivity.character.setPositionInRoom(getTwinPortal().getxPortal(), getTwinPortal().getyPortal());
            MyActivity.character.update();
            MyActivity.canvas.dx = getTwinPortal().getDx();
            MyActivity.canvas.dy = getTwinPortal().getDy();
            state = STATE_REST;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int startAngle = getStartAngle();
        int radius = getRadius();
        DrawUtil.drawArc(canvas, getPaint(), (float) getxPosInScreen() - radius, (float) getyPosInScreen() - radius, (float) getxPosInScreen() + radius, (float) getyPosInScreen() + radius, Color.RED, startAngle, 180);
        DrawUtil.drawArc(canvas, getPaint(), (float) getxPosInScreen() - radius, (float) getyPosInScreen() - radius, (float) getxPosInScreen() + radius, (float) getyPosInScreen() + radius, Color.BLUE, startAngle + 180, 180);
        if (getTwinPortal() != null) {
            DrawUtil.drawCircle(canvas, getPaint(),(float) getxPosInScreen(), (float) getyPosInScreen(), radius,Color.BLACK, Paint.Style.FILL);
        }
    }

    public int getRadius() {
        if (getFrame() < TimeUtil.convertSecondToGameSecond(0.5)){
            return (int) (radius*getFrame()/TimeUtil.convertSecondToGameSecond(0.5));
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
