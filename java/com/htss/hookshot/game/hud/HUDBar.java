package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Execution;

/**
 * Created by Sergio on 31/05/2017.
 */
public class HUDBar extends HUDElement {

    private int color, alpha = 255;
    private int alphaDirection = 10;
    private Execution getFillPercent;

    public HUDBar(int xCenter, int yCenter, int width, int height, int color, Execution getFillPercent) {
        super(xCenter, yCenter, width, height);
        this.color = color;
        this.getFillPercent = getFillPercent;
    }

    @Override
    public void draw(Canvas canvas) {
        if (getAlpha() > 0) {
            if (getAlpha() >= 255) {
                alphaDirection = -10;
                setAlpha(255);
            }
            this.alpha += alphaDirection;
            getPaint().setColor(Color.BLACK);
            this.setAlpha(getAlpha());
            Rect backRect = new Rect(getxCenter() - getWidth()/2, getyCenter() - getHeight()/2, getxCenter() + getWidth()/2, getyCenter() + getHeight()/2);
            canvas.drawRect(backRect, getPaint());
            double fillPercent = this.getFillPercent.execute();
            Rect fillRect = new Rect(getxCenter() - getWidth() / 2, getyCenter() - getHeight() / 2, (int) (getxCenter() - getWidth() / 2 + getWidth() * fillPercent), getyCenter() + getHeight() / 2);
            getPaint().setColor(this.color);
            this.setAlpha(getAlpha());
            canvas.drawRect(fillRect, getPaint());
        } else {
            alphaDirection = 10;
            setAlpha(0);
        }
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
