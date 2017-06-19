package com.htss.hookshot.game.object;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;

import com.htss.hookshot.effect.FadeEffect;
import com.htss.hookshot.executions.MainMenu;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.animation.MainCharacterAnimation;
import com.htss.hookshot.game.hud.HUDBar;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.miscellaneous.CompassObject;
import com.htss.hookshot.game.object.miscellaneous.ExplosionObject;
import com.htss.hookshot.game.object.miscellaneous.JumpEffect;
import com.htss.hookshot.game.object.miscellaneous.PortalObject;
import com.htss.hookshot.game.object.miscellaneous.TimerObject;
import com.htss.hookshot.game.object.shapes.BiCircleShape;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.DrawUtil;
import com.htss.hookshot.util.TimeUtil;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by Sergio on 03/08/2016.
 */
public class MainCharacter extends GameCharacter {

    public static final int MAX_HEALTH = 100, MAX_VELOCITY = 15;
    private static final int MAX_EXPLOSIONS = 5;
    private static final int MASS = 1, COLLISION_PRIORITY = 5;

    public static final int BODY_RADIUS = 10*MyActivity.TILE_WIDTH /50, FIST_RADIUS = MyActivity.TILE_WIDTH /8,
                            FOOT_RADIUS = 10*MyActivity.TILE_WIDTH /100, EYE_RADIUS = MyActivity.TILE_WIDTH /25,
                            MIN_HOOSKSHOT_NODES = 3;

    private Paint paint = new Paint();
    private double hookVelocity = 300;
    private int maxHookNodes = 50, facing = 1;
    private Hook hook = null;
    private CircleShape rightHand, leftHand, rightFoot, leftFoot, body;
    private BiCircleShape leftEye, rightEye;
    private HUDBar healthBar;
    private HashMap<Integer, Integer> powerUps = new HashMap<Integer, Integer>();
    private int currentPowerUp = -1, prevPowerUp = -1;
    private LinkedList<PortalObject> portals = new LinkedList<PortalObject>();
    private CompassObject compass;
    private int explosionsUsed = 0;
    private TimerObject infiniteJumpsTimer;

    public MainCharacter(double xPos, double yPos) {
        super(xPos, yPos, MASS, COLLISION_PRIORITY, MAX_VELOCITY, MAX_HEALTH, false, false);
        makeSureNotUnderground = true;
        body = new CircleShape(xPos, yPos, BODY_RADIUS, Color.BLACK, false);
        rightHand = new CircleShape(xPos,yPos,FIST_RADIUS,Color.WHITE, false);
        leftHand = new CircleShape(xPos,yPos,FIST_RADIUS,Color.WHITE, false);
        leftFoot = new CircleShape(xPos,yPos,FOOT_RADIUS,Color.RED, false);
        rightFoot = new CircleShape(xPos,yPos,FOOT_RADIUS,Color.RED, false);
        leftEye = new BiCircleShape(xPos,yPos,EYE_RADIUS*0.8,new MathVector(0,1),EYE_RADIUS,Color.YELLOW);
        rightEye = new BiCircleShape(xPos,yPos,EYE_RADIUS*0.8,new MathVector(0,1),EYE_RADIUS,Color.YELLOW);
        friction = 1.;
        this.healthBar = new HUDBar((int) getxPosInScreen(),(int) getyPosInScreen(), (int) (MyActivity.TILE_WIDTH *1.5),MyActivity.TILE_WIDTH /10,Color.GREEN,new Execution() {
            @Override
            public double execute() {
                return getHealth()/getMaxHealth();
            }
        });
        this.healthBar.setAlpha(0);
        MyActivity.canvas.gameObjects.add(this);
        MyActivity.dynamicObjects.add(this);
    }

    @Override
    public double getxPosInRoom(){
        return this.xPos - MyActivity.canvas.dx;
    }

    @Override
    public double getyPosInRoom(){
        return this.yPos - MyActivity.canvas.dy;
    }

    @Override
    public double getxPosInScreen(){
        return this.xPos;
    }

    @Override
    public double getyPosInScreen(){
        return this.yPos;
    }

    public void setyPosInScreen(double yPos){
        this.yPos = yPos;
    }
    public void setxPosInScreen(double xPos){
        this.xPos = xPos;
    }

