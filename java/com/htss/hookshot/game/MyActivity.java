package com.htss.hookshot.game;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import com.htss.hookshot.R;
import com.htss.hookshot.effect.FadeEffect;
import com.htss.hookshot.effect.GameEffect;
import com.htss.hookshot.effect.SwitchMapEffect;
import com.htss.hookshot.executions.LaunchGame;
import com.htss.hookshot.game.hud.HUDButton;
import com.htss.hookshot.game.hud.HUDElement;
import com.htss.hookshot.game.hud.HUDText;
import com.htss.hookshot.game.hud.Joystick;
import com.htss.hookshot.game.object.debug.Circle;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.interfaces.Hookable;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.math.MathVector;

import java.util.Vector;


public class MyActivity extends Activity {

    public static final int FRAME_RATE = 20, TILE_WIDTH = 100, FILL_PERCENT = 52; //52
    public static int HORIZONTAL_MARGIN, VERTICAL_MARGIN;
    private static final int BUTTON_A_BOTTOM_PADDING = 70,
            BUTTON_A_RIGHT_PADDING = 50,
            BUTTON_B_BOTTOM_PADDING = 50,
            BUTTON_B_RIGHT_PADDING = 250;

    public static GameBoard canvas;
    public static GameEffect roomSwitchEffect;
    private Handler handler = new Handler();
    public static int screenHeight, screenWidth, mapXTiles = 110, mapYTiles = 80, level = 0; //Default 110 80, for screen size 30 20
    public static int frame = 0;
    public static MainCharacter character;
    public static Joystick joystick;
    public static HUDButton reloadButton, extendButton, buttonA, buttonB;

    public static Vector<HUDElement> hudElements = new Vector<HUDElement>();
    public static Vector<GameDynamicObject> dynamicObjects = new Vector<GameDynamicObject>();
    public static Vector<GameEffect> gameEffects = new Vector<GameEffect>();
    public static Vector<GameEnemy> enemies = new Vector<GameEnemy>();

