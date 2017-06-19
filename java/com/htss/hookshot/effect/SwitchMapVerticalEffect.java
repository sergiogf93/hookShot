package com.htss.hookshot.effect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 28/08/2016.
 */
public class SwitchMapVerticalEffect extends GameEffect {

    private static final double DURATION = TimeUtil.convertSecondToGameSecond(1);

    private Bitmap currentMapScreenBitmap, nextMapScreenBitmap;
    private int frame = 0, direction;
    private double characterVerticalDistanceToEdge;

    public SwitchMapVerticalEffect(Bitmap currentMapScreenBitmap, Bitmap nextMapScreenBitmap, int direction) {
        this.currentMapScreenBitmap = currentMapScreenBitmap;
        this.nextMapScreenBitmap = nextMapScreenBitmap;
        this.direction = direction;
        int factor = (direction > 0) ? 1 : 0;
        this.characterVerticalDistanceToEdge = MyActivity.character.getyPosInScreen() - factor*MyActivity.screenHeight;
    }

    @Override
    public void drawEffectAndUpdate(Canvas canvas){
        int yEdgePosition = (int) (-(MyActivity.screenHeight/DURATION)*frame + MyActivity.screenHeight);
        if (direction > 0){
            canvas.drawBitmap(currentMapScreenBitmap,0,yEdgePosition - MyActivity.screenHeight, GameBoard.paint);
            canvas.drawBitmap(nextMapScreenBitmap,0,yEdgePosition, GameBoard.paint);
        } else {
            yEdgePosition = MyActivity.screenHeight - yEdgePosition;
            canvas.drawBitmap(nextMapScreenBitmap,0,yEdgePosition - MyActivity.screenHeight, GameBoard.paint);
            canvas.drawBitmap(currentMapScreenBitmap,0,yEdgePosition, GameBoard.paint);
        }
        MyActivity.character.setyPosInScreen(yEdgePosition + characterVerticalDistanceToEdge);
        MyActivity.character.draw(canvas);

        frame += MyActivity.FRAME_RATE;
    }

    @Override
    public boolean isFinished (){
        return frame >= DURATION;
    }

    @Override
    public void recycle() {
        currentMapScreenBitmap.recycle();
        nextMapScreenBitmap.recycle();
    }
}
