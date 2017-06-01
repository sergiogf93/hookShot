package com.htss.hookshot.game.object.obstacles;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;

import java.util.Vector;

/**
 * Created by Sergio on 03/09/2016.
 */
public class Door extends GameDynamicObject {

    private int width, height;
    private Vector<WallButton> buttons;

    public Door(double xPos, double yPos, int width, int height, Vector<WallButton> buttons) {
        super(xPos, yPos, 0, 0, 0);
        this.width = width;
        this.height = height;
        this.buttons = buttons;
    }

    @Override
    public void update(){
        super.update();
        boolean allOn = true;
        for (WallButton button : buttons){
            allOn = allOn && button.isOn();
        }
        if (allOn){
            MyActivity.canvas.gameObjects.remove(this);
            MyActivity.dynamicObjects.remove(this);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect rect = new Rect((int)getxPosInScreen()-getWidth()/2,(int)getyPosInScreen()-getHeight()/2,(int)getxPosInScreen()+getWidth()/2,(int)getyPosInScreen()+getHeight()/2);
        Paint paint = new Paint();
        paint.setColor(Color.GRAY);
        canvas.drawRect(rect,paint);
        for (int i = 0 ; i < buttons.size() ; i++){
            paint.setStyle(Paint.Style.FILL);
            WallButton button = buttons.get(i);
            if (button.isOn()){
                paint.setColor(Color.GREEN);
            } else {
                paint.setColor(Color.RED);
            }
            canvas.drawCircle((float)getxPosInScreen() - getWidth()/2 + button.getRadius() + i*4*button.getRadius()/3 , (float) getyPosInScreen(),button.getRadius()/3,paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(button.getRadius() / 50);
            paint.setColor(Color.argb(255, 20, 20, 20));
            canvas.drawCircle((float)getxPosInScreen() - getWidth()/2 + button.getRadius() + i*4*button.getRadius()/3 , (float) getyPosInScreen(),button.getRadius()/3,paint);
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

}