    public static Map currentMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_my);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHeight = displaymetrics.heightPixels;
        screenWidth = displaymetrics.widthPixels;
        HORIZONTAL_MARGIN = screenWidth / 2 - TILE_WIDTH * 2;
        VERTICAL_MARGIN = screenHeight / 2;

        Bitmap joystickBase = BitmapFactory.decodeResource(getResources(), R.drawable.joystick_base);
        Bitmap joystickTop = BitmapFactory.decodeResource(getResources(), R.drawable.joystick_top);
        joystick = new Joystick(TILE_WIDTH +joystickBase.getWidth()/2,
                screenHeight- TILE_WIDTH /2-joystickBase.getHeight()/2,
                joystickBase,joystickTop);

        Bitmap buttonSprite = BitmapFactory.decodeResource(getResources(), R.drawable.button_a);
        Bitmap buttonPSprite = BitmapFactory.decodeResource(getResources(), R.drawable.button_a_pressed);
        buttonA = new HUDButton(screenWidth-buttonSprite.getWidth()/2-BUTTON_A_RIGHT_PADDING,
                screenHeight-buttonSprite.getHeight()/2-BUTTON_A_BOTTOM_PADDING,buttonSprite,buttonPSprite,true,new Execution() {
            @Override
            public double execute() {
//                                                if (MyActivity.character.isOnFloor()) {
                if (true) {
                    MathVector jumpForce = new MathVector(0, -20 * MyActivity.character.getMass());
                    MyActivity.character.addP(jumpForce);
                }

                return 0;
            }
        });

        buttonSprite = BitmapFactory.decodeResource(getResources(), R.drawable.button_b);
        buttonPSprite = BitmapFactory.decodeResource(getResources(), R.drawable.button_b_pressed);
        buttonB = new HUDButton(screenWidth-buttonSprite.getWidth()/2-BUTTON_B_RIGHT_PADDING,
                screenHeight-buttonSprite.getHeight()/2-BUTTON_B_BOTTOM_PADDING,buttonSprite,buttonPSprite,true,new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.getHook() != null){
                    MyActivity.character.removeHook();
                } else {
                    MyActivity.character.getHurt(1);
                }
                return 0;
            }
        });

        canvas = (GameBoard) findViewById(R.id.the_canvas);
        canvas.arcadeClassicFont = Typeface.createFromAsset(getAssets(), "fonts/arcadeclassic.ttf");
        canvas.joystickMonospace = Typeface.createFromAsset(getAssets(),"fonts/joystix_monospace.ttf");
        canvas.setFont(GameBoard.ARCADECLASSIC_FONT_KEY, GameBoard.DEFAULT_FONT_SIZE);

        LinearLayout myLayout = (LinearLayout) findViewById(R.id.layout);
        myLayout.setOnTouchListener(
                new LinearLayout.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent ev){
                        handleTouch(ev);
                        return true;
                    }
                }
        );

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                initGfx();
            }
        }, 1000);

    }

    private void initGfx() {
        handler.removeCallbacks(frameUpdate);

        final FadeEffect fadeEffect = new FadeEffect(new LaunchGame());

        HUDText newGame = new HUDText(screenWidth/2,screenHeight/2 - canvas.fontSize * 3, true, "NEW GAME", TILE_WIDTH *8/10, null, new Execution() {
            @Override
            public double execute() {
                gameEffects.add(fadeEffect);
                return 0;
            }
        });
        hudElements.add(newGame);

        canvas.invalidate();

        handler.postDelayed(frameUpdate, FRAME_RATE);
    }

    private Runnable frameUpdate = new Runnable() {
        @Override
        synchronized public void run() {
            handler.removeCallbacks(frameUpdate);

            frame += FRAME_RATE;

            canvas.invalidate();
            handler.postDelayed(frameUpdate, FRAME_RATE);
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_reset) {
            initGfx();
            return true;
        }
        if (id == R.id.action_regenerate) {
            canvas.generateMap();
            initGfx();
            return true;
        }
        if (id == R.id.action_debug) {
            debug();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void debug() {
        System.out.println();
    }

    public static boolean isInRoom(double x, double y){
        return x >= 0 && x < MyActivity.currentMap.getWidth() && y >= 0 && y < MyActivity.currentMap.getHeight();
    }

    public static boolean isInScreen(double x, double y){
        return x >= 0 && x < screenWidth && y >= 0 && y < screenHeight;
    }

    public static boolean isInScreen(double x, double y, double radius){
        return x -  radius >= 0 && x + radius < screenWidth && y - radius >= 0 && y + radius < screenHeight;
    }

    public static boolean isInScreen(MathVector r){
        return isInScreen(r.x,r.y);
    }

    public static boolean isInScreen(MathVector r, double radius){
        return isInScreen(r.x,r.y,radius);
    }

    private void handleTouch(MotionEvent ev) {
        if (roomSwitchEffect == null) {
            int pointerCount = ev.getPointerCount();
            boolean nothingPressed = true;
            double xHook = 0;
            double yHook = 0;
            for (int i = 0; i < pointerCount; i++) {
                int id = ev.getPointerId(i);
                double xDown = ev.getX(i);
                double yDown = ev.getY(i);
                int action = ev.getActionMasked();
                int actionIndex = ev.getActionIndex();

                switch (action) {
                    case MotionEvent.ACTION_UP: {
                        nothingPressed = false;
                        manageUpTouch(false,id,actionIndex);
                        break;
                    }
                    case MotionEvent.ACTION_POINTER_UP: {
                        nothingPressed = false;
                        manageUpTouch(true,id,actionIndex);
                        break;
                    }
                    case MotionEvent.ACTION_DOWN: {
                        nothingPressed = manageDownTouch(xDown,yDown,id, ev.findPointerIndex(id));
                        if (nothingPressed) {
                            xHook = xDown;
                            yHook = yDown;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_POINTER_DOWN: {
                        nothingPressed = manageDownTouch(xDown,yDown,id, ev.findPointerIndex(id));
                        if (nothingPressed) {
                            xHook = xDown;
                            yHook = yDown;
                        }
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        nothingPressed = false;
                        for (int k = 0; k < hudElements.size(); k++) {
                            HUDElement element = hudElements.get(k);
                            if (element instanceof Clickable) {
                                if (element instanceof Joystick) {
                                    if (((Joystick) element).isOn() && ((Joystick) element).getTouchId() == id) {
                                        ((Joystick) element).moveJoystick(xDown, yDown);
                                    }
                                } else {
                                    if (!((Clickable) element).isOn()) {
                                        if (element.pressed(xDown, yDown)) {
                                            ((Clickable) element).press(xDown, yDown, id, ev.findPointerIndex(id));
                                        }
                                    } else if (((Clickable) element).getTouchId() == id) {
                                        if (!element.pressed(xDown, yDown)) {
                                            ((Clickable) element).reset();
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
            if (character != null) {
                if (nothingPressed) {
                    boolean hookableFound = false;
                    for (GameDynamicObject dynamicObject : dynamicObjects) {
                        if (dynamicObject instanceof Hookable) {
                            if (dynamicObject.pressed(xHook, yHook)) {
                                character.shootHook(dynamicObject.getxPosInScreen(), dynamicObject.getyPosInScreen());
                                hookableFound = true;
                                break;
                            }
                        }
                    }
                    if (!hookableFound) {
                        MathVector objective = checkIfSomethingInTheWay(xHook, yHook);
                        if (isInScreen(objective.x, objective.y)) {
                            MathVector objectiveInRoom = objective.screenToRoom();
                            int pixel = canvas.mapBitmap.getPixel((int) objectiveInRoom.x, (int) objectiveInRoom.y);
                            if (Color.alpha(pixel) == 255) {
                                if (character.isHooked() && character.getHook().getHookedPoint().distanceTo(objectiveInRoom) < TILE_WIDTH) {
                                    character.getHook().setFastReloading(true);
                                } else {
                                    character.shootHook(objective.x, objective.y);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void manageUpTouch(boolean isPointer, int id, int actionIndex) {
        Vector joined = new Vector();
        joined.addAll(hudElements);
        joined.addAll(enemies);
        for (int k = 0; k < joined.size(); k++) {
            Object element = joined.get(k);
            if (element instanceof Clickable) {
                Clickable clickable = (Clickable) element;
                if (isPointer) {
                    if (clickable.isOn() && clickable.getTouchId() == id && clickable.getTouchIndex() == actionIndex) {
                        ((Clickable) element).reset();
                    }
                } else {
                    if (clickable.isOn() && clickable.getTouchId() == id) {
                        clickable.reset();
                    }
                }
            }
        }
    }

    private boolean manageDownTouch(double xDown, double yDown, int id, int pointerIndex) {
        boolean nothingPressed = true;
        Vector joined = new Vector();
        joined.addAll(hudElements);
        joined.addAll(enemies);
        for (int k = 0; k < joined.size(); k++) {
            Object element = joined.get(k);
            if (element instanceof Clickable) {
                Clickable clickable = (Clickable) element;
                if (clickable.isClickable()) {
                    if (!clickable.isOn()) {
                        if (clickable.pressed(xDown,yDown)) {
                            clickable.press(xDown, yDown, id, pointerIndex);
                            nothingPressed = false;
                        }
                    }
                }
            }
        }
        return nothingPressed;
    }

    private MathVector checkIfSomethingInTheWay(double xDown, double yDown) {
        MathVector vector = new MathVector(character.getPositionInScreen(),new MathVector(xDown,yDown));
        int i = 0;
        while (true){
            i++;
            vector.normalize();
            vector.scale(i);
            MathVector point = vector.applyTo(character.getPositionInRoom());
            if (isInRoom(point.x,point.y)) {
                int pixel = canvas.mapBitmap.getPixel((int) point.x, (int) point.y);
                if (Color.alpha(pixel) == 255) {
                    return (new MathVector(point.x, point.y)).roomToScreen();
                }
                canvas.debugObjects.add(new Circle(point.x, point.y, 0, 0, 1, Color.YELLOW));
            } else {
                break;
            }
        }
        return new MathVector(xDown, yDown);
    }

    public static void setHUDUnclickable(){
        joystick.reset();
        joystick.setClickable(false);
        buttonA.setClickable(false);
        buttonB.setClickable(false);
        if (extendButton != null)
            extendButton.setClickable(false);
        if (reloadButton != null)
            reloadButton.setClickable(false);
    }

    public static void setHUDClickable(){
        joystick.setClickable(true);
        buttonA.setClickable(true);
        buttonB.setClickable(true);
        if (extendButton != null)
            extendButton.setClickable(true);
        if (reloadButton != null)
            reloadButton.setClickable(true);
    }

    public static void resetObjectsLists(){
        canvas.gameObjects.clear();
        dynamicObjects.clear();
        canvas.gameObjects.add(character);
        dynamicObjects.add(character);
    }

    public static void switchMap(int direction) {
        if (roomSwitchEffect == null) {
            setHUDUnclickable();

            resetObjectsLists();

            Bitmap currentMapInScreen = canvas.getMapInScreen();
            currentMap.extend(direction);
            canvas.generateMap();
            if (direction > 0) {
                canvas.dy = 0;
            } else if (direction < 0){
                canvas.dy = MyActivity.screenHeight - MyActivity.currentMap.getHeight();
            }
            Bitmap nextMapInScreen = canvas.getMapInScreen();
            roomSwitchEffect = new SwitchMapEffect(currentMapInScreen, nextMapInScreen, direction);
        }
    }
}
