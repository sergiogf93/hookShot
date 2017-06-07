package com.htss.hookshot.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.htss.hookshot.game.GameBoard;
import com.htss.hookshot.game.MyActivity;

/**
 * Created by Sergio on 28/07/2016.
 */
public class DrawUtil {

    public static void drawPolygon (Point[] points, Canvas canvas, int color){
        Path path = new Path();

        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(points[0].x,points[0].y);
        for (int i = 1; i < points.length;i++){
            path.lineTo(points[i].x, points[i].y);
        }
        path.close();

        GameBoard.paint.setColor(color);
        canvas.drawPath(path, GameBoard.paint);
    }

    public static void drawVoidPolygon (Object[] points, Canvas canvas, int color, float width, boolean close){
        if (points.length > 0) {
            Path path = new Path();
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(width);

            path.moveTo(((Point) points[0]).x, ((Point) points[0]).y);
            for (int i = 1; i < points.length; i++) {
                double distToPrev = (((Point) points[i]).x - ((Point) points[i - 1]).x) * (((Point) points[i]).x - ((Point) points[i - 1]).x) + (((Point) points[i]).y - ((Point) points[i - 1]).y) * (((Point) points[i]).y - ((Point) points[i - 1]).y);
                if (distToPrev > MyActivity.TILE_WIDTH * MyActivity.TILE_WIDTH) {
                    Object[] p = new Point[points.length - i];
                    System.arraycopy(points, i, p, 0, points.length - i);
                    break;
                }
                path.lineTo(((Point) points[i]).x, ((Point) points[i]).y);
            }

            if (close) {
                path.close();
            }
            canvas.drawPath(path, paint);
        }
    }

    public static void drawCircle(Canvas canvas, Paint paint, float x, float y, float radius, int color, Paint.Style style) {
        int alpha = paint.getAlpha();
        paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setStyle(style);
        canvas.drawCircle(x, y, radius, paint);
    }

    public static void drawArc(Canvas canvas, Paint paint, float x, float y, float radius, int color, int start, int sweep) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(x - radius, y - radius, x + radius, y + radius, start, sweep, false, paint);
    }

    public static void drawArc(Canvas canvas, Paint paint, float left, float top, float right, float bottom, int color, int start, int sweep) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(left, top, right, bottom, start, sweep, false, paint);
    }

}
