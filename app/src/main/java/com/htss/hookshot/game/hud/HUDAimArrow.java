package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;

/**
 * The line from the character to where the hook will land, while the right stick aims the chain, with an arrowhead
 * near the character for when that's off the screen. Gold when the hook will grip, with a ring where; red when it
 * will hit an enemy; and grey when there's nothing within reach to hold on to.
 */
public class HUDAimArrow extends HUDElement {

    private static final int ENEMY = Color.rgb(255, 110, 90);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path head = new Path();
    private final DashPathEffect dashes;

    public HUDAimArrow() {
        super(0, 0, 0, 0);
        dashes = new DashPathEffect(new float[]{MyActivity.TILE_WIDTH * 0.1f, MyActivity.TILE_WIDTH * 0.09f}, 0);
    }

    @Override
    public void draw(Canvas canvas) {
        if (MyActivity.character == null || MyActivity.hookStick == null || !MyActivity.hookStick.isAiming()) {
            return;
        }
        MathVector direction = new MathVector(MyActivity.hookStick.getxJ(), MyActivity.hookStick.getyJ());
        MyActivity.Aim aim = MyActivity.aimAlong(direction.x, direction.y);
        if (aim == null) {
            return;
        }
        if (aim.enemy != null) {
            // Snapped to an enemy, which the stick only points near
            direction = new MathVector(MyActivity.character.getPositionInScreen(), aim.point);
            if (direction.isNull()) {
                return;
            }
        }
        direction.normalize();
        boolean lands = aim.grips || aim.enemy != null;
        int color = (aim.enemy != null) ? ENEMY : aim.grips ? UiStyle.GOLD : UiStyle.IDLE;
        float tile = MyActivity.TILE_WIDTH, dx = (float) direction.x, dy = (float) direction.y;
        float x = (float) MyActivity.character.getxPosInScreen(), y = (float) MyActivity.character.getyPosInScreen();
        float startX = x + dx * tile * 0.4f, startY = y + dy * tile * 0.4f;

        // All the way to where it lands, fainter than the arrowhead so it doesn't hide the cave
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(tile * 0.04f);
        paint.setPathEffect(dashes);
        paint.setColor(DrawUtil.withAlpha(color, lands ? 170 : 90));
        canvas.drawLine(startX, startY, (float) aim.point.x, (float) aim.point.y, paint);
        paint.setPathEffect(null);

        float tipX = x + dx * tile * 1.25f, tipY = y + dy * tile * 1.25f, size = tile * 0.2f;
        float sideX = -dy * size * 0.6f, sideY = dx * size * 0.6f;
        head.reset();
        head.moveTo(tipX + dx * size, tipY + dy * size);
        head.lineTo(tipX + sideX, tipY + sideY);
        head.lineTo(tipX - sideX, tipY - sideY);
        head.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(DrawUtil.withAlpha(color, 230));
        canvas.drawPath(head, paint);

        if (lands) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(tile * 0.045f);
            canvas.drawCircle((float) aim.point.x, (float) aim.point.y, tile * 0.16f, paint);
        }
    }

    @Override
    public boolean pressed(double x, double y) {
        return false;
    }
}
