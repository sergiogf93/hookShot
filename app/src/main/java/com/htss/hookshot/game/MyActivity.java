package com.htss.hookshot.game;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Choreographer;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.htss.hookshot.R;
import com.htss.hookshot.effect.FadeEffect;
import com.htss.hookshot.effect.GameEffect;
import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.effect.SwitchMapHorizontalEffect;
import com.htss.hookshot.effect.SwitchMapVerticalEffect;
import com.htss.hookshot.executions.MainMenu;
import com.htss.hookshot.game.hud.HUDNotification;
import com.htss.hookshot.game.hud.advices.HUDAdvice;
import com.htss.hookshot.game.hud.HUDCircleButton;
import com.htss.hookshot.game.hud.HUDElement;
import com.htss.hookshot.game.hud.HUDMenu;
import com.htss.hookshot.game.hud.HUDPauseButton;
import com.htss.hookshot.game.hud.HUDPowerUpButton;
import com.htss.hookshot.game.hud.HUDStatus;
import com.htss.hookshot.game.hud.Joystick;
import com.htss.hookshot.game.object.debug.Circle;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.enemies.ClickableEnemy;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.game.object.hook.HookStrike;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;
import com.htss.hookshot.game.object.miscellaneous.PortalObject;
import com.htss.hookshot.game.object.obstacles.Door;
import com.htss.hookshot.interfaces.Clickable;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.interfaces.Hookable;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.FramePacer;

import java.util.LinkedList;
import java.util.Vector;


public class MyActivity extends Activity {

    public static int FILL_PERCENT = 52; //Default 52 for screen size 30
    public static int mapXTiles = 110, mapYTiles = 80; //Default 110 80, for screen size 30 20
//    public static int FILL_PERCENT = 20;
//    public static int mapXTiles = 30, mapYTiles = 20;

    // The game logic runs once per redraw and was tuned on 60 Hz phones, so redraws are capped to this
    // rate. Otherwise it plays faster on 90 and 120 Hz screens
    public static final int UPDATES_PER_SECOND = 60;
    // Most time between two taps, or two presses of a button, for them to count as a double tap. Measured with
    // SystemClock.uptimeMillis, which doesn't jump when the phone's clock is changed
    public static final long DOUBLE_TAP_MILLIS = 500;
    // Least time a finger stays on the screen to reel the chain in. A quicker one is a tap
    private static final long HOLD_MILLIS = 200;
    public static int TILE_WIDTH, HORIZONTAL_MARGIN, VERTICAL_MARGIN;
    // About the biggest tile a phone gets, 7.2 tiles over a 430 dp short side
    private static final int MAX_TILE_WIDTH_DP = 60;
    private static int BUTTON_A_BOTTOM_PADDING,BUTTON_A_RIGHT_PADDING,BUTTON_B_BOTTOM_PADDING,BUTTON_B_RIGHT_PADDING;

    public static GameBoard canvas;
    public static GameEffect roomSwitchEffect;
    private final FramePacer framePacer = new FramePacer(UPDATES_PER_SECOND);
    public static int screenHeight, screenWidth; //Default 110 80, for screen size 30 20
    private static int pendingScreenWidth, pendingScreenHeight;
    public static MainCharacter character;
    public static Joystick joystick;
    public static HUDCircleButton extendButton, buttonB, buttonA;
    public static HUDPauseButton pauseButton;
    public static HUDMenu menu;
    public static HUDStatus status;
    public static LinkedList<HUDPowerUpButton> powerUpButtons = new LinkedList<HUDPowerUpButton>();
    public static boolean paused = false, handleTouch = true, debugging = false;
    public static long lastTap = 0;
    // The finger held on the screen, outside the controls, and when it went down
    private static int holdTouchId = -1;
    private static long holdDownTime = 0;
    public Long seed;
    public int level = 0;
    public String entranceString = "";
    public int portals = 0, bombs = 0, compass = 0, jumps = 0, explosionsUsed = 0, coins = 0;
    public double health = MainCharacter.MAX_HEALTH;
    public int portalsAdvice = 0, compassAdvice = 0, bombAdvice = 0, jumpsAdvice = 0;

