package com.htss.hookshot.game;

import android.app.Activity;
import android.graphics.Bitmap;
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
import com.htss.hookshot.game.hud.HUDCircleButton;
import com.htss.hookshot.game.hud.HUDElement;
import com.htss.hookshot.game.hud.HUDMenu;
import com.htss.hookshot.game.hud.HUDPauseButton;
import com.htss.hookshot.game.hud.HUDPowerUpButton;
import com.htss.hookshot.game.hud.HUDText;
import com.htss.hookshot.game.hud.Joystick;
import com.htss.hookshot.game.object.debug.Circle;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;
import com.htss.hookshot.game.object.miscellaneous.PortalObject;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.interfaces.Hookable;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.math.MathVector;

import java.util.LinkedList;
import java.util.Vector;


public class MyActivity extends Activity {

    public static int FILL_PERCENT = 52; //Default 52 for screen size 30
    public static int mapXTiles = 110, mapYTiles = 80; //Default 110 80, for screen size 30 20
//    public static int FILL_PERCENT = 30;
//    public static int mapXTiles = 30, mapYTiles = 20;

    public static final int FRAME_RATE = 10;
    public static int TILE_WIDTH, HORIZONTAL_MARGIN, VERTICAL_MARGIN;
    private static int BUTTON_A_BOTTOM_PADDING,BUTTON_A_RIGHT_PADDING,BUTTON_B_BOTTOM_PADDING,BUTTON_B_RIGHT_PADDING;

    public static GameBoard canvas;
    public static GameEffect roomSwitchEffect;
    private Handler handler = new Handler();
    public static int screenHeight, screenWidth, level = 0; //Default 110 80, for screen size 30 20
    public static int frame = 0;
    public static MainCharacter character;
    public static Joystick joystick;
    public static HUDCircleButton reloadButton, extendButton, buttonB, buttonA;
    public static HUDPauseButton pauseButton;
    public static HUDMenu menu;
    public static LinkedList<HUDPowerUpButton> powerUpButtons = new LinkedList<HUDPowerUpButton>();
    public static boolean paused = false, handleTouch = true;

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
        screenHeight = displaymetrics.heightPixels; //720
        screenWidth = displaymetrics.widthPixels; //1280
        TILE_WIDTH = 100 * screenHeight / 720;
        HORIZONTAL_MARGIN = screenWidth / 2 - TILE_WIDTH * 2;
        VERTICAL_MARGIN = screenHeight / 2;
        BUTTON_A_BOTTOM_PADDING = 70 * TILE_WIDTH / 100;
        BUTTON_A_RIGHT_PADDING = 50 * TILE_WIDTH / 100;
        BUTTON_B_BOTTOM_PADDING = 50 * TILE_WIDTH / 100;
        BUTTON_B_RIGHT_PADDING = 250 * TILE_WIDTH / 100;

        joystick = new Joystick(2 * TILE_WIDTH, screenHeight - TILE_WIDTH / 2 - TILE_WIDTH, TILE_WIDTH * 2, TILE_WIDTH * 2);

        int buttonRadius = (int) (TILE_WIDTH*0.75);
        buttonA = new HUDCircleButton(screenWidth - buttonRadius - BUTTON_A_RIGHT_PADDING,
                screenHeight - buttonRadius - BUTTON_A_BOTTOM_PADDING, buttonRadius, "A", true, new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.isOnFloor()) {
                    MyActivity.character.jump();
                } else if (MyActivity.character.getCurrentPowerUp() == GamePowerUp.INFINITE_JUMPS) {
                    MyActivity.character.usePowerUp();
                }

