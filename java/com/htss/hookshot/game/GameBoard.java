package com.htss.hookshot.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.htss.hookshot.effect.GameEffect;
import com.htss.hookshot.game.hud.HUDElement;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.GameObject;
import com.htss.hookshot.interfaces.Interactable;
import com.htss.hookshot.util.StringUtil;

import java.util.Vector;

/**
 * Created by Sergio on 04/02/2016.
 */
public class GameBoard extends View{

    public static final int ARCADECLASSIC_FONT_KEY = 1, ARIAL_FONT_KEY = 2, JOYSTIX_MONOSPACE_FONT_KEY = 3;
    public static final int DEFAULT_FONT_SIZE = 48*MyActivity.TILE_WIDTH /100, SMALL_FONT_SIZE = 27*MyActivity.TILE_WIDTH /100;
    public static int fontSize;
    public Typeface arcadeClassicFont, joystickMonospace;

    public static float dx = 0, dy = 0;

    public static Paint paint = new Paint();

    public static Bitmap mapBitmap;

    public static int background = Color.argb(100, 30, 40, 100);

    public static Vector<GameObject> gameObjects = new Vector<GameObject>();
    public static Vector<GameObject> debugObjects = new Vector<GameObject>();

    public static String debugText = "";

    public GameBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
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

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(background);
        canvas.drawRect(0, 0, MyActivity.screenWidth, MyActivity.screenHeight, backgroundPaint);

        if (MyActivity.roomSwitchEffect == null) {

            if (MyActivity.character != null) {

                assertMapMargins();

                Bitmap mapInScreen = getMapInScreen();

                canvas.drawBitmap(mapInScreen, 0, 0, paint);

                for (int i = 0 ; i < gameObjects.size() ; i++) {
                    GameObject gameObject = gameObjects.get(i);
                    if (!MyActivity.paused) {
                        if (gameObject instanceof GameDynamicObject) {
                            ((GameDynamicObject) gameObject).update();
                        }
                        if (gameObject instanceof Interactable) {
                            ((Interactable) gameObject).detect();
                        }
                    }
                    gameObject.draw(canvas);
                }
                for (GameObject object : debugObjects) {
                    object.draw(canvas);
                }
                debugObjects.clear();

            }

            for (HUDElement hudElement : MyActivity.hudElements) {
                hudElement.draw(canvas);
            }

            for (int i = 0 ; i < MyActivity.gameEffects.size() ; i++){
                GameEffect effect = MyActivity.gameEffects.get(i);
                effect.drawEffectAndUpdate(canvas);
                if (effect.isFinished()){
                    MyActivity.gameEffects.remove(effect);
                }
            }

        } else {
            MyActivity.roomSwitchEffect.drawEffectAndUpdate(canvas);
            if (MyActivity.roomSwitchEffect.isFinished()){
                MyActivity.roomSwitchEffect = null;
                MyActivity.setHUDClickable();
                if (MyActivity.character.getCompass() != null) {
                    gameObjects.add(MyActivity.character.getCompass());
                    MyActivity.dynamicObjects.add(MyActivity.character.getCompass());
                    MyActivity.character.getCompass().findInterests();
                }
            }
        }

        drawInfo(canvas);

    }

    public Bitmap getMapInScreen(){
        return Bitmap.createBitmap(mapBitmap,(int)-dx,(int)-dy,MyActivity.screenWidth,MyActivity.screenHeight);
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

    public void generateMap(){
        if (mapBitmap != null) {
            mapBitmap.recycle();
        }
        mapBitmap = Bitmap.createBitmap(MyActivity.currentMap.getWidth(), MyActivity.currentMap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mapCanvas = new Canvas(mapBitmap);

        MyActivity.currentMap.draw(mapCanvas);
//        MyActivity.currentMap.drawNodes(mapCanvas);
        MyActivity.currentMap.drawOutlines(mapCanvas);

    }

    public Bitmap getBitmapById (int id){
        return BitmapFactory.decodeResource(getResources(), id);
    }

    public void assertMapMargins(){
        if (dx < - (MyActivity.currentMap.getWidth() - MyActivity.screenWidth)){
            dx = - (MyActivity.currentMap.getWidth() - MyActivity.screenWidth);
        }
        if (dx > 0) {
            dx = 0;
        }
        if (dy < - (MyActivity.currentMap.getHeight() - MyActivity.screenHeight)){
            dy = - (MyActivity.currentMap.getHeight() - MyActivity.screenHeight);
        }
        if (dy > 0) {
            dy = 0;
        }
    }

    public void clearCircle(Bitmap bitmap, float cx, float cy, float radius) {
        Paint p = new Paint();
        p.setColor(getResources().getColor(android.R.color.transparent));
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        Canvas cnv = new Canvas(bitmap);
        cnv.drawCircle(cx, cy, radius, p);
    }

}
