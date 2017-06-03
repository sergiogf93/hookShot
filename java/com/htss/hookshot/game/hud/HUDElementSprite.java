package com.htss.hookshot.game.hud;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by Sergio on 03/08/2016.
 */
public abstract class HUDElementSprite extends HUDElement {

    protected Bitmap sprite;

    public HUDElementSprite(int xPos, int yPos, Bitmap sprite) {
        super(xPos, yPos);
        this.sprite = sprite;
    }

    @Override
    public void draw(Canvas canvas){
        canvas.drawBitmap(getSprite(),getxCenter()-getWidth()/2,getyCenter()-getHeight()/2, null);
    }

    @Override
    public int getWidth(){
        return getSprite().getWidth();
    }

    @Override
    public int getHeight(){
        return getSprite().getHeight();
    }

    public Bitmap getSprite() {
        return sprite;
    }

    public void setSprite(Bitmap sprite) {
        this.sprite = sprite;
    }

}
