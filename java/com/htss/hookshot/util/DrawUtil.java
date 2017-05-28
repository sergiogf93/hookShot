package com.htss.hookshot.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.htss.hookshot.game.GameBoard;

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

    public static void drawVoidPolygon (Object[] points, Canvas canvas, int color, float width){
        if (points.length > 0) {
            Path path = new Path();
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(width);

            path.moveTo(((Point) points[0]).x, ((Point) points[0]).y);
            for (int i = 1; i < points.length; i++) {
                double distToPrev = (((Point) points[i]).x - ((Point) points[i - 1]).x) * (((Point) points[i]).x - ((Point) points[i - 1]).x) + (((Point) points[i]).y - ((Point) points[i - 1]).y) * (((Point) points[i]).y - ((Point) points[i - 1]).y);
                if (distToPrev > 100 * 100) {
                    Object[] p = new Point[points.length - i];
                    System.arraycopy(points, i, p, 0, points.length - i);
                    break;
//                canvas.drawPath(path, paint);
//                drawVoidPolygon(p,canvas,color);
                }
                path.lineTo(((Point) points[i]).x, ((Point) points[i]).y);
            }
//        path.close();

            canvas.drawPath(path, paint);
        }
    }

}
