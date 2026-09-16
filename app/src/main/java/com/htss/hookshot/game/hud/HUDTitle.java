package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import com.htss.hookshot.game.MyActivity;

/**
 * The game's name on the main menu, fading from white into the cave's accent, with a dark outline and a shadow.
 */
public class HUDTitle extends HUDElement {

    private static final String TITLE = "HOOKSHOT";

    private Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG), edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient gradient;
    private int size, gradientAccent;

    public HUDTitle(int xCenter, int yCenter, int size) {
        super(xCenter, yCenter, 0, size);
        this.size = size;
        fillPaint.setTypeface(MyActivity.canvas.arcadeClassicFont);
        fillPaint.setTextSize(size);
        edgePaint.setTypeface(MyActivity.canvas.arcadeClassicFont);
        edgePaint.setTextSize(size);
        edgePaint.setStrokeWidth(size / 9f);
        edgePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void draw(Canvas canvas) {
        float x = getxCenter() - fillPaint.measureText(TITLE) / 2;
        float baseline = getyCenter() + size * 0.35f;
        int accent = UiStyle.getAccent();
        if (gradient == null || gradientAccent != accent) {
            gradientAccent = accent;
            gradient = new LinearGradient(0, baseline - size * 0.7f, 0, baseline, Color.WHITE, accent, Shader.TileMode.CLAMP);
            fillPaint.setShader(gradient);
        }
        float shadow = size / 12f;
        edgePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        edgePaint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawText(TITLE, x + shadow, baseline + shadow, edgePaint);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setColor(Color.rgb(12, 8, 10));
        canvas.drawText(TITLE, x, baseline, edgePaint);
        canvas.drawText(TITLE, x, baseline, fillPaint);
    }
}