                return 0;
            }
        }
        );

        buttonB = new HUDCircleButton(screenWidth - buttonRadius - BUTTON_B_RIGHT_PADDING,
                screenHeight - buttonRadius - BUTTON_B_BOTTOM_PADDING, buttonRadius, "B", true, new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.getHook() != null) {
                    MyActivity.character.removeHook();
                } else {
                    boolean portalUsed = false;
                    if (MyActivity.character.getPortals().size() > 0) {
                        for (PortalObject portal : MyActivity.character.getPortals()) {
                            if (MyActivity.character.distanceTo(portal) < portal.getRadius()) {
                                portal.use();
                                portalUsed = true;
                                break;
                            }
                        }
                    }
                    if (!portalUsed) {
                        if (MyActivity.character.getCurrentPowerUp() >= 0) {
                            MyActivity.character.usePowerUp();
                        }
                    }
                }
                return 0;
            }
        }
        );

        pauseButton = new HUDPauseButton(screenWidth / 2, screenHeight - TILE_WIDTH / 2, TILE_WIDTH, (int) (TILE_WIDTH * 0.5));

        int nMenuButton = 2;
        int menuButtonHeight = TILE_WIDTH;
        int menuButtonSeparation = TILE_WIDTH / 5;
        int menuWidth = 5*TILE_WIDTH;
        int menuHeight = menuButtonHeight*nMenuButton + (nMenuButton+1)*menuButtonSeparation;
        menu = new HUDMenu(screenWidth / 2, screenHeight / 2, menuWidth, menuHeight, menuButtonHeight, menuButtonSeparation);

        canvas = (GameBoard) findViewById(R.id.the_canvas);
        canvas.DEFAULT_FONT_SIZE = 48*MyActivity.TILE_WIDTH /100;
        canvas.SMALL_FONT_SIZE = 27*MyActivity.TILE_WIDTH /100;
        canvas.arcadeClassicFont = Typeface.createFromAsset(getAssets(), "fonts/arcadeclassic.ttf");
        canvas.joystickMonospace = Typeface.createFromAsset(getAssets(),"fonts/joystix_monospace.ttf");
        canvas.setFont(GameBoard.ARCADECLASSIC_FONT_KEY, GameBoard.DEFAULT_FONT_SIZE);

        LinearLayout myLayout = (LinearLayout) findViewById(R.id.layout);
        myLayout.setOnTouchListener(
                new LinearLayout.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent ev){
                        if (handleTouch) {
                            handleTouch(ev);
                        }
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
        }, FRAME_RATE);

    }

    private void initGfx() {
        handler.removeCallbacks(frameUpdate);

        final FadeEffect fadeEffect = new FadeEffect(new LaunchGame());

        HUDText newGame = new HUDText(screenWidth / 2, screenHeight / 2 - canvas.fontSize * 3, true, "NEW GAME", TILE_WIDTH * 8 / 10, new Execution() {
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
                                    if (joystick.isOn() && joystick.getTouchId() == id && joystick.getTouchIndex() == ev.findPointerIndex(id)) {
                                        joystick.moveJoystick(xDown, yDown);
                                    }
                                } else {
                                    Clickable clickable = (Clickable) element;
                                    if (clickable.isClickable()) {
                                        if (!clickable.isOn()) {
                                            if (clickable.pressed(xDown, yDown)) {
//                                                clickable.press(xDown, yDown, id, ev.findPointerIndex(id));
                                            }
                                        } else if (clickable.getTouchId() == id) {
                                            if (!clickable.pressed(xDown, yDown)) {
//                                                clickable.reset();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
            if (!paused) {
                if (character != null) {
                    if (nothingPressed) {
                        manageHooking(xHook, yHook);
                    }
                }
            }
        }
    }

    private void manageHooking(double xHook, double yHook) {
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

    private void manageUpTouch(boolean isPointer, int id, int actionIndex) {
        Vector joined = new Vector();
        joined.addAll(hudElements);
        if (!paused) {
            joined.addAll(enemies);
        }
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
        if (!paused) {
            joined.addAll(enemies);
        }
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
                canvas.debugObjects.add(new Circle(point.x, point.y, 0, 0, 1, Color.YELLOW, false));
            } else {
                break;
            }
        }
        return new MathVector(xDown, yDown);
    }

    public static void hideControls() {
        joystick.reset();
        buttonA.reset();
        buttonB.reset();
        MyActivity.hudElements.remove(MyActivity.joystick);
        MyActivity.hudElements.remove(MyActivity.buttonA);
        MyActivity.hudElements.remove(MyActivity.buttonB);
    }

    public static void addControls() {
        MyActivity.hudElements.add(MyActivity.joystick);
        MyActivity.hudElements.add(MyActivity.buttonA);
        MyActivity.hudElements.add(MyActivity.buttonB);
    }

    public static void setHUDUnclickable(){
        joystick.reset();
        buttonA.reset();
        buttonB.reset();
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
        enemies.clear();
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

    public static void pause() {
        MyActivity.paused = true;
        hideControls();

        hudElements.add(menu);
        menu.addMenuButtons();
        powerUpButtons = new LinkedList<HUDPowerUpButton>();
        for (Integer i : character.getPowerUps().keySet()) {
            if (character.getPowerUps().get(i) > 0) {
                switch (i) {
                    case GamePowerUp.PORTAL:
                        PortalPowerUp portalPowerUp = new PortalPowerUp(screenWidth / 6 - canvas.dx, screenHeight / 4 - canvas.dy, TILE_WIDTH / 2, false, false);
                        powerUpButtons.add(new HUDPowerUpButton(screenWidth / 6, screenHeight / 4, TILE_WIDTH * 2, true, portalPowerUp, character.getPowerUps().get(i)));
                        break;
                    case GamePowerUp.COMPASS:
                        CompassPowerUp compassPowerUp = new CompassPowerUp(screenWidth / 6 - canvas.dx, 3 * screenHeight / 4 - canvas.dy, (int) (TILE_WIDTH * 0.8), false, false);
                        powerUpButtons.add(new HUDPowerUpButton(screenWidth / 6, 3 * screenHeight / 4, TILE_WIDTH * 2, MyActivity.character.getCompass() == null, compassPowerUp, character.getPowerUps().get(i)));
                        break;
                    case GamePowerUp.BOMB:
                        BombPowerUp bombPowerUp = new BombPowerUp( 5 * screenWidth / 6 - canvas.dx, screenHeight / 4 - canvas.dy, (int) (TILE_WIDTH * 0.8), false, false);
                        powerUpButtons.add(new HUDPowerUpButton( 5 * screenWidth / 6, screenHeight / 4, TILE_WIDTH * 2, true, bombPowerUp, character.getPowerUps().get(i)));
                        break;
                    case GamePowerUp.INFINITE_JUMPS:
                        InfiniteJumpsPowerUp infiniteJumpsPowerUp = new InfiniteJumpsPowerUp( 5 * screenWidth / 6 - canvas.dx, 3 * screenHeight / 4 - canvas.dy, (int) (TILE_WIDTH * 0.9), (int) (TILE_WIDTH * 0.8), false, false);
                        powerUpButtons.add(new HUDPowerUpButton( 5 * screenWidth / 6, 3 * screenHeight / 4, TILE_WIDTH * 2, character.getInfiniteJumpsTimer() == null, infiniteJumpsPowerUp, character.getPowerUps().get(i)));
                        break;
                }
            }
        }
        MyActivity.hudElements.addAll(powerUpButtons);
    }

    public static void unpause() {
        MyActivity.paused = false;
        addControls();
        menu.removeButtons();
        hudElements.remove(menu);
        hudElements.removeAll(powerUpButtons);
    }
}
