package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;

import com.htss.hookshot.game.MyActivity;

/**
 * Created by Sergio on 13/06/2017.
 */
public class HUDNotification extends HUDElement {

    public static final int NOTIFICATIONS_SIZE = MyActivity.TILE_WIDTH / 4;
    public static final int MARGIN = MyActivity.TILE_WIDTH / 4;

    private String text;
    private int frame = 0;
    private double duration = 0;

    public HUDNotification(String text, double duration) {
        super(0, 0, 0, NOTIFICATIONS_SIZE);
        this.text = text;
        getPaint().setTypeface(MyActivity.canvas.joystickMonospace);
        getPaint().setTextSize(NOTIFICATIONS_SIZE);
        getPaint().setColor(Color.WHITE);
        setWidth((int) getPaint().measureText(text));
        this.duration = duration;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawText(text, MARGIN, MARGIN + getHeight() * (1 + MyActivity.notifications.indexOf(this)), getPaint());
        frame += MyActivity.FRAME_RATE;
        if (frame >= duration) {
            MyActivity.notifications.remove(this);
        }
    }
}