    @Override
    public void updatePosition() {
        if (MyActivity.currentMap != null) {
            managePositionRelativeToMap();
            this.healthBar.setxCenter((int) this.getxPosInScreen());
            int yDirection = (getyPosInScreen() < MyActivity.VERTICAL_MARGIN) ? -1 : 1;
            this.healthBar.setyCenter((int) (getyPosInScreen() + yDirection * getHeight()));
        } else {
            super.updatePosition();
        }
    }

    private void managePositionRelativeToMap() {
        MathVector futurePosition = getFuturePositionInScreen();
        if (getP().x > 0){
            if (futurePosition.x > MyActivity.screenWidth - MyActivity.HORIZONTAL_MARGIN && MyActivity.canvas.dx > - (MyActivity.currentMap.getWidth() - MyActivity.screenWidth)){
                MyActivity.canvas.dx -= getP().x;
            } else {
                this.xPos = (int) (getxPosInScreen() + getP().x);
            }
        } else if (getP().x < 0){
            if (futurePosition.x < MyActivity.HORIZONTAL_MARGIN && MyActivity.canvas.dx < 0){
                MyActivity.canvas.dx -= getP().x;
            } else {
                this.xPos = (int) (getxPosInScreen() + getP().x);
            }
        }
        if (getP().y > 0){
            if (futurePosition.y > MyActivity.screenHeight - MyActivity.VERTICAL_MARGIN && MyActivity.canvas.dy > - (MyActivity.currentMap.getHeight() - MyActivity.screenHeight)){
                MyActivity.canvas.dy -= getP().y;
            } else {
                this.yPos = (int) (getyPosInScreen() + getP().y);
            }
        } else if (getP().y < 0){
            if (futurePosition.y < MyActivity.VERTICAL_MARGIN && MyActivity.canvas.dy < 0) {
                MyActivity.canvas.dy -= getP().y;
            } else {
                this.yPos = (int) (getyPosInScreen() + getP().y);
            }
        }
        if (getyPosInScreen() > MyActivity.screenHeight + getHeight() || getxPosInScreen() < 0 || getxPosInScreen() > MyActivity.screenWidth) {
            manageExitMap();
        }
    }

    private void manageExitMap() {
        MyActivity.switchMap();
        for (PortalObject portal : getPortals()) {
            portal.destroy();
        }
        getPortals().clear();
        if (getCurrentPowerUp() == GamePowerUp.PORTAL){
            equipPowerUp(GamePowerUp.PORTAL);
        }
        getPortals().clear();
        if (isHooked()) {
            removeHook();
        }
        if (compass != null) {
            compass.clearInterests();
        }
    }

    @Override
    public void update(){
        if (getHook() != null){
            if (getHook().isHooked()){
                manageHookUpdate();
            }
            if (getHook().isFastReloading()) {
                if (distanceTo(rightHand) > BODY_RADIUS * 5) {
                    removeHook();
                }
            }
        }
        super.update();
        if (getP().x != 0f){
            setState(STATE_MOVING);
        } else {
            setState(STATE_REST);
        }
        manageFacingDirection();
        if (getHealth() > 0) {
            manageEnemyCollision();
        }
    }

    private void manageEnemyCollision() {
        for (GameEnemy enemy : MyActivity.enemies) {
            if (this.distanceTo(enemy) < enemy.getHurtDistance()) {
                this.getHurt(enemy.getDamageDone());
            }
        }
    }

    private void manageFacingDirection() {
        if (getP().x != 0f) {
            setFacing((int) Math.signum(getP().x));
        }
    }

