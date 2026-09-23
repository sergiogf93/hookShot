package com.htss.hookshot.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.htss.hookshot.effect.GameEffect;
import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.hud.HUDElement;
import com.htss.hookshot.game.hud.advices.HUDAdvice;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.map.Cave;
import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.map.World;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.StringUtil;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

/**
 * Created by Sergio on 04/02/2016.
 */
public class GameBoard extends View{

    public static final int ARCADECLASSIC_FONT_KEY = 1, ARIAL_FONT_KEY = 2, JOYSTIX_MONOSPACE_FONT_KEY = 3;
    public static int DEFAULT_FONT_SIZE = 48*MyActivity.TILE_WIDTH /100, SMALL_FONT_SIZE = 27*MyActivity.TILE_WIDTH /100;
    public static int fontSize;
    public Typeface arcadeClassicFont, joystickMonospace;
    public MyActivity myActivity;

    public static float dx = 0, dy = 0;

    public static Paint paint = new Paint();

    public static Vector<GameObject> gameObjects = new Vector<GameObject>();
    public static Vector<GameObject> debugObjects = new Vector<GameObject>();
    private final ArrayList<GameObject> objectsThisFrame = new ArrayList<GameObject>();
    // The updates due for the frames since the last draw, run before drawing it
    private int pendingUpdates = 0;

    public static String debugText = "";

    private final Atmosphere atmosphere = new Atmosphere();
    // Where the character last was, to tell how far it moved, and how much further than that the camera moves in an
    // update when it has catching up to do, in tiles
    private double lastCharacterX = Double.NaN, lastCharacterY;
    private static final double CAMERA_CATCH_UP = 0.12;

