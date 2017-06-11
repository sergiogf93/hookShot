package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.obstacles.WallButton;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

import java.util.LinkedList;

/**
 * Created by Sergio on 07/06/2017.
 */
public class CompassObject extends GameDynamicObject {

    private GameDynamicObject parent;
    private LinkedList<GameObject> interests = new LinkedList<GameObject>();
    private Paint paint = new Paint();
    private TimerObject timer;

    public CompassObject(GameDynamicObject parent, boolean addToGameObjectsList, boolean addToDynamicObjectsList) {
        super(parent.getxPosInRoom(), parent.getyPosInRoom(), 0, 0, 0, addToGameObjectsList, addToDynamicObjectsList);
        this.parent = parent;
        findInterests();
        paint.setStrokeWidth(getWidth() / 500);
        this.timer = new TimerObject(parent, (int) (getWidth() / 1.75), TimeUtil.convertSecondToGameSecond(10), Color.YELLOW, addToGameObjectsList, addToDynamicObjectsList, new Execution() {
            @Override
            public double execute() {
                MyActivity.character.getCompass().destroy();
                MyActivity.character.setCompass(null);
                return 0;
            }
        });
        setGhost(true);
    }

    @Override
    public void update() {
        updateFrame();
    }

    @Override
    public void draw(Canvas canvas) {
        timer.draw(canvas);
        for (GameObject object : interests) {
            drawArrow(canvas, parent.vectorTo(object));
        }
    }

    private void drawArrow(Canvas canvas, MathVector vector) {
        Point[] points = new Point[4];
        double size = getWidth() * 0.8;
        points[0] = vector.rescaled(getWidth() / 2).applyTo(parent.getPositionInScreen()).toPoint();
        points[1] = vector.getNormal().rescaled(getWidth() / 10).applyTo(vector.rescaled(getWidth() / 1.75).applyTo(parent.getPositionInScreen())).toPoint();
        points[2] = vector.rescaled(size).applyTo(parent.getPositionInScreen()).toPoint();
        points[3] = vector.getNormal().rescaled(-1 * getWidth() / 10).applyTo(vector.rescaled(getWidth() / 1.75).applyTo(parent.getPositionInScreen())).toPoint();
        DrawUtil.drawVoidPolygon(points, canvas, Color.WHITE, getWidth() / 8, true);
        DrawUtil.drawPolygon(points,canvas, Color.YELLOW);
    }

    public void findInterests() {
        clearInterests();
        for (GameObject object : MyActivity.canvas.gameObjects) {
            if (object instanceof WallButton) {
                if (!((WallButton)object).isOn()) {
                    interests.add(object);
                }
            } else if (object instanceof GamePowerUp) {
                interests.add(object);
            }
        }
    }

    public void clearInterests() {
        interests.clear();
    }

    @Override
    public int getWidth() {
        return parent.getWidth() * 2;
    }

    @Override
    public int getHeight() {
        return parent.getHeight() * 2;
    }

    public void removeInterest(GameObject interest) {
        this.interests.remove(interest);
    }

    public TimerObject getTimer() {
        return timer;
    }
}
