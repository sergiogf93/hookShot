package com.htss.hookshot.game.object.interactables.powerups;

import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.interfaces.Interactable;

/**
 * Created by Sergio on 05/06/2017.
 */
public abstract class GamePowerUp extends GameDynamicObject implements Interactable {

    public static final int PORTAL = 0;

    private int type;
    private int width, height;
    private Paint paint = new Paint();

    public GamePowerUp(double xPos, double yPos, int width, int height, int type, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, 0, 0, 0, addToGameObjects, addToDynamicObjects);
        this.width = width;
        this.height = height;
        this.type = type;
    }

    public double getDy(int frame, float maxDy) {
        return maxDy*Math.sin(2*Math.PI*frame/(50* MyActivity.FRAME_RATE));
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void detect() {
        if (distanceTo(MyActivity.character) < MyActivity.TILE_WIDTH /2){
            MyActivity.canvas.gameObjects.remove(this);
            MyActivity.character.addPowerUp(this.type);
        }
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
