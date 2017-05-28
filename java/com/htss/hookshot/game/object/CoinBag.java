package com.htss.hookshot.game.object;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.math.MathVector;

import java.util.Random;

/**
 * Created by Sergio on 07/09/2016.
 */
public class CoinBag extends GameDynamicObject implements Interactable {

    private final static float RADIUS = MyActivity.tileWidth/4;
    private final static int COINS = 10;

    public CoinBag(double xPos, double yPos) {
        super(xPos, yPos, 0, 0, 0);
    }

//    @Override
//    public void update(){
//        super.update();
//        double dx = (getRadius()/3)*Math.sin(2*Math.PI*getFrame()/40);
//        setxPosInRoom((int) (getxPosInRoom()+dx));
//    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        canvas.drawCircle((float)getxPosInScreen(), (float) getyPosInScreen(),getRadius(),paint);
    }

    @Override
    public int getWidth() {
        return (int) (getRadius()*2);
    }

    @Override
    public int getHeight() {
        return getWidth();
    }

    @Override
    public void detect() {
        if (distanceTo(MyActivity.character) < getRadius()*1.3){
            addCoinPowder();
            destroy();
        }
    }

    private void addCoinPowder() {
        for (int i = 0 ; i < COINS ; i++){
            Random random = new Random();
            MathVector initP = new MathVector(random.nextFloat()*2 - 1,random.nextFloat()*2 - 1);
            CoinPowder coinPowder = new CoinPowder(getxPosInRoom(),getyPosInRoom(),initP.scaled(MyActivity.tileWidth/2));
            MyActivity.canvas.gameObjects.add(coinPowder);
        }
    }

    public static float getRadius() {
        return RADIUS;
    }
}
