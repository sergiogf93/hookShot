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
import com.htss.hookshot.executions.MainMenu;
import com.htss.hookshot.game.hud.HUDNotification;
import com.htss.hookshot.game.hud.advices.HUDAdvice;
import com.htss.hookshot.game.hud.HUDAimArrow;
import com.htss.hookshot.game.hud.HUDArrowButton;
import com.htss.hookshot.game.hud.HUDCircleButton;
import com.htss.hookshot.game.hud.HUDElement;
import com.htss.hookshot.game.hud.HUDHookStick;
import com.htss.hookshot.game.hud.HUDMenu;
import com.htss.hookshot.game.hud.HUDPauseButton;
import com.htss.hookshot.game.hud.HUDShopMenu;
import com.htss.hookshot.game.hud.HUDPlaygroundMenu;
import com.htss.hookshot.game.hud.HUDPowerUpButton;
import com.htss.hookshot.game.hud.HUDPowerUpStrip;
import com.htss.hookshot.game.hud.HUDReach;
import com.htss.hookshot.game.hud.HUDReelGauge;
import com.htss.hookshot.game.hud.HUDStatus;
import com.htss.hookshot.game.hud.HUDUnhookButton;
import com.htss.hookshot.game.hud.HUDUseButton;
import com.htss.hookshot.game.hud.Joystick;
import com.htss.hookshot.game.object.debug.Circle;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.enemies.ClickableEnemy;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.game.object.hook.HookStrike;
import com.htss.hookshot.game.object.interactables.Shop;
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
import com.htss.hookshot.map.Cave;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.map.OpenWorld;
import com.htss.hookshot.map.World;
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

    // The game advances this many times a second whatever the screen's refresh rate, and frames that are dropped are
    // caught up on the next one
    public static final int UPDATES_PER_SECOND = 60;
    // Most time between two presses of a button for them to count as a double press. Measured with
    // SystemClock.uptimeMillis, which doesn't jump when the phone's clock is changed
    public static final long DOUBLE_TAP_MILLIS = 500;
    // Least time a finger stays on the screen to reel the chain in. A quicker one is a tap
    private static final long HOLD_MILLIS = 200;

    // Which controls are in use, switched from the pause menu so a change can be tried straight away. Classic and new
    // throw the hook where the finger lands, reel it in while the finger is held and let go when it lifts. Classic
    // keeps the E button that follows the chain and the B button that does everything; new pays the chain out with the
    // joystick instead, and gives the powers their own row. Twin aims with a second stick on the right, which throws
    // the hook when it's let go, with buttons beside it to reel the chain in and out and under it to let go. Twin 2
    // drops the reeling buttons: the joystick reels in when pushed towards where the chain is hooked and lets out when
    // pushed away, and tapping the right stick twice zips
    public static final int CONTROLS_CLASSIC = 0, CONTROLS_NEW = 1, CONTROLS_TWIN = 2, CONTROLS_TWIN_2 = 3;
    public static final String[] CONTROL_NAMES = {"CLASSIC", "NEW", "TWIN", "TWIN 2"};
    public static int controls = CONTROLS_NEW;

    // Either of the controls with a stick on the right to aim the chain with
    public static boolean isTwin() {
        return controls == CONTROLS_TWIN || controls == CONTROLS_TWIN_2;
    }
    // How forgiving aiming the chain at enemies with the stick is, as a thumb can't point as finely as a finger taps.
    // An enemy is aimed at when the aim passes this close to its body, or within this many degrees of it and no
    // further to the side than the widest. The distances are set with the tile size
    private static final double ASSIST_DEGREES = 10;
    private static double ASSIST_MARGIN, ASSIST_WIDEST;
    public static int TILE_WIDTH, HORIZONTAL_MARGIN, VERTICAL_MARGIN;
    // About the biggest tile a phone gets, 7.2 tiles over a 430 dp short side
    private static final int MAX_TILE_WIDTH_DP = 60;
    private static int BUTTON_A_BOTTOM_PADDING,BUTTON_A_RIGHT_PADDING,BUTTON_B_BOTTOM_PADDING,BUTTON_B_RIGHT_PADDING;

    public static GameBoard canvas;
    private final FramePacer framePacer = new FramePacer(UPDATES_PER_SECOND);
    public static int screenHeight, screenWidth; //Default 110 80, for screen size 30 20
    private static int pendingScreenWidth, pendingScreenHeight;
    public static MainCharacter character;
    public static Joystick joystick;
    public static HUDCircleButton extendButton, buttonUse, buttonB, buttonA, buttonUnhook, buttonRetract, buttonLetOut;
    public static HUDHookStick hookStick;
    public static HUDAimArrow aimArrow;
    public static HUDPowerUpStrip powerUpStrip;
    public static HUDReach reach;
    public static HUDReelGauge reelGauge;
    public static HUDPauseButton pauseButton;
    public static HUDMenu menu;
    public static HUDStatus status;
    public static LinkedList<HUDPowerUpButton> powerUpButtons = new LinkedList<HUDPowerUpButton>();
    public static boolean paused = false, handleTouch = true, debugging = false;
    // In the playground, which never saves and has its own menu, and whether the character can be hurt there
    public static boolean playground = false, godMode = false;
    // In the open world, which is saved apart from the game of caves one after another, and the seed it's made from
    public static boolean openWorld = false;
    public long openSeed = 0;
    // Whether the camera goes after the character. Not while a portal carries it, which moves the camera itself
    public static boolean cameraFollows = true;
    public static HUDPlaygroundMenu playgroundMenu;
    public static HUDShopMenu shopMenu;
    public static long lastTap = 0;
    // Where a shot found nothing to grip, and the character's frame then
    private static MathVector missedShot = null;
    private static int missedShotFrame = Integer.MIN_VALUE / 2;
    // The finger working the chain, outside the controls: where it went down, where it is, and whether it has moved
    // far enough to be sliding. With a chain out it reels; without one it aims a shot
    private static int chainTouchId = -1;
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
        controls = Math.max(0, Math.min(preferences.getInt("Controls", CONTROLS_NEW), CONTROL_NAMES.length - 1));
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
        ASSIST_MARGIN = TILE_WIDTH * 0.6;
        ASSIST_WIDEST = TILE_WIDTH * 1.5;

        // The controls and menu are placed by layoutForScreen, which runs again when the screen size changes
        joystick = new Joystick(0, 0, TILE_WIDTH * 2, TILE_WIDTH * 2);

        int buttonRadius = (int) (TILE_WIDTH*0.75);
        buttonA = new HUDCircleButton(0, 0, buttonRadius, "A", true, new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.isOnFloor()) {
                    MyActivity.character.jump( -1 * MyActivity.TILE_WIDTH);
                } else if (MyActivity.character.isSwimming()) {
                    MyActivity.character.swim();
                } else if (MyActivity.character.getCurrentPowerUp() == GamePowerUp.INFINITE_JUMPS) {
                    MyActivity.character.usePowerUp();
                }

                return 0;
            }
        }
        );

        // With the slide controls B only ever lets go of the chain. The classic ones also use the power with it
        buttonB = new HUDCircleButton(0, 0, buttonRadius, "B", true, new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.getHook() != null) {
                    // Zipping up the chain into rock, letting go would leave the character stuck inside it
                    if (!MyActivity.character.getHook().isFastReloading() || !MyActivity.character.inContactWithMap(MyActivity.character.getMargin())) {
                        MyActivity.character.removeHook();
                    }
                } else if (controls == CONTROLS_CLASSIC) {
                    usePowerOrPortal();
                }
                return 0;
            }
        }
        );

        // Steps through a portal it's standing in, or else uses the power picked in the strip, chain or no chain
        buttonUse = new HUDUseButton(TILE_WIDTH * 0.5f, new Execution() {
            @Override
            public double execute() {
                usePowerOrPortal();
                return 0;
            }
        }
        );

        // The twin stick controls: the stick that aims the chain, the pair beside it that reels it in and lets it
        // out while held, and the button under it that lets go of it. The ones used mid swing are the bigger ones
        hookStick = new HUDHookStick(0, 0, TILE_WIDTH * 2, TILE_WIDTH * 2);
        aimArrow = new HUDAimArrow();
        buttonRetract = new HUDArrowButton(TILE_WIDTH * 0.55f, true, null, null, new Execution() {
            @Override
            public double execute() {
                // Pressed twice quickly, it zips up the chain
                if (MyActivity.character.isHooked() && MyActivity.character.getHook().getNodesNumber() > Hook.MIN_RELOADING_NODES) {
                    MyActivity.character.getHook().setFastReloading(true);
                }
                return 0;
            }
        });
        buttonLetOut = new HUDArrowButton(TILE_WIDTH * 0.45f, false, null, null, null);
        buttonUnhook = new HUDUnhookButton(TILE_WIDTH * 0.55f, new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.getHook() != null) {
                    // Zipping up the chain into rock, letting go would leave the character stuck inside it
                    if (!MyActivity.character.getHook().isFastReloading() || !MyActivity.character.inContactWithMap(MyActivity.character.getMargin())) {
                        MyActivity.character.removeHook();
                    }
                }
                return 0;
            }
        }
        );

        powerUpStrip = new HUDPowerUpStrip();
        reach = new HUDReach();
        reelGauge = new HUDReelGauge();

        pauseButton = new HUDPauseButton(0, 0, TILE_WIDTH, (int) (TILE_WIDTH * 0.5));

        canvas = (GameBoard) findViewById(R.id.the_canvas);
        canvas.myActivity = this;
        canvas.DEFAULT_FONT_SIZE = 48*MyActivity.TILE_WIDTH /100;
        canvas.SMALL_FONT_SIZE = 27*MyActivity.TILE_WIDTH /100;
        canvas.arcadeClassicFont = Typeface.createFromAsset(getAssets(), "fonts/arcadeclassic.ttf");
        canvas.joystickMonospace = Typeface.createFromAsset(getAssets(),"fonts/joystix_monospace.ttf");
        canvas.setFont(GameBoard.ARCADECLASSIC_FONT_KEY, GameBoard.DEFAULT_FONT_SIZE);

        int nMenuButton = 3;
        int menuButtonHeight = TILE_WIDTH;
        int menuButtonSeparation = TILE_WIDTH / 5;
        int menuWidth = 5*TILE_WIDTH;
        int menuHeight = menuButtonHeight*nMenuButton + (nMenuButton+1)*menuButtonSeparation;
        menu = new HUDMenu(0, 0, menuWidth, menuHeight, menuButtonHeight, menuButtonSeparation);
        playgroundMenu = new HUDPlaygroundMenu();
        shopMenu = new HUDShopMenu();
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
        reelGauge.setCenter((int) (screenWidth - TILE_WIDTH * 0.3), screenHeight * 2 / 5);
        if (isTwin()) {
            // The chain stick sits a little higher than the joystick, to fit the two buttons underneath, and in from
            // the edge when the pair that reels is to its right. The powers run down the right edge above them all
            hookStick.setHome((int) (screenWidth - TILE_WIDTH * (controls == CONTROLS_TWIN ? 2.8 : 2)), (int) (screenHeight - TILE_WIDTH * 2.35));
            buttonRetract.setCenter((int) (screenWidth - TILE_WIDTH * 0.8), (int) (hookStick.getHomeY() - TILE_WIDTH * 0.6));
            buttonLetOut.setCenter((int) (screenWidth - TILE_WIDTH * 0.8), (int) (hookStick.getHomeY() + TILE_WIDTH * 0.6));
            // Far enough apart that a thumb between them presses neither by mistake
            buttonUnhook.setCenter((int) (hookStick.getHomeX() - TILE_WIDTH * 0.8), (int) (screenHeight - TILE_WIDTH * 0.6));
            buttonUse.setCenter((int) (hookStick.getHomeX() + TILE_WIDTH * 0.8), (int) (screenHeight - TILE_WIDTH * 0.6));
            buttonUse.setRadius(TILE_WIDTH * 0.45f);
            powerUpStrip.setVertical(true);
            powerUpStrip.setCenter((int) (screenWidth - TILE_WIDTH * 0.5), (int) (TILE_WIDTH * 0.65));
        } else {
            // The power row sits above the chain buttons: what's picked, then the button that uses it
            buttonUse.setRadius(TILE_WIDTH * 0.5f);
            buttonUse.setCenter(buttonB.getxCenter(), (int) (buttonB.getyCenter() - TILE_WIDTH * 1.7));
            powerUpStrip.setVertical(false);
            powerUpStrip.setCenter((int) (buttonUse.getxCenter() - TILE_WIDTH * 1.1), buttonUse.getyCenter());
        }
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

    // Whether the character is still in the game. It leaves it as it dies
    public static boolean isCharacterAlive() {
        return character != null && canvas.gameObjects.contains(character);
    }

    // The camera jumps to the character, as when a game starts or the screen changes size
    public static void centerCameraOnCharacter() {
        canvas.dx = (float) (screenWidth / 2 - character.getxPosInRoom());
        canvas.dy = (float) (screenHeight / 2 - character.getyPosInRoom());
        canvas.assertMapMargins();
    }

    public static void applyPendingResize() {
        // Portal travel moves the camera for the old size, so it's left to finish
        boolean sizeChanged = pendingScreenWidth != screenWidth || pendingScreenHeight != screenHeight;
        if (sizeChanged && handleTouch) {
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
        centerCameraOnCharacter();
        for (HUDAdvice advice : advices) {
            advice.layoutForScreen();
        }
        // The player is busy folding the phone, so pause. Or pause again, to rebuild the menu for the new size
        if (paused && isMenuOpen()) {
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
        // The game may not be opened again before Android closes it
        if (openWorld && character != null) {
            saveOpenWorld(isCharacterAlive());
        }
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
        } else if (paused && isMenuOpen()) {
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

    // Whether the screen is being kept on. Thumbs resting on the sticks don't count as touching it, so the phone dimmed
    // and then slept in the middle of a game. Menus and the pause screen still let it
    private boolean keptOn = false;

    private Choreographer.FrameCallback frameUpdate = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            boolean playing = currentMap != null && !paused;
            if (playing != keptOn) {
                keptOn = playing;
                canvas.setKeepScreenOn(playing);
            }
            int updates = framePacer.updatesDue(frameTimeNanos);
            if (updates > 0) {
                canvas.queueUpdates(updates);
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
        return World.contains(x, y);
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
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                // Only the finger that went down presses. Pressing again under the fingers already on the
                // screen would repeat their A and B actions
                int index = ev.getActionIndex();
                double xDown = ev.getX(index);
                double yDown = ev.getY(index);
                boolean nothingPressed = manageDownTouch(xDown, yDown, ev.getPointerId(index), index);
                // With the twin sticks the chain is only worked from its stick, so the cave itself isn't tapped
                if (nothingPressed && !paused && currentMap != null && !isTwin()) {
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
                cancelChainTouch();
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
                        if (hookStick.isClickable() && hookStick.isOn() && hookStick.getTouchId() == ev.getPointerId(i)) {
                            hookStick.moveHandle(ev.getX(i), ev.getY(i));
                        }
                    } else {
                        character.setPositionInRoom(ev.getX(i), ev.getY(i));
                    }
                }
                break;
            }
        }
    }

    // Throws the hook where the finger landed: at a hookable object, at an enemy in the way, or at the rock behind
    // it. A second tap on a chain that's out reels it in at once instead
    private void manageHooking(double xHook, double yHook) {
        for (GameDynamicObject dynamicObject : dynamicObjects) {
            if (dynamicObject instanceof Hookable && dynamicObject.pressed(xHook, yHook)) {
                character.shootHook(dynamicObject.getxPosInScreen(), dynamicObject.getyPosInScreen());
                lastTap = SystemClock.uptimeMillis();
                return;
            }
        }
        MathVector objective = checkIfSomethingInTheWay(xHook, yHook);
        MathVector objectiveInRoom = objective.screenToRoom();
        ClickableEnemy enemyInTheWay = getEnemyInTheWay(objectiveInRoom);
        if (enemyInTheWay != null) {
            // The chain hits the enemy instead of going past it, so the hook never pulls you towards one
            strikeAt(enemyInTheWay);
        } else if (character.distanceTo(objectiveInRoom) <= getHookReach() && isHookable(objectiveInRoom)) {
            decideBetweenFastReloadOrShoot(objective);
        } else if (character.isHooked() && SystemClock.uptimeMillis() - lastTap < DOUBLE_TAP_MILLIS) {
            character.getHook().setFastReloading(true);
        } else {
            // Nothing to grip there, so the shot is shown falling short rather than doing nothing at all
            missedShot = objective;
            missedShotFrame = (character == null) ? 0 : character.getFrame();
        }
        lastTap = SystemClock.uptimeMillis();
    }

    // A second tap reels in at once the chain the first tap threw. Any other tap throws the hook
    private void decideBetweenFastReloadOrShoot(MathVector objective) {
        if (character.isHooked() && character.getHook().getNodesNumber() > Hook.MIN_RELOADING_NODES
                && !character.getHook().isFastReloading() && SystemClock.uptimeMillis() - lastTap < DOUBLE_TAP_MILLIS) {
            character.getHook().setFastReloading(true);
        } else {
            character.shootHook(objective.x, objective.y);
        }
    }

    // A finger on the screen, outside the controls, throws the hook at once. Held, it reels the chain in, and lets go
    // of it when it's lifted, so the character flies on. Only one finger works the chain at a time
    private void touchScreen(double x, double y, int id) {
        manageHooking(x, y);
        if (chainTouchId < 0) {
            chainTouchId = id;
            holdDownTime = SystemClock.uptimeMillis();
        }
    }

    // The finger was lifted. A tap leaves the chain, and a finger held long enough to reel it in lets go of it, so
    // the character flies on with the swing
    private static void endChainTouch() {
        boolean held = SystemClock.uptimeMillis() - holdDownTime >= HOLD_MILLIS;
        cancelChainTouch();
        if (held && character != null && character.getHook() != null) {
            character.removeHook();
        }
    }

    // The finger is gone: it was lifted, the system took the gesture, or the controls were taken away
    private static void cancelChainTouch() {
        if (character != null && character.getHook() != null) {
            character.getHook().setReloading(false);
        }
        chainTouchId = -1;
    }

    // Reels the chain in while a finger is held on the screen, whichever chain it is by then. With the twin sticks,
    // the pair of buttons beside the chain stick does the reeling instead, for as long as one is held
    public static void updateHold() {
        if (controls == CONTROLS_TWIN_2) {
            // The joystick works the chain, as the character steers
            return;
        }
        if (controls == CONTROLS_TWIN) {
            if (character != null && character.isHooked()) {
                Hook hook = character.getHook();
                hook.setReloading(buttonRetract.isOn());
                if (buttonLetOut.isOn() && !buttonRetract.isOn()) {
                    // A target a step ahead every update, so the chain keeps coming out while the button is held
                    hook.reelTo(hook.getChainLength() + Hook.LET_OUT_SPEED * 2);
                } else {
                    hook.stopReeling();
                }
            }
            return;
        }
        boolean reeling = chainTouchId >= 0 && SystemClock.uptimeMillis() - holdDownTime >= HOLD_MILLIS;
        if (reeling) {
            // A held finger isn't a tap, so the next tap doesn't count as a double tap
            lastTap = 0;
        }
        if (character != null && character.getHook() != null) {
            character.getHook().setReloading(reeling && character.getHook().isHooked());
        }
    }

    // What a shot thrown along a direction from the character would reach: the first rock, door or enemy on its way,
    // as far as the chain goes. Null without a direction
    public static Aim aimAlong(double dx, double dy) {
        MathVector direction = new MathVector(dx, dy);
        if (direction.isNull() || character == null || currentMap == null) {
            return null;
        }
        // Just inside the reach, as a point any further isn't looked along at all
        MathVector far = direction.rescaled(getHookReach() * 0.999).applyTo(character.getPositionInScreen());
        MathVector objective = checkIfSomethingInTheWay(far.x, far.y);
        MathVector objectiveInRoom = objective.screenToRoom();
        ClickableEnemy enemyInTheWay = getEnemyInTheWay(objectiveInRoom);
        if (enemyInTheWay == null) {
            enemyInTheWay = getEnemyAimedNear(direction, character.distanceTo(objectiveInRoom));
        }
        if (enemyInTheWay != null) {
            return new Aim(enemyInTheWay.getPositionInScreen(), false, enemyInTheWay);
        }
        return new Aim(objective, isHookable(objectiveInRoom), null);
    }

    // The enemy the aim passes near enough to count as aimed at, when it doesn't pass through one: the one most in line
    // with it, among those within reach, in plain sight, and no further than the rock the aim ends at. One beyond that
    // rock isn't what's being aimed at, and taking the shot would keep the hook from gripping where it was pointed
    private static ClickableEnemy getEnemyAimedNear(MathVector direction, double distanceToRock) {
        MathVector start = character.getPositionInRoom();
        MathVector aim = direction.getUnitVector();
        ClickableEnemy nearest = null;
        double smallestAngle = 0;
        for (GameEnemy enemy : enemies) {
            if (!(enemy instanceof ClickableEnemy) || !((ClickableEnemy) enemy).isClickable()) {
                continue;
            }
            ClickableEnemy target = (ClickableEnemy) enemy;
            MathVector toTarget = new MathVector(start, target.getPositionInRoom());
            double distance = toTarget.magnitude(), along = toTarget.dotProduct(aim);
            if (along <= 0 || distance - target.getBodyRadius() > Math.min(getHookReach(), distanceToRock + TILE_WIDTH)) {
                continue;
            }
            double aside = Math.sqrt(Math.max(0, distance * distance - along * along));
            double angle = Math.toDegrees(Math.atan2(aside, along));
            boolean near = aside <= target.getBodyRadius() + ASSIST_MARGIN || (angle <= ASSIST_DEGREES && aside <= ASSIST_WIDEST);
            if (near && (nearest == null || angle < smallestAngle)) {
                // In plain sight, up to its body, as one on a wall has its middle right against the rock
                MathVector edge = toTarget.scaled(Math.max(0, 1 - target.getBodyRadius() / distance)).applyTo(start);
                if (getObstacle(start, edge) == null) {
                    nearest = target;
                    smallestAngle = angle;
                }
            }
        }
        return nearest;
    }

    // Throws the hook along a direction, as the chain stick does when it's let go after aiming
    public static void shootToward(double dx, double dy) {
        Aim aim = aimAlong(dx, dy);
        if (aim == null) {
            return;
        }
        if (aim.enemy != null) {
            strikeAt(aim.enemy);
        } else if (aim.grips) {
            character.shootHook(aim.point.x, aim.point.y);
        } else {
            missedShot = aim.point;
            missedShotFrame = character.getFrame();
        }
    }

    // Where an aimed shot would land, in screen coordinates, and what's there: rock it grips, or an enemy it hits
    public static class Aim {
        public final MathVector point;
        public final boolean grips;
        public final ClickableEnemy enemy;

        private Aim(MathVector point, boolean grips, ClickableEnemy enemy) {
            this.point = point;
            this.grips = grips;
            this.enemy = enemy;
        }
    }

    // Where the last shot that found nothing fell short, and when, for the mark that shows how far the chain reaches
    public static MathVector getMissedShot() {
        return missedShot;
    }

    public static int getMissedShotFrame() {
        return missedShotFrame;
    }

    public static double getHookReach() {
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
        return World.isSolid((int) point.x, (int) point.y) || checkIfDoorsContain(point);
    }

    // Beyond the top of the cave, and beyond the side the character came in from, it can't go back, as if there was
    // rock. So the hook grips there too, when it's thrown through the entrance. The exits stay open
    private static boolean isEntranceWall(double x, double y) {
        Cave cave = World.getCurrent();
        // The open world has no way in: beyond its pieces there's only what hasn't been made yet
        if (cave == null || openWorld) {
            return false;
        }
        int entranceX = currentMap.getEntrance().tileX;
        return y < cave.y || (x < cave.x && entranceX == 0) || (x >= cave.x + cave.width && entranceX == mapXTiles - 1);
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
            if (World.isSolid((int) point.x, (int) point.y)) {
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

    private void manageUpTouch(int id) {
        if (id == chainTouchId) {
            endChainTouch();
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
        // The stick that aims the chain comes last, wherever on its side of the screen the thumb lands: the controls and
        // the enemies under it have had their turn
        if (nothingPressed && !paused && currentMap != null && isTwin() && hookStick.isClickable()
                && hudElements.contains(hookStick) && hookStick.isOnItsSide(xDown, yDown)) {
            hookStick.press(xDown, yDown, id, pointerIndex);
            nothingPressed = false;
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
    private static MathVector checkIfSomethingInTheWay(double xDown, double yDown) {
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
    // Steps through a portal the character is standing in, or opens the stall it's standing at, or else uses the power
    // it's carrying
    private static void usePowerOrPortal() {
        for (PortalObject portal : character.getPortals()) {
            // A portal without its twin leads nowhere, so standing in one still places the next
            if (portal.getTwinPortal() != null && character.distanceTo(portal) < portal.getRadius()) {
                portal.use();
                return;
            }
        }
        if (Shop.getNearby() != null) {
            openShop();
            return;
        }
        if (character.getCurrentPowerUp() >= 0) {
            character.usePowerUp();
        }
    }

    // Switched from the pause menu. The controls are laid out again as the game carries on
    public static void setControls(int scheme) {
        controls = scheme;
        canvas.myActivity.saveControls();
        layoutForScreen();
        // Whatever the last controls were doing to the chain stops with them
        if (character != null && character.getHook() != null) {
            character.getHook().setReloading(false);
            character.getHook().setExtending(false);
            character.getHook().stopReeling();
        }
        if (controls != CONTROLS_CLASSIC && extendButton != null) {
            hudElements.remove(extendButton);
            extendButton = null;
        }
    }

    public static void hideControls() {
        cancelChainTouch();
        joystick.reset();
        buttonA.reset();
        buttonB.reset();
        buttonUse.reset();
        powerUpStrip.reset();
        MyActivity.hudElements.remove(MyActivity.joystick);
        MyActivity.hudElements.remove(MyActivity.buttonA);
        MyActivity.hudElements.remove(MyActivity.buttonB);
        MyActivity.hudElements.remove(MyActivity.buttonUse);
        MyActivity.hudElements.remove(MyActivity.powerUpStrip);
        MyActivity.hudElements.remove(MyActivity.reach);
        MyActivity.hudElements.remove(MyActivity.reelGauge);
        hookStick.cancel();
        buttonUnhook.reset();
        buttonRetract.reset();
        buttonLetOut.reset();
        MyActivity.hudElements.remove(MyActivity.hookStick);
        MyActivity.hudElements.remove(MyActivity.buttonUnhook);
        MyActivity.hudElements.remove(MyActivity.buttonRetract);
        MyActivity.hudElements.remove(MyActivity.buttonLetOut);
        MyActivity.hudElements.remove(MyActivity.aimArrow);
        if (extendButton != null) {
            extendButton.setClickable(false);
        }
    }

    public static void addControls() {
        // Placed for the controls in use, which are read from the save after the first layout
        layoutForScreen();
        // The reach mark, the gauge and the aim arrow go under the controls, as the buttons are drawn over them
        if (controls != CONTROLS_CLASSIC) {
            MyActivity.hudElements.add(MyActivity.reach);
        }
        if (controls == CONTROLS_NEW) {
            MyActivity.hudElements.add(MyActivity.reelGauge);
        }
        if (isTwin()) {
            MyActivity.hudElements.add(MyActivity.aimArrow);
        }
        MyActivity.hudElements.add(MyActivity.joystick);
        if (isTwin()) {
            MyActivity.hudElements.add(MyActivity.hookStick);
            if (controls == CONTROLS_TWIN) {
                MyActivity.hudElements.add(MyActivity.buttonRetract);
                MyActivity.hudElements.add(MyActivity.buttonLetOut);
            }
            MyActivity.hudElements.add(MyActivity.buttonUnhook);
        } else {
            MyActivity.hudElements.add(MyActivity.buttonA);
            MyActivity.hudElements.add(MyActivity.buttonB);
        }
        if (controls != CONTROLS_CLASSIC) {
            MyActivity.hudElements.add(MyActivity.buttonUse);
            MyActivity.hudElements.add(MyActivity.powerUpStrip);
        }
    }

    public static void setHUDUnclickable(){
        cancelChainTouch();
        joystick.reset();
        buttonA.reset();
        buttonB.reset();
        buttonUse.reset();
        powerUpStrip.reset();
        joystick.setClickable(false);
        buttonA.setClickable(false);
        buttonB.setClickable(false);
        buttonUse.setClickable(false);
        powerUpStrip.setClickable(false);
        hookStick.cancel();
        buttonUnhook.reset();
        buttonRetract.reset();
        buttonLetOut.reset();
        hookStick.setClickable(false);
        buttonUnhook.setClickable(false);
        buttonRetract.setClickable(false);
        buttonLetOut.setClickable(false);
        if (extendButton != null) {
            extendButton.setClickable(false);
        }
    }

    public static void setHUDClickable(){
        joystick.setClickable(true);
        buttonA.setClickable(true);
        buttonB.setClickable(true);
        buttonUse.setClickable(true);
        powerUpStrip.setClickable(true);
        hookStick.setClickable(true);
        buttonUnhook.setClickable(true);
        buttonRetract.setClickable(true);
        buttonLetOut.setClickable(true);
        if (extendButton != null) {
            extendButton.setClickable(true);
        }
    }

    public static void pause() {
        MyActivity.paused = true;
        hideControls();
        if (playground) {
            playgroundMenu.open();
            return;
        }

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
        playgroundMenu.close();
        shopMenu.close();
    }

    // Pauses the game with the stall's wares on show, rather than the pause menu. Leaving them carries on
    private static void openShop() {
        if (!canPause()) {
            return;
        }
        MyActivity.paused = true;
        hideControls();
        shopMenu.open();
    }

    // Whether the pause menu, the playground's or a stall's is showing
    private static boolean isMenuOpen() {
        return hudElements.contains(menu) || hudElements.contains(playgroundMenu) || hudElements.contains(shopMenu);
    }

    // Back at the start of the playground, healed. It has no ending: dying there brings the character back, and so
    // would leaving through its walls, somehow
    public static void respawnInPlayground() {
        if (character.isHooked()) {
            character.removeHook();
        }
        character.setHealth(character.getMaxHealth());
        character.setP(new MathVector(0, 0));
        character.setPositionInRoom(currentMap.startPosition());
        centerCameraOnCharacter();
    }

    // A few of every power, to try them all
    public static void fillPlaygroundPowers() {
        character.setPowerUp(GamePowerUp.PORTAL, 3);
        character.setPowerUp(GamePowerUp.COMPASS, 3);
        character.setPowerUp(GamePowerUp.BOMB, 3);
        character.setPowerUp(GamePowerUp.INFINITE_JUMPS, 3);
        character.setExplosionsUsed(0);
    }

    // Takes every enemy away at once, without the bursts and loot of beating them
    public static void clearEnemies() {
        for (GameEnemy enemy : new LinkedList<GameEnemy>(enemies)) {
            canvas.gameObjects.remove(enemy);
            dynamicObjects.remove(enemy);
            character.checkIfRemoveInterest(enemy);
        }
        enemies.clear();
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
        editor.apply();
    }

    // Where the character is in the open world and what it has, under names of their own, so the game of caves is left
    // as it was, and what it changed in the world. Every time the character crosses into another piece of it
    public void saveOpenWorld() {
        saveOpenWorld(true);
    }

    // The same, but for where the character is if not the place, as when it died and goes back to where it was last saved
    public void saveOpenWorld(boolean place) {
        OpenWorld.save();
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putLong("OpenSeed", openSeed);
        // In squares, as how many pixels a square is comes from the screen the game was opened on, which on a phone that
        // folds can be another the next time. The pixels it was once kept in go
        if (place) {
            editor.putFloat("OpenSquareX", (float) (character.getxPosInRoom() / Map.SQUARE_SIZE));
            editor.putFloat("OpenSquareY", (float) (character.getyPosInRoom() / Map.SQUARE_SIZE));
            editor.remove("OpenX");
            editor.remove("OpenY");
        }
        editor.putInt("OpenPortals", getPowerUpCount(GamePowerUp.PORTAL));
        editor.putInt("OpenCompass", getPowerUpCount(GamePowerUp.COMPASS));
        editor.putInt("OpenBombs", getPowerUpCount(GamePowerUp.BOMB));
        editor.putInt("OpenJumps", getPowerUpCount(GamePowerUp.INFINITE_JUMPS));
        editor.putInt("OpenExplosionsUsed", character.getExplosionsUsed());
        editor.putInt("OpenCoins", character.getCoins());
        editor.apply();
    }

    // What the top of the screen calls where the character is: the level, or in the open world how deep it is, in tiles
    // under the surface
    public static String getPlaceName() {
        if (playground) {
            return "PLAYGROUND";
        } else if (openWorld) {
            return "DEPTH " + (int) Math.max(0, character.getyPosInRoom() / Map.SQUARE_SIZE);
        }
        return "LEVEL " + canvas.myActivity.level;
    }

    private static int getPowerUpCount(int type) {
        Integer count = character.getPowerUps().get(type);
        return (count == null) ? 0 : count;
    }

    public void saveControls() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("Controls", controls);
        editor.apply();
    }

    public void saveHealth() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat("Health", (float) character.getHealth());
        editor.apply();
    }

    public void saveAdvices() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("PortalsAdvice", portalsAdvice);
        editor.putInt("CompassAdvice", compassAdvice);
        editor.putInt("BombAdvice", bombAdvice);
        editor.putInt("JumpsAdvice", jumpsAdvice);
        editor.apply();
    }

}
