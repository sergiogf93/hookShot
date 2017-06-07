package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.util.DrawUtil;

/**
 * Created by Sergio on 05/06/2017.
 */
public class HUDPowerUpButton extends HUDElement implements Clickable{

    private boolean clickable = true, on = false;
    private int touchId = -1, touchIndex = -1;
    private GamePowerUp powerUp;
    private int quantity;

    public HUDPowerUpButton(int xCenter, int yCenter, int width, boolean clickable, GamePowerUp powerUp, int quantity) {
        super(xCenter, yCenter, width, width);
        this.powerUp = powerUp;
        this.quantity = quantity;
        this.clickable = clickable;
        getPaint().setStrokeWidth(getWidth()/10);
        getPaint().setTypeface(GameBoard.paint.getTypeface());
        getPaint().setTextSize(width / 5);
        setAlpha(HUDMenu.MENU_ALPHA);
    }

    private void manageExecution() {
        if (MyActivity.character.getCurrentPowerUp() != powerUp.getType()){
            usePowerUp();
        } else {
            unequipPowerUp();
        }
    }

    private void usePowerUp() {
        MyActivity.character.equipPowerUp(powerUp.getType());
        MyActivity.unpause();
    }

    private void unequipPowerUp() {
        if (powerUp.getType() == GamePowerUp.PORTAL) {
            MyActivity.character.equipPowerUp(-1);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        drawButton(canvas);
        drawPowerUp(canvas);
        if (quantity > 1) {
            drawQuantity(canvas);
        }
    }

    private void drawQuantity(Canvas canvas) {
        setColor(Color.WHITE);
        setAlpha(HUDMenu.MENU_ALPHA);
        canvas.drawText("x " + quantity, getxCenter() + getWidth() / 5, getyCenter() + getHeight() / 2, getPaint());
    }

    private void drawButton(Canvas canvas) {
        DrawUtil.drawCircle(canvas, getPaint(), getxCenter(), getyCenter(), getWidth() / 2, Color.rgb(0, 150, 150), Paint.Style.STROKE);
        DrawUtil.drawCircle(canvas, getPaint(), getxCenter(), getyCenter(), getWidth() / 2, getMainColor(), Paint.Style.FILL);
    }

    private void drawPowerUp(Canvas canvas) {
        powerUp.draw(canvas);
        if (!isOn() && MyActivity.character.getCurrentPowerUp() != powerUp.getType()) {
            powerUp.updateFrame();
        }
    }

    private int getMainColor() {
        if (isOn() || MyActivity.character.getCurrentPowerUp() == powerUp.getType()) {
            return Color.rgb(45, 85, 110);
        } else {
            return Color.CYAN;
        }
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    @Override
    public void press(double x, double y, int id, int index) {
        setTouchIndex(index);
        setTouchId(id);
        setOn(true);
    }

    @Override
    public void reset() {
        setTouchIndex(-1);
        setTouchId(-1);
        setOn(false);
        manageExecution();
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public int getTouchId() {
        return touchId;
    }

    public void setTouchId(int touchId) {
        this.touchId = touchId;
    }

    public int getTouchIndex() {
        return touchIndex;
    }

    public void setTouchIndex(int touchIndex) {
        this.touchIndex = touchIndex;
    }
}
