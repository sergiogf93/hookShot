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
import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.map.Map;
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

    public static Bitmap mapBitmap;

    public static Vector<GameObject> gameObjects = new Vector<GameObject>();
    public static Vector<GameObject> debugObjects = new Vector<GameObject>();
    private final ArrayList<GameObject> objectsThisFrame = new ArrayList<GameObject>();
    // The updates due for the frames since the last draw, run before drawing it
    private int pendingUpdates = 0;

    public static String debugText = "";

    private final MapTiles mapTiles = new MapTiles();
    private final Atmosphere atmosphere = new Atmosphere();
    private final Paint digEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Only drawn over rock, keeping it opaque. No sizes here, as the tile size isn't known yet
        digEdgePaint.setStyle(Paint.Style.STROKE);
        digEdgePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
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

        if (MyActivity.roomSwitchEffect == null) {

            runUpdates(updates);
            canvas.save();
            canvas.translate(ScreenShake.getOffsetX(), ScreenShake.getOffsetY());

            if (MyActivity.currentMap != null) {
                drawGame(canvas);
            }

            drawObjects(canvas);

            Particles.draw(canvas);
            canvas.restore();

            if (MyActivity.currentMap != null && MyActivity.character != null) {
                atmosphere.drawLight(canvas, (float) MyActivity.character.getxPosInScreen(), (float) MyActivity.character.getyPosInScreen());
            }

            drawHudElements(canvas);

            drawNotifications(canvas);

            manageGameEffects(canvas);

        } else {
            manageRoomSwitchEffect(canvas);
        }

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

    private void manageRoomSwitchEffect(Canvas canvas) {
        MyActivity.roomSwitchEffect.drawEffectAndUpdate(canvas);
        if (MyActivity.roomSwitchEffect.isFinished()){
            myActivity.save();
            MyActivity.roomSwitchEffect.recycle();
            MyActivity.roomSwitchEffect = null;
            MyActivity.setHUDClickable();
            if (MyActivity.character.getCompass() != null) {
                gameObjects.add(MyActivity.character.getCompass());
                MyActivity.dynamicObjects.add(MyActivity.character.getCompass());
                gameObjects.add(MyActivity.character.getCompass().getTimer());
                MyActivity.dynamicObjects.add(MyActivity.character.getCompass().getTimer());
                MyActivity.character.getCompass().findInterests();
            }
            if (MyActivity.character.getInfiniteJumpsTimer() != null) {
                gameObjects.add(MyActivity.character.getInfiniteJumpsTimer());
                MyActivity.dynamicObjects.add(MyActivity.character.getInfiniteJumpsTimer());
            }
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
        // The whole map is too big to draw on the screen canvas, so it's drawn from tiles
        mapTiles.draw(canvas, (int) -dx, (int) -dy, MyActivity.screenWidth, MyActivity.screenHeight);
    }

    public void queueUpdates(int updates) {
        pendingUpdates += updates;
    }

    // Runs the given updates, several when frames were dropped so the game keeps its speed, and stops early if one
    // pauses the game or leaves the level. The game is drawn once they're all done, so everything is drawn where it
    // ended up, with the camera where the character left it
    private void runUpdates(int updates) {
        boolean updated = false;
        for (int i = 0; i < updates && !MyActivity.paused && MyActivity.roomSwitchEffect == null; i++) {
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

    public Bitmap getMapInScreen(){
        assertMapMargins();
        return Bitmap.createBitmap(mapBitmap,(int)-dx,(int)-dy,MyActivity.screenWidth,MyActivity.screenHeight);
    }

    public void generateMap(){
        if (mapBitmap != null) {
            mapBitmap.recycle();
        }
        mapBitmap = Bitmap.createBitmap(MyActivity.currentMap.getWidth(), MyActivity.currentMap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mapCanvas = new Canvas(mapBitmap);

        MyActivity.currentMap.draw(mapCanvas, CavePalette.forLevel(myActivity.level));
        mapTiles.setMap(mapBitmap);
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

    public static float clampDx(float dx) {
        return Math.min(0, Math.max(dx, -(MyActivity.currentMap.getWidth() - MyActivity.screenWidth)));
    }

    public static float clampDy(float dy) {
        return Math.min(0, Math.max(dy, -(MyActivity.currentMap.getHeight() - MyActivity.screenHeight)));
    }

    public void clearCircle(Bitmap bitmap, float cx, float cy, float radius) {
        Paint p = new Paint();
        p.setColor(getResources().getColor(android.R.color.transparent));
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        Canvas cnv = new Canvas(bitmap);
        cnv.drawCircle(cx, cy, radius, p);
        // Dug tunnels get the same dark edge as the rest of the cave
        digEdgePaint.setColor(CavePalette.forLevel(myActivity.level).outline);
        digEdgePaint.setStrokeWidth((float) (Map.SQUARE_SIZE / 4));
        cnv.drawCircle(cx, cy, radius, digEdgePaint);
        if (bitmap == mapBitmap) {
            mapTiles.changed(cx, cy, radius + digEdgePaint.getStrokeWidth());
        }
    }

}