    public GameBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        MyActivity.setPendingScreenSize(w, h);
    }

    public void setFont(int c, float size){
        paint.setTextSize(size);
        fontSize = (int) size;
        switch (c){
            case ARCADECLASSIC_FONT_KEY:{
                paint.setTypeface(arcadeClassicFont);
                break;
            }
            case JOYSTIX_MONOSPACE_FONT_KEY:{
                paint.setTypeface(joystickMonospace);
                break;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        MyActivity.applyPendingResize();

        int updates = pendingUpdates;
        pendingUpdates = 0;

        atmosphere.drawBehind(canvas, myActivity.level);

        runUpdates(updates);
        canvas.save();
        canvas.translate(ScreenShake.getOffsetX(), ScreenShake.getOffsetY());

        if (MyActivity.currentMap != null) {
            drawGame(canvas);
        }

        drawObjects(canvas);

        if (MyActivity.currentMap != null && MyActivity.character != null) {
            MyActivity.character.drawBreath(canvas);
        }

        Particles.draw(canvas);
        canvas.restore();

        if (MyActivity.currentMap != null && MyActivity.character != null) {
            atmosphere.drawLight(canvas, (float) MyActivity.character.getxPosInScreen(), (float) MyActivity.character.getyPosInScreen());
        }

        drawHudElements(canvas);

        drawNotifications(canvas);

        manageGameEffects(canvas);

        manageAdvices();

        if (MyActivity.debugging) {
            drawInfo(canvas);
        }

    }

    private void drawNotifications(Canvas canvas) {
        for (int i = 0; i < MyActivity.notifications.size(); i++) {
            MyActivity.notifications.get(i).draw(canvas);
        }
    }

    private void manageAdvices() {
        for (int i = 0; i < MyActivity.advices.size(); i++) {
            MyActivity.advices.get(i).check();
        }
    }

    private void manageGameEffects(Canvas canvas) {
        for (int i = 0 ; i < MyActivity.gameEffects.size() ; i++){
            GameEffect effect = MyActivity.gameEffects.get(i);
            effect.drawEffectAndUpdate(canvas);
            if (effect.isFinished()){
                MyActivity.gameEffects.remove(effect);
                effect.recycle();
            }
        }
    }

    private void drawHudElements(Canvas canvas) {
        for (HUDElement hudElement : MyActivity.hudElements) {
            hudElement.draw(canvas);
        }
    }

    private void drawGame(Canvas canvas) {
        assertMapMargins();
        World.draw(canvas, (int) -dx, (int) -dy, MyActivity.screenWidth, MyActivity.screenHeight);
    }

    public void queueUpdates(int updates) {
        pendingUpdates += updates;
    }

    // Runs the given updates, several when frames were dropped so the game keeps its speed, and stops early if one
    // pauses the game. The game is drawn once they're all done, so everything is drawn where it
    // ended up, with the camera where the character left it
    private void runUpdates(int updates) {
        boolean updated = false;
        for (int i = 0; i < updates && !MyActivity.paused; i++) {
            updateGame();
            updated = true;
        }
        if (!updated) {
            // Nothing moved, but objects placed since, like while paused, are drawn
            objectsThisFrame.clear();
            objectsThisFrame.addAll(gameObjects);
        }
    }

    private void updateGame() {
        ScreenShake.update();
        Particles.update();
        // Before the character and its chain update, so both see whether the chain is being reeled in
        MyActivity.updateHold();
        // Objects leave the list while others update, like picked up coins or enemies killed by a bomb, which shifts
        // the rest and skipped the next one. So a copy is walked instead, passing over objects that left. Objects that
        // join start on the next update
        objectsThisFrame.clear();
        objectsThisFrame.addAll(gameObjects);
        for (GameObject gameObject : objectsThisFrame) {
            if (!gameObjects.contains(gameObject)) {
                continue;
            }
            if (gameObject instanceof GameDynamicObject) {
                ((GameDynamicObject) gameObject).update();
            }
            if (gameObject instanceof Interactable) {
                ((Interactable) gameObject).detect();
            }
        }
        // Once everything has moved: the camera goes after the character, and the world keeps up with where it is
        followCharacter();
        if (MyActivity.character != null && MyActivity.currentMap != null) {
            World.update(MyActivity.character.getxPosInRoom(), MyActivity.character.getyPosInRoom(), (int) -dx, (int) -dy, MyActivity.screenWidth, MyActivity.screenHeight);
        }
    }

    // The objects the last update went over that are still there, so objects that joined during it are drawn once
    // they've been updated
    private void drawObjects(Canvas canvas) {
        for (GameObject gameObject : objectsThisFrame) {
            if (gameObjects.contains(gameObject)) {
                gameObject.draw(canvas);
            }
        }
        for (GameObject object : debugObjects) {
            object.draw(canvas);
        }
        debugObjects.clear();
    }

    // A picture of the caves as they show on the screen
    public Bitmap getMapInScreen(){
        assertMapMargins();
        Bitmap picture = Bitmap.createBitmap(MyActivity.screenWidth, MyActivity.screenHeight, Bitmap.Config.ARGB_8888);
        World.draw(new Canvas(picture), (int) -dx, (int) -dy, MyActivity.screenWidth, MyActivity.screenHeight);
        return picture;
    }

    // The world starts over with the current map's cave. In a game, more caves follow it
    public void generateMap(){
        World.start(MyActivity.currentMap, myActivity.level, !MyActivity.playground);
        lastCharacterX = Double.NaN;
    }

    // Keeps the character within the margins round the middle of the screen, inside the box round the caves. It moves
    // as far as the character did, so it follows it exactly, and a little more when it has catching up to do, like
    // when a cave joins the world past an edge it was held at, so that it pans there rather than jumping
    private void followCharacter() {
        if (!MyActivity.cameraFollows || MyActivity.character == null || MyActivity.currentMap == null) {
            return;
        }
        double x = MyActivity.character.getxPosInRoom(), y = MyActivity.character.getyPosInRoom();
        double moved = Double.isNaN(lastCharacterX) ? Double.MAX_VALUE : Math.hypot(x - lastCharacterX, y - lastCharacterY);
        lastCharacterX = x;
        lastCharacterY = y;
        double screenX = x + dx, screenY = y + dy;
        float targetDx = dx, targetDy = dy;
        if (screenX > MyActivity.screenWidth - MyActivity.HORIZONTAL_MARGIN) {
            targetDx -= screenX - (MyActivity.screenWidth - MyActivity.HORIZONTAL_MARGIN);
        } else if (screenX < MyActivity.HORIZONTAL_MARGIN) {
            targetDx += MyActivity.HORIZONTAL_MARGIN - screenX;
        }
        if (screenY > MyActivity.screenHeight - MyActivity.VERTICAL_MARGIN) {
            targetDy -= screenY - (MyActivity.screenHeight - MyActivity.VERTICAL_MARGIN);
        } else if (screenY < MyActivity.VERTICAL_MARGIN) {
            targetDy += MyActivity.VERTICAL_MARGIN - screenY;
        }
        targetDx = clampDx(targetDx);
        targetDy = clampDy(targetDy);
        double away = Math.hypot(targetDx - dx, targetDy - dy), most = moved + MyActivity.TILE_WIDTH * CAMERA_CATCH_UP;
        if (away > most) {
            targetDx = (float) (dx + (targetDx - dx) * most / away);
            targetDy = (float) (dy + (targetDy - dy) * most / away);
        }
        dx = targetDx;
        dy = targetDy;
    }

    private void drawInfo(Canvas canvas) {
        int textSize = MyActivity.TILE_WIDTH /4;
        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setTextSize(textSize);
        if (MyActivity.character != null) {
            canvas.drawText(String.valueOf(MyActivity.character.isOnFloor()), MyActivity.TILE_WIDTH, MyActivity.TILE_WIDTH / 2, whitePaint);
        }
        int i=1;
        for (GameDynamicObject gameDynamicObject : MyActivity.dynamicObjects){
            String[] name = gameDynamicObject.getClass().getName().split("\\.");
            canvas.drawText(name[name.length-1] + ":  x = " + String.valueOf(gameDynamicObject.getxPosInRoom()) + " , y = " + String.valueOf(gameDynamicObject.getyPosInRoom()), MyActivity.TILE_WIDTH, MyActivity.TILE_WIDTH /2 + i*textSize, whitePaint);
            i++;
        }
        if (debugText != ""){
            canvas.drawText(debugText,MyActivity.screenWidth - MyActivity.TILE_WIDTH - StringUtil.sizeOfString(debugText, (int) whitePaint.getTextSize()), MyActivity.TILE_WIDTH,whitePaint);
        }
    }

    public Bitmap getBitmapById (int id){
        return BitmapFactory.decodeResource(getResources(), id);
    }

    public void assertMapMargins(){
        dx = clampDx(dx);
        dy = clampDy(dy);
    }

    // Inside the box round the caves
    public static float clampDx(float dx) {
        return Math.min(-World.getLeft(), Math.max(dx, -(World.getRight() - MyActivity.screenWidth)));
    }

    public static float clampDy(float dy) {
        return Math.min(-World.getTop(), Math.max(dy, -(World.getBottom() - MyActivity.screenHeight)));
    }

}
