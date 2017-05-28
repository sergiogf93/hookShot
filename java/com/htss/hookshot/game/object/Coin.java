package com.htss.hookshot.game.object;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Interactable;

/**
 * Created by Sergio on 06/09/2016.
 */
public class Coin extends GameDynamicObject implements Interactable{

    private static final float RADIUS = MyActivity.tileWidth/10;

    public Coin(double xPos, double yPos) {
        super(xPos, yPos, 0, 0, 0);
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        canvas.drawCircle((float)getxPosInScreen(), (float) getyPosInScreen(),RADIUS,paint);
    }

    @Override
    public int getWidth() {
        return (int) (RADIUS*2);
    }

    @Override
    public int getHeight() {
        return (int) (RADIUS*2);
    }

    @Override
    public void detect() {
        if (distanceTo(MyActivity.character) < MyActivity.tileWidth/2){
            MyActivity.canvas.gameObjects.remove(this);
        }
    }

}
