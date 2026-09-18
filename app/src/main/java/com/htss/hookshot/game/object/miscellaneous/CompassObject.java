package com.htss.hookshot.game.object.miscellaneous;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.interactables.HealthDrop;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.obstacles.WallButton;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

import java.util.LinkedList;

/**
 * Created by Sergio on 07/06/2017.
 */
public class CompassObject extends GameDynamicObject {

    // The arrows are needles like the compass icon's: gold for what's worth finding and red for enemies, lit on the
    // side facing the top left and outlined in a darker shade
    private static final int GOLD_LIGHT = Color.rgb(247, 218, 140), GOLD_DARK = Color.rgb(200, 140, 30), GOLD_EDGE = Color.rgb(74, 50, 8),
            RED_LIGHT = Color.rgb(255, 138, 122), RED_DARK = Color.rgb(192, 40, 28), RED_EDGE = Color.rgb(74, 8, 8);
    // Its timer, around the character, is brass like the compass
    private static final int TIMER_COLOR = Color.rgb(224, 168, 46);

    private GameDynamicObject parent;
    private LinkedList<GameObject> interests = new LinkedList<GameObject>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path half = new Path(), outline = new Path();
    private TimerObject timer;

    public CompassObject(GameDynamicObject parent, boolean addToGameObjectsList, boolean addToDynamicObjectsList) {
        super(parent.getxPosInRoom(), parent.getyPosInRoom(), 0, 0, 0, addToGameObjectsList, addToDynamicObjectsList);
        this.parent = parent;
        findInterests();
        this.timer = new TimerObject(parent, (int) (getWidth() / 1.75), TimeUtil.secondsToUpdates(16.667), TIMER_COLOR, false, addToDynamicObjectsList, new Execution() {
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
        timer.update();
    }

    @Override
    public void draw(Canvas canvas) {
        timer.draw(canvas);
        for (GameObject object : interests) {
            MathVector toInterest = parent.vectorTo(object);
            if (!toInterest.isNull()) {
                drawArrow(canvas, toInterest.getUnitVector(), object instanceof GameEnemy);
            }
        }
    }

    // A needle around the character, pointing the given way. Its two halves split along it, the one facing the top left
    // lighter
    private void drawArrow(Canvas canvas, MathVector direction, boolean enemy) {
        MathVector center = parent.getPositionInScreen(), side = direction.getNormal();
        float w = getWidth();
        // Just outside the timer's ring
        MathVector tip = direction.scaled(w * 0.98).applyTo(center), back = direction.scaled(w * 0.64).applyTo(center),
                middle = direction.scaled(w * 0.74).applyTo(center);
        MathVector left = side.scaled(w * 0.11).applyTo(middle), right = side.scaled(-w * 0.11).applyTo(middle);
        boolean leftLit = side.x + side.y < 0;
        int light = enemy ? RED_LIGHT : GOLD_LIGHT, dark = enemy ? RED_DARK : GOLD_DARK;
        paint.setStyle(Paint.Style.FILL);
        fillHalf(canvas, tip, left, back, leftLit ? light : dark);
        fillHalf(canvas, tip, right, back, leftLit ? dark : light);
        outline.reset();
        outline.moveTo((float) tip.x, (float) tip.y);
        outline.lineTo((float) left.x, (float) left.y);
        outline.lineTo((float) back.x, (float) back.y);
        outline.lineTo((float) right.x, (float) right.y);
        outline.close();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(Math.max(1.5f, w / 30));
        paint.setColor(enemy ? RED_EDGE : GOLD_EDGE);
        canvas.drawPath(outline, paint);
    }

    private void fillHalf(Canvas canvas, MathVector tip, MathVector side, MathVector back, int color) {
        half.reset();
        half.moveTo((float) tip.x, (float) tip.y);
        half.lineTo((float) side.x, (float) side.y);
        half.lineTo((float) back.x, (float) back.y);
        half.close();
        paint.setColor(color);
        canvas.drawPath(half, paint);
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
            } else if (object instanceof HealthDrop) {
                interests.add(object);
            } else if (object instanceof GameEnemy) {
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