    public static LinkedList<HUDElement> hudElements = new LinkedList<HUDElement>();
    public static LinkedList<GameDynamicObject> dynamicObjects = new LinkedList<GameDynamicObject>();
    public static LinkedList<GameEffect> gameEffects = new LinkedList<GameEffect>();
    public static LinkedList<GameEnemy> enemies = new LinkedList<GameEnemy>();
    public static LinkedList<HUDAdvice> advices = new LinkedList<HUDAdvice>();
    public static LinkedList<HUDNotification> notifications = new LinkedList<HUDNotification>();

    public static Map currentMap;

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void load() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        seed = preferences.getLong("Seed", -1);
        level = preferences.getInt("Level", 0);
        entranceString = preferences.getString("Entrance", mapXTiles / 2 + " " + mapYTiles / 2);
        portals = preferences.getInt("Portals", 0);
        compass = preferences.getInt("Compass", 0);
        bombs = preferences.getInt("Bombs", 0);
        jumps = preferences.getInt("Jumps", 0);
        explosionsUsed = preferences.getInt("ExplosionsUsed", 0);
        coins = preferences.getInt("Coins", 0);
        health = preferences.getFloat("Health", MainCharacter.MAX_HEALTH);
        portalsAdvice = preferences.getInt("PortalsAdvice", 0);
        compassAdvice = preferences.getInt("CompassAdvice", 0);
        bombAdvice = preferences.getInt("BombAdvice", 0);
        jumpsAdvice = preferences.getInt("JumpsAdvice", 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The game state lives in static fields, which outlive the activity when Android keeps the process
        roomSwitchEffect = null;
        handleTouch = true;
        gameEffects.clear();
        notifications.clear();
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Ask screens that can change refresh rate to match the update rate, so no frame is repeated
        WindowManager.LayoutParams windowAttributes = getWindow().getAttributes();
        windowAttributes.preferredRefreshRate = UPDATES_PER_SECOND;
        getWindow().setAttributes(windowAttributes);
        setContentView(R.layout.activity_my);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHeight = displaymetrics.heightPixels; //720
        screenWidth = displaymetrics.widthPixels; //1280
        pendingScreenWidth = screenWidth;
        pendingScreenHeight = screenHeight;
        if (TILE_WIDTH == 0) {
            // Many sizes are fixed from TILE_WIDTH when their classes load, so it can't change while the process
            // lives, even when the activity comes back on the other screen of a foldable
            TILE_WIDTH = getTileWidth(getWindowManager().getDefaultDisplay());
        }
        BUTTON_A_BOTTOM_PADDING = 70 * TILE_WIDTH / 100;
        BUTTON_A_RIGHT_PADDING = 50 * TILE_WIDTH / 100;
        BUTTON_B_BOTTOM_PADDING = 50 * TILE_WIDTH / 100;
        BUTTON_B_RIGHT_PADDING = 250 * TILE_WIDTH / 100;

        // The controls and menu are placed by layoutForScreen, which runs again when the screen size changes
        joystick = new Joystick(0, 0, TILE_WIDTH * 2, TILE_WIDTH * 2);

        int buttonRadius = (int) (TILE_WIDTH*0.75);
        buttonA = new HUDCircleButton(0, 0, buttonRadius, "A", true, new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.isOnFloor()) {
                    MyActivity.character.jump( -1 * MyActivity.TILE_WIDTH);
                } else if (MyActivity.character.getCurrentPowerUp() == GamePowerUp.INFINITE_JUMPS) {
                    MyActivity.character.usePowerUp();
                }

                return 0;
            }
        }
        );

        buttonB = new HUDCircleButton(0, 0, buttonRadius, "B", true, new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.getHook() != null) {
                    if (MyActivity.character.getHook().isFastReloading()){
                        if (!MyActivity.character.inContactWithMap(MyActivity.character.getMargin())) {
                            MyActivity.character.removeHook();
                        }
                    } else {
                        MyActivity.character.removeHook();
                    }
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

        pauseButton = new HUDPauseButton(0, 0, TILE_WIDTH, (int) (TILE_WIDTH * 0.5));

        canvas = (GameBoard) findViewById(R.id.the_canvas);
        canvas.myActivity = this;
        canvas.DEFAULT_FONT_SIZE = 48*MyActivity.TILE_WIDTH /100;
        canvas.SMALL_FONT_SIZE = 27*MyActivity.TILE_WIDTH /100;
        canvas.arcadeClassicFont = Typeface.createFromAsset(getAssets(), "fonts/arcadeclassic.ttf");
        canvas.joystickMonospace = Typeface.createFromAsset(getAssets(),"fonts/joystix_monospace.ttf");
        canvas.setFont(GameBoard.ARCADECLASSIC_FONT_KEY, GameBoard.DEFAULT_FONT_SIZE);

        int nMenuButton = 2;
        int menuButtonHeight = TILE_WIDTH;
        int menuButtonSeparation = TILE_WIDTH / 5;
        int menuWidth = 5*TILE_WIDTH;
        int menuHeight = menuButtonHeight*nMenuButton + (nMenuButton+1)*menuButtonSeparation;
        menu = new HUDMenu(0, 0, menuWidth, menuHeight, menuButtonHeight, menuButtonSeparation);
        status = new HUDStatus();
        layoutForScreen();

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
        }, 10);

    }

    private static int getTileWidth(Display display) {
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        // Phones fit 7.2 tiles across their short side, as the game was designed on 720 px tall screens. Bigger
        // screens, like tablets and unfolded foldables, show more of the cave instead of zooming in
        int shortSide = Math.min(metrics.widthPixels, metrics.heightPixels);
        return (int) Math.min(100 * shortSide / 720, MAX_TILE_WIDTH_DP * metrics.density);
    }

    private static void layoutForScreen() {
        HORIZONTAL_MARGIN = screenWidth / 2 - TILE_WIDTH * 2;
        VERTICAL_MARGIN = screenHeight / 2;
        joystick.setCenter(2 * TILE_WIDTH, screenHeight - TILE_WIDTH / 2 - TILE_WIDTH);
        int buttonRadius = (int) buttonA.getRadius();
        buttonA.setCenter(screenWidth - buttonRadius - BUTTON_A_RIGHT_PADDING, screenHeight - buttonRadius - BUTTON_A_BOTTOM_PADDING);
        buttonB.setCenter(screenWidth - buttonRadius - BUTTON_B_RIGHT_PADDING, screenHeight - buttonRadius - BUTTON_B_BOTTOM_PADDING);
        pauseButton.setCenter(screenWidth / 2, screenHeight - TILE_WIDTH / 2);
        menu.setCenter(screenWidth / 2, screenHeight / 2);
    }

    public static void setPendingScreenSize(int width, int height) {
        // The manifest keeps the activity, and so the game, through size changes like folding or unfolding. The
        // game board reports its new size, which the next frame applies once it's safe. The display metrics aren't
        // used, as right after the change they still subtract the navigation bar of the old layout
        pendingScreenWidth = width;
        pendingScreenHeight = height;
    }

    public static void applyPendingResize() {
        // Room switches and portal travel move the character and camera for the old size, so let them finish
        boolean sizeChanged = pendingScreenWidth != screenWidth || pendingScreenHeight != screenHeight;
        if (sizeChanged && roomSwitchEffect == null && handleTouch) {
            resize(pendingScreenWidth, pendingScreenHeight);
        }
    }

    private static void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        layoutForScreen();
        if (currentMap == null) {
            (new MainMenu()).execute();
            return;
        }
        // Keep the character where it is in the cave and center the camera on it
        MathVector characterInRoom = character.getPositionInRoom();
        canvas.dx = (float) (screenWidth / 2 - characterInRoom.x);
        canvas.dy = (float) (screenHeight / 2 - characterInRoom.y);
        canvas.assertMapMargins();
        character.setxPosInScreen(characterInRoom.x + canvas.dx);
        character.setyPosInScreen(characterInRoom.y + canvas.dy);
        for (HUDAdvice advice : advices) {
            advice.layoutForScreen();
        }
        // The player is busy folding the phone, so pause. Or pause again, to rebuild the menu for the new size
        if (paused && hudElements.contains(menu)) {
            unpause();
        }
        if (canPause()) {
            pause();
        }
    }

    private void initGfx() {
        stopFrameUpdates();

        (new MainMenu()).execute();

        canvas.invalidate();

        startFrameUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startFrameUpdates();
    }

    @Override
    protected void onPause() {
        stopFrameUpdates();
        // Come back to the pause menu instead of straight into the action
        if (canPause()) {
            pause();
        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (currentMap == null) {
            super.onBackPressed();
        } else {
            togglePause();
        }
    }

    public static void togglePause() {
        if (canPause()) {
            pause();
        } else if (paused && hudElements.contains(menu)) {
            unpause();
        }
    }

    private static boolean canPause() {
        // Not while dead, reading a tip or travelling through a portal. Nor during the fade into a game, which
        // can show a tip when it ends
        if (currentMap == null || paused || !handleTouch) {
            return false;
        }
        for (GameEffect effect : gameEffects) {
            if (effect instanceof FadeEffect) {
                return false;
            }
        }
        return true;
    }

    private void startFrameUpdates() {
        framePacer.reset();
        Choreographer.getInstance().removeFrameCallback(frameUpdate);
        Choreographer.getInstance().postFrameCallback(frameUpdate);
    }

    private void stopFrameUpdates() {
        Choreographer.getInstance().removeFrameCallback(frameUpdate);
    }

    private Choreographer.FrameCallback frameUpdate = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (framePacer.shouldUpdate(frameTimeNanos)) {
                canvas.invalidate();
            }
            Choreographer.getInstance().postFrameCallback(this);
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
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    // Only the finger that went down presses. Pressing again under the fingers already on the
                    // screen would repeat their A and B actions
                    int index = ev.getActionIndex();
                    double xDown = ev.getX(index);
                    double yDown = ev.getY(index);
                    boolean nothingPressed = manageDownTouch(xDown, yDown, ev.getPointerId(index), index);
                    if (nothingPressed && !paused && currentMap != null) {
                        touchScreen(xDown, yDown, ev.getPointerId(index));
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP: {
                    manageUpTouch(ev.getPointerId(ev.getActionIndex()));
                    break;
                }
                case MotionEvent.ACTION_CANCEL: {
                    // The system took the gesture, so the fingers are gone without being lifted, and the chain stays
                    stopHold();
                    for (int i = 0; i < ev.getPointerCount(); i++) {
                        manageUpTouch(ev.getPointerId(i));
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    for (int i = 0; i < ev.getPointerCount(); i++) {
                        if (currentMap != null) {
                            // Match fingers by id, as their index changes when another finger lifts
                            if (joystick.isClickable() && joystick.isOn() && joystick.getTouchId() == ev.getPointerId(i)) {
                                joystick.moveJoystick(ev.getX(i), ev.getY(i));
                            }
                        } else {
                            character.setPositionInRoom(ev.getX(i), ev.getY(i));
                        }
                    }
                    break;
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
            MathVector objectiveInRoom = objective.screenToRoom();
            ClickableEnemy enemyInTheWay = getEnemyInTheWay(objectiveInRoom);
            if (enemyInTheWay != null) {
                // The chain hits the enemy instead of going past it, so the hook never pulls you towards one
                strikeAt(enemyInTheWay);
            } else if (character.distanceTo(objectiveInRoom) <= getHookReach()) {
                if (isHookable(objectiveInRoom)) {
                    decideBetweenFastReloadOrShoot(objective);
                } else {
                    if (character.isHooked()) {
                        if (SystemClock.uptimeMillis() - lastTap < DOUBLE_TAP_MILLIS) {
                            character.getHook().setFastReloading(true);
                        }
                    }
                }
            }
        }
        lastTap = SystemClock.uptimeMillis();
    }

    // A finger on the screen, outside the controls, throws the hook. Held, it also reels the chain in, and lets go of
    // the chain when it's lifted, so the character flies on. Only one finger holds at a time
    private void touchScreen(double x, double y, int id) {
        manageHooking(x, y);
        if (holdTouchId < 0) {
            holdTouchId = id;
            holdDownTime = SystemClock.uptimeMillis();
        }
    }

    // The held finger was lifted. A tap leaves the chain, but a finger held long enough to reel it in lets go of it
    private static void releaseHold() {
        boolean held = SystemClock.uptimeMillis() - holdDownTime >= HOLD_MILLIS;
        stopHold();
        if (held && character.getHook() != null) {
            character.removeHook();
        }
    }

    private static void stopHold() {
        holdTouchId = -1;
    }

    // Reels the chain in while a finger is held on the screen, whichever chain it is by then
    public static void updateHold() {
        boolean reeling = holdTouchId >= 0 && SystemClock.uptimeMillis() - holdDownTime >= HOLD_MILLIS;
        if (reeling) {
            // A held finger isn't a tap, so the next tap doesn't count as a double tap
            lastTap = 0;
        }
        if (character != null && character.getHook() != null) {
            character.getHook().setReloading(reeling && character.getHook().isHooked());
        }
    }

    private static double getHookReach() {
        return (character.getMaxHookNodes() - 1) * Hook.SEPARATION;
    }

    // The hook is thrown at an enemy, and hits it when it's within reach. Rock doesn't stop it, as worms dig through
    // rock and hits shove enemies into it, where they couldn't be hit again
    public static void strikeAt(ClickableEnemy target) {
        MathVector start = character.getPositionInRoom();
        MathVector toTarget = new MathVector(start, target.getPositionInRoom());
        if (toTarget.magnitude() - target.getBodyRadius() <= getHookReach()) {
            new HookStrike(target);
            target.hit(start);
        } else {
            new HookStrike(toTarget.rescaled(getHookReach()).applyTo(start));
        }
    }

    // Rock, a door, or the edge of the cave the character came in through
    private static boolean isHookable(MathVector point) {
        if (!isInRoom(point.x, point.y)) {
            return isEntranceWall(point.x, point.y);
        }
        return Color.alpha(canvas.mapBitmap.getPixel((int) point.x, (int) point.y)) == 255 || checkIfDoorsContain(point);
    }

    // Beyond the top of the cave, and beyond the side the character came in from, it can't go back, as if there was
    // rock. So the hook grips there too, when it's thrown through the entrance. The exits stay open
    private static boolean isEntranceWall(double x, double y) {
        int entranceX = currentMap.getEntrance().tileX;
        return y < 0 || (x < 0 && entranceX == 0) || (x >= currentMap.getWidth() && entranceX == mapXTiles - 1);
    }

    // The first rock, door or wall of the entrance on the straight line between two points in the room, if any
    private static MathVector getObstacle(MathVector from, MathVector to) {
        MathVector direction = new MathVector(from, to);
        int length = (int) direction.magnitude();
        if (length == 0) {
            return null;
        }
        direction.normalize();
        for (int i = 1; i <= length; i++) {
            MathVector point = direction.scaled(i).applyTo(from);
            if (checkIfDoorsContain(point)) {
                return point;
            }
            if (!isInRoom(point.x, point.y)) {
                return isEntranceWall(point.x, point.y) ? point : null;
            }
            if (Color.alpha(canvas.mapBitmap.getPixel((int) point.x, (int) point.y)) == 255) {
                return point;
            }
        }
        return null;
    }

    private static ClickableEnemy getEnemyInTheWay(MathVector objectiveInRoom) {
        MathVector start = character.getPositionInRoom();
        MathVector end = objectiveInRoom;
        if (start.distanceTo(end) > getHookReach()) {
            end = new MathVector(start, end).rescaled(getHookReach()).applyTo(start);
        }
        ClickableEnemy closest = null;
        for (GameEnemy enemy : enemies) {
            if (enemy instanceof ClickableEnemy && ((ClickableEnemy) enemy).isClickable()) {
                ClickableEnemy target = (ClickableEnemy) enemy;
                boolean inTheWay = GameMath.distanceToSegment(target.getPositionInRoom(), start, end) <= target.getBodyRadius() + Hook.RADIUS;
                if (inTheWay && (closest == null || character.distanceTo(target) < character.distanceTo(closest))) {
                    closest = target;
                }
            }
        }
        return closest;
    }

    // A double tap reels in at once the chain the first tap threw. Any other tap throws the hook
    private void decideBetweenFastReloadOrShoot(MathVector objective) {
        if (character.isHooked()) {
            if (character.getHook().getNodesNumber() > Hook.MIN_RELOADING_NODES && !character.getHook().isFastReloading() && SystemClock.uptimeMillis() - lastTap < DOUBLE_TAP_MILLIS) {
                character.getHook().setFastReloading(true);
            } else {
                character.shootHook(objective.x, objective.y);
            }
        } else {
            character.shootHook(objective.x, objective.y);
        }
    }

    private void manageUpTouch(int id) {
        if (id == holdTouchId) {
            releaseHold();
        }
        Vector joined = new Vector();
        joined.addAll(hudElements);
        if (!paused) {
            joined.addAll(enemies);
        }
        for (int k = 0; k < joined.size(); k++) {
            Object element = joined.get(k);
            if (element instanceof Clickable) {
                Clickable clickable = (Clickable) element;
                if (clickable.isOn() && clickable.getTouchId() == id) {
                    clickable.reset();
                }
            }
        }
    }

    private boolean manageDownTouch(double xDown, double yDown, int id, int pointerIndex) {
        boolean nothingPressed = true;

        Vector joined = new Vector();
        joined.addAll(hudElements);
        for (int k = 0; k < joined.size(); k++) {
            Object element = joined.get(k);
            if (element instanceof Clickable) {
                Clickable clickable = (Clickable) element;
                if (clickable.isClickable()) {
                    if (clickable.pressed(xDown, yDown)) {
                        clickable.press(xDown, yDown, id, pointerIndex);
                        nothingPressed = false;
                    }
                }
            }
        }
        if (nothingPressed && !paused) {
            // The controls come first. And a tap throws the hook at a single enemy, the closest, even when its generous
            // hit area reaches several, like the segments of a worm
            ClickableEnemy enemy = getTappedEnemy(xDown, yDown);
            if (enemy != null) {
                enemy.press(xDown, yDown, id, pointerIndex);
                nothingPressed = false;
            }
        }
        if (currentMap == null) {
            MyActivity.character.setPositionInRoom(xDown,yDown);
        }
        return nothingPressed;
    }

    private static ClickableEnemy getTappedEnemy(double x, double y) {
        MathVector tap = new MathVector(x, y);
        ClickableEnemy closest = null;
        double closestDistance = 0;
        for (GameEnemy enemy : enemies) {
            if (enemy instanceof ClickableEnemy) {
                ClickableEnemy clickable = (ClickableEnemy) enemy;
                double distance = tap.distanceTo(clickable.getPositionInScreen()) - clickable.getBodyRadius();
                if (clickable.isClickable() && clickable.pressed(x, y) && (closest == null || distance < closestDistance)) {
                    closest = clickable;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    public static boolean checkIfDoorsContain(MathVector point) {
        for (GameDynamicObject dynamicObject : dynamicObjects) {
            if (dynamicObject instanceof Door) {
                if (dynamicObject.getBounds().contains(point)) {
                    return true;
                }
            }
        }
        return false;
    }

    // The first rock or door in the tap's direction, as far as the hook reaches, or the tap itself
    private MathVector checkIfSomethingInTheWay(double xDown, double yDown) {
        MathVector tap = new MathVector(xDown, yDown);
        MathVector direction = new MathVector(character.getPositionInScreen(), tap);
        if (direction.isNull() || direction.magnitude() > getHookReach()) {
            return tap;
        }
        MathVector start = character.getPositionInRoom();
        MathVector obstacle = getObstacle(start, direction.rescaled(getHookReach()).applyTo(start));
        return (obstacle != null) ? obstacle.roomToScreen() : tap;
    }

    // Also lets go of the held finger, as its lifting may never be handled
    public static void hideControls() {
        stopHold();
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
        stopHold();
        joystick.reset();
        buttonA.reset();
        buttonB.reset();
        joystick.setClickable(false);
        buttonA.setClickable(false);
        buttonB.setClickable(false);
        if (extendButton != null)
            extendButton.setClickable(false);
    }

    public static void setHUDClickable(){
        joystick.setClickable(true);
        buttonA.setClickable(true);
        buttonB.setClickable(true);
        if (extendButton != null)
            extendButton.setClickable(true);
    }

    public static void resetObjectsLists(){
        canvas.gameObjects.clear();
        dynamicObjects.clear();
        enemies.clear();
        Particles.clear();
        ScreenShake.clear();
        canvas.gameObjects.add(character);
        dynamicObjects.add(character);
    }

    public static void switchMap() {
        if (roomSwitchEffect == null) {
            setHUDUnclickable();

            resetObjectsLists();

            Bitmap currentMapInScreen = canvas.getMapInScreen();
            currentMap.extend();
            canvas.generateMap();
            if (currentMap.getEntrance().tileX == 0) {
                canvas.dx = 0;
                Bitmap nextMapInScreen = canvas.getMapInScreen();
                roomSwitchEffect = new SwitchMapHorizontalEffect(currentMapInScreen, nextMapInScreen, 1);
            } else if (currentMap.getEntrance().tileX == mapXTiles - 1) {
                canvas.dx = MyActivity.screenWidth - MyActivity.currentMap.getWidth();
                Bitmap nextMapInScreen = canvas.getMapInScreen();
                roomSwitchEffect = new SwitchMapHorizontalEffect(currentMapInScreen, nextMapInScreen, -1);
            } else {
                canvas.dy = 0;
                Bitmap nextMapInScreen = canvas.getMapInScreen();
                roomSwitchEffect = new SwitchMapVerticalEffect(currentMapInScreen, nextMapInScreen, 1);
            }
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
                        PortalPowerUp portalPowerUp = new PortalPowerUp(screenWidth / 6 - canvas.dx, 3 * screenHeight / 4 - canvas.dy, TILE_WIDTH / 2, false, false);
                        powerUpButtons.add(new HUDPowerUpButton(screenWidth / 6, 3 * screenHeight / 4, TILE_WIDTH * 2, true, portalPowerUp, character.getPowerUps().get(i)));
                        break;
                    case GamePowerUp.COMPASS:
                        CompassPowerUp compassPowerUp = new CompassPowerUp(screenWidth / 6 - canvas.dx, screenHeight / 4 - canvas.dy, (int) (TILE_WIDTH * 0.8), false, false);
                        powerUpButtons.add(new HUDPowerUpButton(screenWidth / 6, screenHeight / 4, TILE_WIDTH * 2, MyActivity.character.getCompass() == null, compassPowerUp, character.getPowerUps().get(i)));
                        break;
                    case GamePowerUp.BOMB:
                        BombPowerUp bombPowerUp = new BombPowerUp( 5 * screenWidth / 6 - canvas.dx, 3 * screenHeight / 4 - canvas.dy, (int) (TILE_WIDTH * 0.8), false, false);
                        powerUpButtons.add(new HUDPowerUpButton( 5 * screenWidth / 6, 3 * screenHeight / 4, TILE_WIDTH * 2, true, bombPowerUp, character.getPowerUps().get(i)));
                        break;
                    case GamePowerUp.INFINITE_JUMPS:
                        InfiniteJumpsPowerUp infiniteJumpsPowerUp = new InfiniteJumpsPowerUp( 5 * screenWidth / 6 - canvas.dx, screenHeight / 4 - canvas.dy, (int) (TILE_WIDTH * 0.9), (int) (TILE_WIDTH * 0.8), false, false);
                        powerUpButtons.add(new HUDPowerUpButton( 5 * screenWidth / 6, screenHeight / 4, TILE_WIDTH * 2, character.getInfiniteJumpsTimer() == null, infiniteJumpsPowerUp, character.getPowerUps().get(i)));
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

    public void save () {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("Seed", seed);
        editor.putInt("Level", level);
        editor.putString("Entrance", currentMap.getEntrance().toString());
        editor.putInt("Portals", character.getPowerUps().get(GamePowerUp.PORTAL));
        editor.putInt("Compass", character.getPowerUps().get(GamePowerUp.COMPASS));
        editor.putInt("Bombs", character.getPowerUps().get(GamePowerUp.BOMB));
        editor.putInt("Jumps", character.getPowerUps().get(GamePowerUp.INFINITE_JUMPS));
        editor.putInt("ExplosionsUsed", character.getExplosionsUsed());
        editor.putInt("Coins", character.getCoins());
        saveHealth();
        editor.commit();
    }

    public void saveHealth() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat("Health", (float) character.getHealth());
        editor.commit();
    }

    public void saveAdvices() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("PortalsAdvice", portalsAdvice);
        editor.putInt("CompassAdvice", compassAdvice);
        editor.putInt("BombAdvice", bombAdvice);
        editor.putInt("JumpsAdvice", jumpsAdvice);
        editor.commit();
    }

}
