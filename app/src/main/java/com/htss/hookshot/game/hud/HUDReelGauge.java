package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.util.DrawUtil;

/**
 * How much chain is out, on the edge of the screen while there's a chain. The track fills from the top with the chain
 * that's out, so the handle rises as the finger slides up and the chain comes in, and the mark above the track is
 * where sliding further zips the character up the chain.
 */
public class HUDReelGauge extends HUDElement {

    private static final int TRACK = Color.rgb(196, 208, 204), CHAIN = Color.rgb(247, 196, 58);
    private static final int TRACK_ALPHA = 90, ZIP_ALPHA = 130;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bar = new RectF();

    public HUDReelGauge() {
        super(0, 0, (int) (MyActivity.TILE_WIDTH * 0.3), (int) (MyActivity.TILE_WIDTH * 3));
    }

    @Override
    public void draw(Canvas canvas) {
        if (MyActivity.character == null || !MyActivity.character.isHooked()) {
            return;
        }
        Hook hook = MyActivity.character.getHook();
        float tile = MyActivity.TILE_WIDTH, width = tile * 0.09f;
        float x = getxCenter(), top = getyCenter() - getHeight() / 2f, bottom = getyCenter() + getHeight() / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(DrawUtil.withAlpha(TRACK, TRACK_ALPHA));
        round(canvas, x, top, bottom, width);

        // The chain that's out, against the chain this shot was thrown with: full at the bottom, all in at the top
        double share = Math.max(0, Math.min(1, hook.getChainLength() / hook.getChainWhenHooked()));
        float handle = (float) (top + (bottom - top) * share);
        paint.setColor(CHAIN);
        round(canvas, x, top, handle, width);
        canvas.drawCircle(x, handle, tile * 0.1f, paint);

        // Past the top of the track, the chain zips in
        paint.setColor(DrawUtil.withAlpha(CHAIN, hook.isFastReloading() ? 255 : ZIP_ALPHA));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width * 0.7f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        float zip = top - tile * 0.22f;
        canvas.drawLine(x - tile * 0.11f, zip, x + tile * 0.11f, zip, paint);
        canvas.drawLine(x - tile * 0.07f, zip + tile * 0.09f, x + tile * 0.07f, zip + tile * 0.09f, paint);
    }

    private void round(Canvas canvas, float x, float top, float bottom, float width) {
        bar.set(x - width / 2, top, x + width / 2, bottom);
        canvas.drawRoundRect(bar, width / 2, width / 2, paint);
    }

    @Override
    public boolean pressed(double x, double y) {
        return false;
    }
}
