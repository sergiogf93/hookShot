package com.htss.hookshot.game.object.collectables;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 07/09/2016.
 */
public class CoinPowder extends GameDynamicObject implements Interactable {

    private final static float RADIUS = MyActivity.TILE_WIDTH /20;
    private int dir;

    public CoinPowder(double xPos, double yPos, MathVector initP) {
        super(xPos, yPos, 0, 0, MyActivity.TILE_WIDTH /20);
        this.setGhost(true);
        this.p = initP;
        MathVector toChar = new MathVector(getPositionInRoom(),MyActivity.character.getPositionInRoom());
        this.dir = (int) Math.signum(getP().signedAngleDeg(toChar))*10;
    }

    @Override
    public void update(){
        super.update();
        MathVector toChar = new MathVector(getPositionInRoom(),MyActivity.character.getPositionInRoom());
        setP(toChar.scaled(getMaxVelocity()));
//        double angle = getP().signedAngleDeg(toChar);
//        if (Math.abs(angle) > 15) {
//            this.p.rotateDeg(dir);
////            this.p.scale(1.05);
//        } else {
//            this.p.scale(1.1);
//            this.dir = (int) Math.signum(angle)*10;
//        }
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        canvas.drawCircle((float)getxPosInScreen(), (float) getyPosInScreen(),getRadius(),paint);
    }

    @Override
    public int getWidth() {
        return (int) (2*getRadius());
    }

    @Override
    public int getHeight() {
        return getWidth();
    }

    @Override
    public void detect() {
        if (distanceTo(MyActivity.character) < getRadius()*3){
            destroy();
        }
    }

    public static float getRadius() {
        return RADIUS;
    }
}
