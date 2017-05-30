package com.htss.hookshot.game.object;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.game.object.shapes.RectShape;
import com.htss.hookshot.interfaces.Hookable;

/**
 * Created by Sergio on 27/08/2016.
 */
public class Ball extends GameDynamicObject implements Hookable {

    private float radius;

    public Ball(double xPos, double yPos, int mass, int collisionPriority, float radius) {
        super(xPos, yPos, mass, collisionPriority, 5);
        this.radius = radius;
        makeSureNotUnderground = true;
    }

    @Override
    public void update(){
        manageSlopesUpdate(getWidth() / 4);
        super.update();
    }

    private void manageSlopesUpdate(int margin) {
        int voidsLeft = 0;
        int voidsRight = 0;
        int y = (int) (getyPosInRoom() + getHeight()/2 + 3);
        for (int x = margin ; x > 0 ; x--){
            if (MyActivity.isInRoom(getxPosInRoom()-x,y)) {
                int pixelLeft = MyActivity.canvas.mapBitmap.getPixel((int) (getxPosInRoom() - x), y);
                if (Color.alpha(pixelLeft) == 0) {
                    voidsLeft++;
                }
                int pixelRight = MyActivity.canvas.mapBitmap.getPixel((int) (getxPosInRoom() + x), y);
                if (Color.alpha(pixelRight) == 0) {
                    voidsRight++;
                }
                if (voidsLeft != voidsRight) {
                    break;
                }
            }
        }
        p.x += voidsRight - voidsLeft;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.argb(255,220,200,200));
        canvas.drawCircle((float)getxPosInScreen(),(float)getyPosInScreen(),getRadius(),paint);
    }

    @Override
    public int getWidth() {
        return (int) (getRadius()*2);
    }

    @Override
    public int getHeight() {
        return (int) (getRadius()*2);
    }

    @Override
    public GameShape getBounds (){
        return new CircleShape(getxPosInRoom(),getyPosInRoom(), (int) getRadius());
    }

    @Override
    public GameShape getFutureBounds(){
        return new CircleShape(getFuturePositionInRoom().x,getFuturePositionInRoom ().y, (int) getRadius());
    }

    @Override
    public int getMargin(){
        return getWidth()/4;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}