    public void manageHookUpdate () {
        if (getHook().isFastReloading()) {
            setMaxVelocity(MAX_VELOCITY*5);
        } else {
            if (getCurrentPowerUp() == GamePowerUp.INFINITE_JUMPS) {
                setMaxVelocity(MAX_VELOCITY * 2);
            } else {
                setMaxVelocity(MAX_VELOCITY);
            }
        }
        if (getHook().isReloading()){
            MathVector vectorToLastNode = new MathVector(getPositionInRoom(),getHook().getLastNode().getPositionInRoom());
            setP(vectorToLastNode);
        } else {
            int maxSeparation = getWidth();
            if (distanceTo(getHook().getLastNode()) > maxSeparation) {
                MathVector v = new MathVector(getHook().getLastNode().getPositionInRoom(), getFuturePositionInRoom());
                v.rescale(maxSeparation);
                if (getHook().getNodesNumber() > 1) {
                    getHook().getLastNode().addP(getP());
                }
                MathVector newP = new MathVector(getPositionInRoom(), v.applyTo(getHook().getLastNode().getPositionInRoom()));
                setP(newP);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        MathVector separationHand;
        MathVector separationFoot;
        MathVector positionFromHands;
        MathVector vectorForEyes;
        MathVector axisForFeet;
        double separationToEye = BODY_RADIUS/5;
        if (isHooked()){
            separationHand = new MathVector(getFacing()*FIST_RADIUS/3,FIST_RADIUS/3);
            separationFoot = new MathVector(getFacing() * BODY_RADIUS / 3, BODY_RADIUS + FOOT_RADIUS / 3);
            axisForFeet = new MathVector(0,-1);
            if (!isOnFloor()){
                axisForFeet = new MathVector(getHook().getFirstNode().getPositionInRoom(), getPositionInRoom());
                axisForFeet.normalize();
                double angle = separationFoot.angleDeg(new MathVector(0, 1));
                separationFoot = axisForFeet.rotatedDeg(angle).rescaled(separationFoot.magnitude());
            } else {
                separationFoot.x += MainCharacterAnimation.getFootAnimatedMovingX(getFrame());
            }
            positionFromHands = getHook().getLastNode().getPositionInRoom();
            vectorForEyes = new MathVector(0,-1);
        } else {
            separationHand = new MathVector(getFacing()*BODY_RADIUS,FIST_RADIUS);
            separationFoot = new MathVector(getFacing()*BODY_RADIUS/2,BODY_RADIUS+FOOT_RADIUS/4);
            positionFromHands = getPositionInRoom();
            vectorForEyes = new MathVector(0,-1);
            if (isOnFloor()){
                if (isInState(STATE_REST)) {
                    separationHand.y += MainCharacterAnimation.getFistAnimatedY(getFrame());
                } else {
                    separationHand.x += MainCharacterAnimation.getFistAnimatedMovingX(getFrame());
                    separationFoot.x += MainCharacterAnimation.getFootAnimatedMovingX(getFrame());
                }
            } else {
                separationHand.y -= 2*getP().y;
            }
            axisForFeet = new MathVector(0,1);
        }
        // Right hand
        rightHand.setPositionInRoom(separationHand.applyTo(positionFromHands));
        if (getCurrentPowerUp() == GamePowerUp.PORTAL) {
            paint.setStrokeWidth(rightHand.getWidth()/4);
            int startAngle = (int) (180 * Math.sin(2 * Math.PI * getFrame() / TimeUtil.convertSecondToGameSecond(1)) + 25);
            DrawUtil.drawArc(canvas, paint, (float) rightHand.getPositionInScreen().x - rightHand.getRadius(), (float) rightHand.getPositionInScreen().y - rightHand.getRadius(), (float) rightHand.getPositionInScreen().x + rightHand.getRadius(), (float)(float) rightHand.getPositionInScreen().y + rightHand.getRadius(), Color.RED, startAngle, 180);
            DrawUtil.drawArc(canvas, paint, (float) rightHand.getPositionInScreen().x - rightHand.getRadius(), (float) rightHand.getPositionInScreen().y - rightHand.getRadius(), (float) rightHand.getPositionInScreen().x + rightHand.getRadius(), (float)(float) rightHand.getPositionInScreen().y + rightHand.getRadius(), Color.BLUE, startAngle + 180, 180);
        }
        if (getCurrentPowerUp() == GamePowerUp.BOMB) {
            rightHand.setRadius((int) GameMath.linealValue(0, 0, TimeUtil.convertSecondToGameSecond(0.2), FIST_RADIUS, getFrame() % TimeUtil.convertSecondToGameSecond(0.2)));
        } else {
            rightHand.setRadius(FIST_RADIUS);
        }
        rightHand.draw(canvas);
        // Right foot
        rightFoot.setPositionInRoom(separationFoot.applyTo(getPositionInRoom()));
        rightFoot.draw(canvas);
        // Body
        body.setPositionInRoom(getPositionInRoom());
        body.draw(canvas);
        // Eyes
        paint.setColor(Color.YELLOW);
        vectorForEyes.rotateDeg(-1 * getFacing() * 90);
        vectorForEyes.rescale(separationToEye);
        leftEye.setPositionInRoom(vectorForEyes.applyTo(getPositionInRoom()));
        leftEye.draw(canvas);
        vectorForEyes.reflect(new MathVector(0, 1));
        vectorForEyes.scale(2);
        rightEye.setPositionInRoom(vectorForEyes.applyTo(getPositionInRoom()));
        rightEye.draw(canvas);
        // Left hand
        separationHand.reflect(new MathVector(0,1));
        separationFoot.reflect(axisForFeet);
        leftHand.setPositionInRoom(separationHand.applyTo(positionFromHands));
        if (getCurrentPowerUp() == GamePowerUp.PORTAL && portals.size() % 2 == 0) {
            int startAngle = (int) (180 * Math.sin(2 * Math.PI * getFrame() / TimeUtil.convertSecondToGameSecond(1)) + 25);
            DrawUtil.drawArc(canvas, paint, (float) leftHand.getPositionInScreen().x - leftHand.getRadius(), (float) leftHand.getPositionInScreen().y - leftHand.getRadius(), (float) leftHand.getPositionInScreen().x + leftHand.getRadius(), (float)(float) leftHand.getPositionInScreen().y + leftHand.getRadius(), Color.RED, startAngle, 180);
            DrawUtil.drawArc(canvas, paint, (float) leftHand.getPositionInScreen().x - leftHand.getRadius(), (float) leftHand.getPositionInScreen().y - leftHand.getRadius(), (float) leftHand.getPositionInScreen().x + leftHand.getRadius(), (float)(float) leftHand.getPositionInScreen().y + leftHand.getRadius(), Color.BLUE, startAngle + 180, 180);
        }
        if (getCurrentPowerUp() == GamePowerUp.BOMB) {
            leftHand.setRadius((int) GameMath.linealValue(0, 0, TimeUtil.convertSecondToGameSecond(0.2), FIST_RADIUS, getFrame() % TimeUtil.convertSecondToGameSecond(0.2)));
        } else {
            leftHand.setRadius(FIST_RADIUS);
        }
        leftHand.draw(canvas);
        // Left foot
        leftFoot.setPositionInRoom(separationFoot.applyTo(getPositionInRoom()));
        leftFoot.draw(canvas);
        // Bomb explosions left
        if (getCurrentPowerUp() == GamePowerUp.BOMB) {
            double[] angles = {0, 30, -30, 60, -60};
            MathVector v = new MathVector(0, -getHeight() * 3 / 4);
            Paint rPaint = new Paint();
            for (int i = 0; i < MAX_EXPLOSIONS - (explosionsUsed % MAX_EXPLOSIONS); i++) {
                MathVector p = v.rotatedDeg(angles[i]).applyTo(getPositionInScreen());
                DrawUtil.drawRadialGradient(canvas, rPaint, (float) p.x, (float) p.y, FIST_RADIUS, Color.YELLOW, Color.RED, Shader.TileMode.MIRROR);
            }
        }
    }

    public void jump(double jump) {
        MathVector jumpForce = new MathVector(0, jump);
        addP(jumpForce);
    }

    @Override
    public int getWidth() {
        return BODY_RADIUS*2;
    }

    @Override
    public int getHeight() {
        return (int) Math.abs(rightFoot.getPositionInRoom().y + rightFoot.getRadius() - (getPositionInRoom().y - BODY_RADIUS));
    }

    @Override
    public GameShape getBounds () {
        return new CircleShape(getxPosInRoom(), getyPosInRoom(), getWidth() / 2, false);
    }

    @Override
    public GameShape getFutureBounds () {
        return new CircleShape(getFuturePositionInRoom().x, getFuturePositionInRoom().y, getWidth() / 2, false);
    }

    public double getHookVelocity() {
        return hookVelocity;
    }

    public void setHookVelocity(double hookVelocity) {
        this.hookVelocity = hookVelocity;
    }

    public Hook getHook() {
        return hook;
    }

    public void setHook(Hook hook) {
        this.hook = hook;
    }

    public int getExplosionsUsed() {
        return explosionsUsed;
    }

    public void setExplosionsUsed(int explosionsUsed) {
        this.explosionsUsed = explosionsUsed;
    }

    public boolean isHooked(){
        if (getHook() != null){
            return getHook().isHooked();
        } else {
            return false;
        }
    }

    public int getMaxHookNodes() {
        return maxHookNodes;
    }

    public void setMaxHookNodes(int maxHookNodes) {
        this.maxHookNodes = maxHookNodes;
    }

    public int getFacing() {
        return facing;
    }

    public void setFacing(int facing) {
        this.facing = facing;
    }

    public boolean isFacingLeft() {
        return getFacing() == -1;
    }

    public boolean isFacingRight() {
        return getFacing() == 1;
    }

    public CompassObject getCompass() {
        return compass;
    }

    public void setCompass(CompassObject compass) {
        this.compass = compass;
    }

    public LinkedList<PortalObject> getPortals() {
        return portals;
    }

    public TimerObject getInfiniteJumpsTimer() {
        return infiniteJumpsTimer;
    }

    public void setInfiniteJumpsTimer(TimerObject infiniteJumpsTimer) {
        this.infiniteJumpsTimer = infiniteJumpsTimer;
    }

    @Override
    public int getMargin(){
        return 1;
    }

    public void checkIfRemoveInterest(GameObject interest) {
        if (getCompass() != null) {
            getCompass().removeInterest(interest);
        }
    }

    public void shootHook(double xDown, double yDown) {
        if (getHook() != null) {
            removeHook();
        }
        MathVector downPoint = new MathVector(xDown,yDown);
        MathVector initP = new MathVector(getPositionInScreen(),downPoint);
        int nNodes = (int) (initP.magnitude()/Hook.SEPARATION) + 2;
        nNodes = Math.max(nNodes + 1,MIN_HOOSKSHOT_NODES);
        setHook(new Hook(getxPosInRoom(), getyPosInRoom(), nNodes, Color.GRAY, this, new MathVector(0, 0)));
        getHook().hook(downPoint.screenToRoom());
        MyActivity.canvas.debugText = getP().toString();
    }

    public void removeHook() {
        if (getHook().isFastReloading()) {
            setMass(MASS);
        }
        MyActivity.canvas.gameObjects.remove(hook);
        MyActivity.dynamicObjects.removeAll(hook.getNodes());
        if (hook.getHookedObject() != null){
            hook.getHookedObject().setMass(getHook().getPrevHookedMass());
        }
        hook.getNodes().clear();
        setHook(null);
        MyActivity.hudElements.remove(MyActivity.reloadButton);
        MyActivity.hudElements.remove(MyActivity.extendButton);
        MyActivity.reloadButton = null;
        MyActivity.extendButton = null;
        if (getCurrentPowerUp() == GamePowerUp.INFINITE_JUMPS) {
            setMaxVelocity(MAX_VELOCITY * 2);
        } else {
            setMaxVelocity(MAX_VELOCITY);
        }
        setState(STATE_MOVING);
    }

    @Override
    public void die() {
        if (isHooked()) {
            removeHook();
        }
        MyActivity.hideControls();
        MyActivity.paused = true;
        MyActivity.gameEffects.add(new FadeEffect(Color.WHITE, new Execution() {
            @Override
            public double execute() {
                setHealth(getMaxHealth());
                MyActivity.canvas.myActivity.saveHealth();
                (new MainMenu()).execute();
                return 0;
            }
        }));
        this.destroy();
    }

    @Override
    public void getHurt(int damage) {
        super.getHurt(damage);
        this.manageHealthBar();
    }

    private void manageHealthBar() {
        if (getHealth() < getMaxHealth()/5) {
            this.healthBar.setColor(Color.RED);
        } else if (getHealth() < getMaxHealth() / 2) {
            this.healthBar.setColor(Color.YELLOW);
        } else {
            this.healthBar.setColor(Color.GREEN);
        }
        if (this.healthBar.getAlpha() == 0) {
            this.healthBar.setAlpha(1);
        }
        if (!MyActivity.hudElements.contains(this.healthBar)){
            MyActivity.hudElements.add(this.healthBar);
        }
    }

    public void setPowerUp (int type, int quantity) {
        powerUps.put(type, quantity);
    }

    public void addPowerUp(int type) {
        if (powerUps.containsKey(type)) {
            powerUps.put(type, powerUps.get(type) + 1);
        } else {
            powerUps.put(type, 1);
        }
    }

    public void equipPowerUp(int type) {
        setCurrentPowerUp(type);
        setMaxVelocity(MAX_VELOCITY);
        switch (type) {
            case GamePowerUp.PORTAL:
                if (portals.size() % 2 == 1){
                    setColors(Color.MAGENTA, Color.YELLOW, Color.BLACK, Color.WHITE, Color.RED, Color.RED);
                } else {
                    setColors(Color.MAGENTA, Color.YELLOW, Color.BLACK, Color.BLACK, Color.RED, Color.RED);
                }
                if (getInfiniteJumpsTimer() != null) {
                    getInfiniteJumpsTimer().destroy();
                    setInfiniteJumpsTimer(null);
                }
                break;
            case GamePowerUp.COMPASS:
                usePowerUp();
                break;
            case GamePowerUp.BOMB:
                setColors(Color.RED, Color.CYAN, Color.rgb(255, 255, 0), Color.rgb(255, 255, 0), Color.BLACK, Color.BLACK);
                if (getInfiniteJumpsTimer() != null) {
                    getInfiniteJumpsTimer().destroy();
                    setInfiniteJumpsTimer(null);
                }
                break;
            case GamePowerUp.INFINITE_JUMPS:
                setMaxVelocity(MAX_VELOCITY * 2);
                setColors(Color.CYAN, Color.BLACK, Color.WHITE, Color.WHITE, Color.BLUE, Color.BLUE);
                powerUps.put(GamePowerUp.INFINITE_JUMPS, powerUps.get(GamePowerUp.INFINITE_JUMPS) - 1);
                setInfiniteJumpsTimer(new TimerObject(this, (int) (getWidth()*2/1.5),TimeUtil.convertSecondToGameSecond(5),Color.BLUE,true,true, new Execution() {
                    @Override
                    public double execute() {
                        setMaxVelocity(MAX_VELOCITY);
                        equipPowerUp(-1);
                        setInfiniteJumpsTimer(null);
                        return 0;
                    }
                }));
                break;
            default:
                setColors(Color.BLACK, Color.YELLOW, Color.WHITE, Color.WHITE, Color.RED, Color.RED);
        }
    }

    public void setColors(int bodyColor, int eyesColor, int rightHandColor, int leftHandColor, int rightFootColor, int leftFootColor) {
        body.setColor(bodyColor);
        leftEye.setColor(eyesColor);
        rightEye.setColor(eyesColor);
        leftHand.setColor(leftHandColor);
        rightHand.setColor(rightHandColor);
        leftFoot.setColor(leftFootColor);
        rightFoot.setColor(rightFootColor);
    }

    public void usePowerUp() {
        switch (getCurrentPowerUp()) {
            case GamePowerUp.PORTAL:
                PortalObject portal = new PortalObject(getxPosInRoom(), getyPosInRoom(), getxPosInScreen(), getyPosInScreen(), MyActivity.canvas.dx, MyActivity.canvas.dy, (int) (BODY_RADIUS * 2.5));
                portals.add(portal);
                leftHand.setColor(Color.WHITE);
                if (portals.size() % 2 == 0) {
                    portals.get(portals.size() - 2).setTwinPortal(portals.get(portals.size() - 1));
                    portals.get(portals.size() - 1).setTwinPortal(portals.get(portals.size() - 2));
                    equipPowerUp(-1);
                    powerUps.put(GamePowerUp.PORTAL, powerUps.get(GamePowerUp.PORTAL) - 1);
                }
                break;
            case GamePowerUp.COMPASS:
                setCompass(new CompassObject(this, true, true));
                if (prevPowerUp == GamePowerUp.INFINITE_JUMPS) {
                    setMaxVelocity(MAX_VELOCITY * 2);
                }
                setCurrentPowerUp(prevPowerUp);
                powerUps.put(GamePowerUp.COMPASS, powerUps.get(GamePowerUp.COMPASS) - 1);
                break;
            case GamePowerUp.BOMB:
                new ExplosionObject(getxPosInRoom(), getyPosInRoom(), MyActivity.TILE_WIDTH, true, true);
                explosionsUsed += 1;
                if (explosionsUsed % MAX_EXPLOSIONS == 0) {
                    equipPowerUp(-1);
                    powerUps.put(GamePowerUp.BOMB, powerUps.get(GamePowerUp.BOMB) - 1);
                }
                break;
            case GamePowerUp.INFINITE_JUMPS:
                jump( -1 * MyActivity.TILE_WIDTH * 2);
                new JumpEffect(getxPosInRoom(), getyPosInRoom() + getHeight() / 2, MyActivity.TILE_WIDTH, (int) (MyActivity.TILE_WIDTH * 0.25), true, true);
        }
    }

    public HashMap<Integer, Integer> getPowerUps() {
        return powerUps;
    }

    public int getCurrentPowerUp() {
        return currentPowerUp;
    }

    public void setCurrentPowerUp(int currentPowerUp) {
        prevPowerUp = getCurrentPowerUp();
        this.currentPowerUp = currentPowerUp;
    }
}
