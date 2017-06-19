package com.htss.hookshot.effect;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 28/08/2016.
 */
public class SwitchMapHorizontalEffect extends GameEffect {

    private static final double DURATION = TimeUtil.convertSecondToGameSecond(1);

    private Bitmap currentMapScreenBitmap, nextMapScreenBitmap;
    private int frame = 0, direction;
    private double characterHorizontalDistanceToEdge;

    public SwitchMapHorizontalEffect(Bitmap currentMapScreenBitmap, Bitmap nextMapScreenBitmap, int direction) {
        this.currentMapScreenBitmap = currentMapScreenBitmap;
        this.nextMapScreenBitmap = nextMapScreenBitmap;
        this.direction = direction;
        int factor = (direction > 0) ? 1 : 0;
        this.characterHorizontalDistanceToEdge = MyActivity.character.getxPosInScreen() - factor*MyActivity.screenWidth;
    }

    @Override
    public void drawEffectAndUpdate(Canvas canvas){
        int xEdgePosition = (int) (-(MyActivity.screenWidth/DURATION)*frame + MyActivity.screenWidth);
        if (direction > 0) {
            canvas.drawBitmap(currentMapScreenBitmap, xEdgePosition - MyActivity.screenWidth, 0, GameBoard.paint);
            canvas.drawBitmap(nextMapScreenBitmap, xEdgePosition, 0, GameBoard.paint);
        } else {
            xEdgePosition = MyActivity.screenWidth - xEdgePosition;
            canvas.drawBitmap(nextMapScreenBitmap, xEdgePosition - MyActivity.screenWidth, 0, GameBoard.paint);
            canvas.drawBitmap(currentMapScreenBitmap, xEdgePosition, 0, GameBoard.paint);
        }
        MyActivity.character.setxPosInScreen(xEdgePosition + characterHorizontalDistanceToEdge);
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
