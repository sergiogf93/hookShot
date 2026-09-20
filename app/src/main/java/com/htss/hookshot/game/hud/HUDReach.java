package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

/**
 * Where a shot found nothing to grip, crossed out for a moment, with a ring showing how far the chain reaches. So a
 * shot that falls short says so instead of doing nothing at all.
 */
public class HUDReach extends HUDElement {

    private static final int SHOWN = (int) TimeUtil.secondsToUpdates(0.6);
    private static final int EMPTY = Color.rgb(224, 232, 229);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DashPathEffect dashes;

    public HUDReach() {
        super(0, 0, 0, 0);
        dashes = new DashPathEffect(new float[]{MyActivity.TILE_WIDTH * 0.07f, MyActivity.TILE_WIDTH * 0.12f}, 0);
    }

    @Override
    public void draw(Canvas canvas) {
        MathVector missed = MyActivity.getMissedShot();
        if (missed == null || MyActivity.character == null) {
            return;
        }
        int since = MyActivity.character.getFrame() - MyActivity.getMissedShotFrame();
        if (since < 0 || since >= SHOWN) {
            return;
        }
        // Fading out, so it's gone before it's in the way
        int alpha = (int) (200 * (1 - since / (float) SHOWN));
        float tile = MyActivity.TILE_WIDTH, mark = tile * 0.16f;
        float x = (float) MyActivity.character.getxPosInScreen(), y = (float) MyActivity.character.getyPosInScreen();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(tile * 0.02f);
        paint.setPathEffect(dashes);
        paint.setColor(DrawUtil.withAlpha(CavePalette.forLevel(MyActivity.canvas.myActivity.level).rim, alpha / 3));
        canvas.drawCircle(x, y, (float) MyActivity.getHookReach(), paint);

        paint.setPathEffect(null);
        paint.setStrokeWidth(tile * 0.04f);
        paint.setColor(DrawUtil.withAlpha(EMPTY, alpha));
        canvas.drawLine((float) missed.x - mark, (float) missed.y - mark, (float) missed.x + mark, (float) missed.y + mark, paint);
        canvas.drawLine((float) missed.x + mark, (float) missed.y - mark, (float) missed.x - mark, (float) missed.y + mark, paint);
    }

    @Override
    public boolean pressed(double x, double y) {
        return false;
    }
}
