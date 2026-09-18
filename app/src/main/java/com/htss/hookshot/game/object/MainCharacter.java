package com.htss.hookshot.game.object;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.htss.hookshot.effect.FadeEffect;
import com.htss.hookshot.effect.HurtEffect;
import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.executions.MainMenu;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.animation.MainCharacterAnimation;
import com.htss.hookshot.game.hud.HUDBar;
import com.htss.hookshot.game.hud.Joystick;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.game.object.miscellaneous.BurstArt;
import com.htss.hookshot.game.object.miscellaneous.CompassObject;
import com.htss.hookshot.game.object.miscellaneous.ExplosionObject;
import com.htss.hookshot.game.object.miscellaneous.JumpEffect;
import com.htss.hookshot.game.object.miscellaneous.PortalArt;
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

    // Speed tuned on 720 px tall screens, where TILE_WIDTH is 100
    public static final int MAX_HEALTH = 100, MAX_VELOCITY = 15 * MyActivity.TILE_WIDTH / 100;
    private static final int MAX_EXPLOSIONS = 5;
    private static final int MASS = 1, COLLISION_PRIORITY = 5;
    // The swiftness timer around it, cyan like the jump rings
    private static final int SWIFTNESS_TIMER = Color.rgb(43, 184, 214);
    // How much speed is kept every update on the ground
    private static final double GROUND_FRICTION = 0.75;
    // Swinging on the chain, and flying once let go of it, can be this many times faster than walking
    private static final double SWING_SPEED = 2;
    // How far up the joystick is pushed to jump, as a share of its reach. About 12 degrees above level
    private static final double JUMP_PUSH = 0.2;
    // The character falls faster than other objects, so jumps rise and fall quickly: 0.7 tiles high in a third of a
    // second, and infinite jumps, with twice the top speed, 2.7 tiles high
    private static final double GRAVITY_SCALE = 5 / 3.0;
    // How hard the joystick pushes a swing, as a share of gravity. Pushing the way it swings builds it up
    private static final double SWING_PUSH = 0.5;
    // As long as the red flash of the HurtEffect
    private static final double INVULNERABLE_DURATION = TimeUtil.secondsToUpdates(0.833);
    // Falling faster than this kicks up dust on landing
    private static final double LANDING_DUST_SPEED = MyActivity.TILE_WIDTH * 0.08;
    // Landing this fast squashes the most, and every update undoes part of the squash or stretch
    private static final double MAX_SQUASH_SPEED = MyActivity.TILE_WIDTH * 0.25;
    private static final float TAKEOFF_STRETCH = -0.6f, SQUASH_DECAY = 0.85f;
    private static final int BLINK_PERIOD = (int) TimeUtil.secondsToUpdates(3.5), BLINK_UPDATES = (int) TimeUtil.secondsToUpdates(0.117);
    // The body's highlight, mixed with its colour, and the dark line around hands and feet
    private static final int BODY_LIGHT = Color.rgb(150, 160, 190), LIMB_OUTLINE = Color.argb(200, 20, 20, 28);

    public static final int BODY_RADIUS = 10*MyActivity.TILE_WIDTH /50, FIST_RADIUS = MyActivity.TILE_WIDTH /8,
                            FOOT_RADIUS = 10*MyActivity.TILE_WIDTH /100, EYE_RADIUS = MyActivity.TILE_WIDTH /25,
                            MIN_HOOSKSHOT_NODES = 3;
    // How far above its body the character holds the chain
    private static final int GRIP = BODY_RADIUS * 2;

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
    private int explosionsUsed = 0, coins = 0;
    private TimerObject infiniteJumpsTimer;
    private double invulnerableUntilFrame = 0;
    // Above 0 squashed, below 0 stretched
    private float squash = 0;
    // Let go of the chain in the air, until landing or hooking again
    private boolean flying = false;
    // The explosions left with the bomb power-up, shown above its head like the explosions
    private final BurstArt explosionsLeftArt = new BurstArt();
    private Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG), outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RadialGradient bodyGradient;
    private int bodyGradientColor;
    private Matrix bodyMatrix = new Matrix();

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
        // Slows down being thrown by the chain once on the ground. Walking sets the speed on every update anyway. In the
        // main menu, nothing slows it down, so the character walking along the bottom keeps going
        friction = (MyActivity.currentMap == null) ? 1 : GROUND_FRICTION;
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
                this.xPos = getxPosInScreen() + getP().x;
            }
        } else if (getP().x < 0){
            if (futurePosition.x < MyActivity.HORIZONTAL_MARGIN && MyActivity.canvas.dx < 0){
                MyActivity.canvas.dx -= getP().x;
            } else {
                this.xPos = getxPosInScreen() + getP().x;
            }
        }
        if (getP().y > 0){
            if (futurePosition.y > MyActivity.screenHeight - MyActivity.VERTICAL_MARGIN && MyActivity.canvas.dy > - (MyActivity.currentMap.getHeight() - MyActivity.screenHeight)){
                MyActivity.canvas.dy -= getP().y;
            } else {
                this.yPos = getyPosInScreen() + getP().y;
            }
        } else if (getP().y < 0){
            if (futurePosition.y < MyActivity.VERTICAL_MARGIN && MyActivity.canvas.dy < 0) {
                MyActivity.canvas.dy -= getP().y;
            } else {
                this.yPos = getyPosInScreen() + getP().y;
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
        boolean wasOnFloor = isOnFloor();
        double fallSpeed = getP().y;
        if (MyActivity.currentMap == null && isHooked()) {
            // Hanging in the main menu, where there's no gravity, it only moves when dragged
            setP(new MathVector(0, 0));
        }
        steer();
        if (isHooked()) {
            setMaxVelocity(getHook().isFastReloading() ? MAX_VELOCITY * 5 : getWalkingSpeed());
        }
        super.update();
        if (isHooked()) {
            getHook().updateChain(getPositionInRoom(), getHandsPosition());
        }
        if (isOnFloor()) {
            flying = false;
        }
        if (!wasOnFloor && isOnFloor() && fallSpeed > LANDING_DUST_SPEED) {
            kickUpDust();
            squash = (float) Math.min(1, fallSpeed / MAX_SQUASH_SPEED);
        } else if (wasOnFloor && !isOnFloor() && getP().y < -LANDING_DUST_SPEED) {
            squash = TAKEOFF_STRETCH;
        }
        squash *= SQUASH_DECAY;
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

    // The joystick sets the walking speed, also while jumping, and jumps when pushed up. Swinging, it pushes the swing
    // instead. Thrown by the chain, it steers, but pushing the way the character already goes faster doesn't slow it
    // down, and letting go keeps it going. With the chain reeled in all the way, it climbs around where it's hooked
    private void steer() {
        Joystick joystick = MyActivity.joystick;
        if (!joystick.isOn() || MyActivity.currentMap == null) {
            return;
        }
        double x = joystick.getSteerX() * getWalkingSpeed();
        if (isHooked() && getHook().getNodesNumber() <= MIN_HOOSKSHOT_NODES) {
            if (joystick.getSteerX() != 0 || joystick.getSteerY() != 0) {
                setP(new MathVector(x, joystick.getSteerY() * getWalkingSpeed()));
            }
        } else if (isSwinging()) {
            p.x += joystick.getSteerX() * getGravity() * SWING_PUSH;
        } else if (flying) {
            if (x != 0 && !(Math.signum(x) == Math.signum(p.x) && Math.abs(p.x) > Math.abs(x))) {
                p.x = x;
            }
        } else {
            p.x = x;
            if (isOnFloor() && joystick.getPushY() < -JUMP_PUSH) {
                jump(-MyActivity.TILE_WIDTH);
            }
        }
    }

    // Letting go of the joystick stops walking and jumping sideways, but not swinging or being thrown by the chain
    public void releaseJoystick() {
        if (!flying && !isSwinging()) {
            setP(new MathVector(0, getP().y));
        }
    }

    // Hanging from the chain in the air, and not climbing around where it's hooked
    private boolean isSwinging() {
        return isHooked() && !isOnFloor() && getHook().getNodesNumber() > MIN_HOOSKSHOT_NODES;
    }

    @Override
    protected double getGravity() {
        return super.getGravity() * GRAVITY_SCALE;
    }

    // On the chain, and flying once let go of it, the character can go faster than walking, in any direction. After a
    // double tap, the top speed is higher still
    @Override
    protected void limitSpeed() {
        boolean onChain = isHooked() && !getHook().isFastReloading();
        if (onChain || flying) {
            double limit = getWalkingSpeed() * SWING_SPEED;
            if (getP().magnitude() > limit) {
                setP(getP().rescaled(limit));
            }
        } else {
            super.limitSpeed();
        }
    }

    private double getWalkingSpeed() {
        return (getCurrentPowerUp() == GamePowerUp.INFINITE_JUMPS) ? MAX_VELOCITY * 2 : MAX_VELOCITY;
    }

    private void kickUpDust() {
        double x = getxPosInRoom(), y = getyPosInRoom() + getHeight() / 2;
        int dust = DrawUtil.withAlpha(CavePalette.forLevel(MyActivity.canvas.myActivity.level).dust, 140);
        Particles.burst(x, y, 5, dust, 0.05f, 0.035f, 0.5, -0.0005f, 170, 200);
        Particles.burst(x, y, 5, dust, 0.05f, 0.035f, 0.5, -0.0005f, -20, 10);
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

    // Right before moving, the chain holds the character: it can't get further from what it swings around than the
    // chain between them. Only the speed away from it is lost, so swings keep going, and reeling the chain in pulls
    // the character. While the extend button is held, the chain is let out instead, as far as it goes
    @Override
    public void manageConstraints() {
        super.manageConstraints();
        if (!isHooked()) {
            return;
        }
        MathVector pivot = getHook().getPivotNode().getPositionInRoom();
        MathVector fromPivot = new MathVector(pivot, getFuturePositionInRoom());
        double distance = fromPivot.magnitude(), reach = getHook().getLengthToPivot() + GRIP;
        if (distance > reach && getHook().isExtending()) {
            getHook().letOut(distance - reach);
            reach = getHook().getLengthToPivot() + GRIP;
        }
        if (distance > reach) {
            setP(new MathVector(getPositionInRoom(), fromPivot.scaled(reach / distance).applyTo(pivot)));
        }
    }

    // Holding the chain above its head, towards what it swings around
    private MathVector getHandsPosition() {
        MathVector toPivot = new MathVector(getPositionInRoom(), getHook().getPivotNode().getPositionInRoom());
        if (toPivot.isNull()) {
            return getPositionInRoom();
        }
        return toPivot.rescaled(Math.min(GRIP, toPivot.magnitude())).applyTo(getPositionInRoom());
    }

    @Override
    public void draw(Canvas canvas) {
        // Squashed on landing and stretched on takeoff, around the feet
        canvas.save();
        float pivotY = (float) getyPosInScreen() + BODY_RADIUS + FOOT_RADIUS;
        canvas.scale(1 + 0.3f * squash, 1 - 0.25f * squash, (float) getxPosInScreen(), pivotY);
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
                axisForFeet = new MathVector(getHook().getPivotNode().getPositionInRoom(), getPositionInRoom());
                axisForFeet.normalize();
                double angle = separationFoot.angleDeg(new MathVector(0, 1));
                separationFoot = axisForFeet.rotatedDeg(angle).rescaled(separationFoot.magnitude());
            } else {
                separationFoot.x += MainCharacterAnimation.getFootAnimatedMovingX(getFrame());
            }
            positionFromHands = getHook().getGripNode().getPositionInRoom();
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
            int startAngle = (int) (180 * Math.sin(2 * Math.PI * getFrame() / TimeUtil.secondsToUpdates(1.667)) + 25);
            DrawUtil.drawArc(canvas, paint, (float) rightHand.getPositionInScreen().x - rightHand.getRadius(), (float) rightHand.getPositionInScreen().y - rightHand.getRadius(), (float) rightHand.getPositionInScreen().x + rightHand.getRadius(), (float)(float) rightHand.getPositionInScreen().y + rightHand.getRadius(), PortalArt.RED, startAngle, 180);
            DrawUtil.drawArc(canvas, paint, (float) rightHand.getPositionInScreen().x - rightHand.getRadius(), (float) rightHand.getPositionInScreen().y - rightHand.getRadius(), (float) rightHand.getPositionInScreen().x + rightHand.getRadius(), (float)(float) rightHand.getPositionInScreen().y + rightHand.getRadius(), PortalArt.BLUE, startAngle + 180, 180);
        }
        if (getCurrentPowerUp() == GamePowerUp.BOMB) {
            rightHand.setRadius((int) GameMath.linealValue(0, 0, TimeUtil.secondsToUpdates(0.333), FIST_RADIUS, getFrame() % TimeUtil.secondsToUpdates(0.333)));
        } else {
            rightHand.setRadius(FIST_RADIUS);
        }
        drawLimb(canvas, rightHand);
        // Right foot
        rightFoot.setPositionInRoom(separationFoot.applyTo(getPositionInRoom()));
        drawLimb(canvas, rightFoot);
        // Body
        body.setPositionInRoom(getPositionInRoom());
        drawBody(canvas);
        // Eyes
        paint.setColor(Color.YELLOW);
        vectorForEyes.rotateDeg(-1 * getFacing() * 90);
        vectorForEyes.rescale(separationToEye);
        leftEye.setPositionInRoom(vectorForEyes.applyTo(getPositionInRoom()));
        drawEye(canvas, leftEye);
        vectorForEyes.reflect(new MathVector(0, 1));
        vectorForEyes.scale(2);
        rightEye.setPositionInRoom(vectorForEyes.applyTo(getPositionInRoom()));
        drawEye(canvas, rightEye);
        // Left hand
        separationHand.reflect(new MathVector(0,1));
        separationFoot.reflect(axisForFeet);
        leftHand.setPositionInRoom(separationHand.applyTo(positionFromHands));
        if (getCurrentPowerUp() == GamePowerUp.PORTAL && portals.size() % 2 == 0) {
            int startAngle = (int) (180 * Math.sin(2 * Math.PI * getFrame() / TimeUtil.secondsToUpdates(1.667)) + 25);
            DrawUtil.drawArc(canvas, paint, (float) leftHand.getPositionInScreen().x - leftHand.getRadius(), (float) leftHand.getPositionInScreen().y - leftHand.getRadius(), (float) leftHand.getPositionInScreen().x + leftHand.getRadius(), (float)(float) leftHand.getPositionInScreen().y + leftHand.getRadius(), PortalArt.RED, startAngle, 180);
            DrawUtil.drawArc(canvas, paint, (float) leftHand.getPositionInScreen().x - leftHand.getRadius(), (float) leftHand.getPositionInScreen().y - leftHand.getRadius(), (float) leftHand.getPositionInScreen().x + leftHand.getRadius(), (float)(float) leftHand.getPositionInScreen().y + leftHand.getRadius(), PortalArt.BLUE, startAngle + 180, 180);
        }
        if (getCurrentPowerUp() == GamePowerUp.BOMB) {
            leftHand.setRadius((int) GameMath.linealValue(0, 0, TimeUtil.secondsToUpdates(0.333), FIST_RADIUS, getFrame() % TimeUtil.secondsToUpdates(0.333)));
        } else {
            leftHand.setRadius(FIST_RADIUS);
        }
        drawLimb(canvas, leftHand);
        // Left foot
        leftFoot.setPositionInRoom(separationFoot.applyTo(getPositionInRoom()));
        drawLimb(canvas, leftFoot);
        // Bomb explosions left
        if (getCurrentPowerUp() == GamePowerUp.BOMB) {
            double[] angles = {0, 30, -30, 60, -60};
            MathVector v = new MathVector(0, -getHeight() * 3 / 4);
            for (int i = 0; i < MAX_EXPLOSIONS - (explosionsUsed % MAX_EXPLOSIONS); i++) {
                MathVector p = v.rotatedDeg(angles[i]).applyTo(getPositionInScreen());
                explosionsLeftArt.draw(canvas, (float) p.x, (float) p.y, FIST_RADIUS * 1.1f, getFrame() + i * 7, 0);
            }
        }
        canvas.restore();
    }

    private void drawBody(Canvas canvas) {
        MathVector center = body.getPositionInScreen();
        if (bodyGradient == null || bodyGradientColor != body.getColor()) {
            bodyGradientColor = body.getColor();
            bodyGradient = new RadialGradient(0, 0, BODY_RADIUS * 1.6f, DrawUtil.blend(bodyGradientColor, BODY_LIGHT, 0.45f),
                    DrawUtil.blend(bodyGradientColor, Color.BLACK, 0.55f), Shader.TileMode.CLAMP);
            bodyPaint.setShader(bodyGradient);
        }
        // Lit from the top left
        bodyMatrix.setTranslate((float) center.x - BODY_RADIUS * 0.4f, (float) center.y - BODY_RADIUS * 0.4f);
        bodyGradient.setLocalMatrix(bodyMatrix);
        canvas.drawCircle((float) center.x, (float) center.y, BODY_RADIUS, bodyPaint);
        // A light rim, so it stands out in the dark caves
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(BODY_RADIUS / 7f);
        outlinePaint.setColor(DrawUtil.withAlpha(CavePalette.forLevel(MyActivity.canvas.myActivity.level).dust, 170));
        canvas.drawCircle((float) center.x, (float) center.y, BODY_RADIUS, outlinePaint);
    }

    private void drawEye(Canvas canvas, BiCircleShape eye) {
        MathVector center = eye.getPositionInScreen();
        if (getFrame() % BLINK_PERIOD < BLINK_UPDATES) {
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(EYE_RADIUS * 0.6f);
            outlinePaint.setStrokeCap(Paint.Cap.ROUND);
            outlinePaint.setColor(eye.getColor());
            canvas.drawLine((float) center.x - EYE_RADIUS, (float) center.y, (float) center.x + EYE_RADIUS, (float) center.y, outlinePaint);
        } else {
            eye.draw(canvas);
            // A glint on the upper part
            outlinePaint.setStyle(Paint.Style.FILL);
            outlinePaint.setColor(Color.WHITE);
            canvas.drawCircle((float) eye.getCenter2().x, (float) eye.getCenter2().y, EYE_RADIUS * 0.4f, outlinePaint);
        }
    }

    private void drawLimb(Canvas canvas, CircleShape limb) {
        limb.draw(canvas);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(limb.getRadius() / 4f);
        outlinePaint.setColor(LIMB_OUTLINE);
        canvas.drawCircle((float) limb.getxPosInScreen(), (float) limb.getyPosInScreen(), limb.getRadius(), outlinePaint);
    }

    // Pushes the character up, never faster than its top speed, even while swinging, when it can go faster
    public void jump(double push) {
        p.y = Math.max(p.y + push, -getMaxVelocity());
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

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public void addCoins(int coins) {
        this.coins += coins;
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
        setHook(new Hook(getxPosInRoom(), getyPosInRoom(), nNodes, Color.GRAY));
        getHook().hook(downPoint.screenToRoom());
        flying = false;
        // Sparks where the hook bites
        MathVector bite = downPoint.screenToRoom();
        Particles.burst(bite.x, bite.y, 6, Color.rgb(230, 230, 210), 0.05f, 0.02f, 0.25, 0.002f);
    }

    public void removeHook() {
        // Holding the extend button makes the character heavier, and the button goes with the chain, so letting go of
        // it would never make the character light again
        setMass(MASS);
        MyActivity.canvas.gameObjects.remove(hook);
        MyActivity.dynamicObjects.removeAll(hook.getNodes());
        if (hook.getHookedObject() != null){
            hook.getHookedObject().setMass(getHook().getPrevHookedMass());
        }
        hook.getNodes().clear();
        setHook(null);
        MyActivity.hudElements.remove(MyActivity.extendButton);
        MyActivity.extendButton = null;
        setMaxVelocity(getWalkingSpeed());
        flying = !isOnFloor();
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
    public void addHealth(double health) {
        super.addHealth(health);
        this.manageHealthBar();
    }

    @Override
    public void getHurt(int damage) {
        // Enemy contact is checked every update, so without this window it hits 60 times a second
        if (getFrame() < invulnerableUntilFrame) {
            return;
        }
        invulnerableUntilFrame = getFrame() + INVULNERABLE_DURATION;
        MyActivity.gameEffects.add(new HurtEffect());
        ScreenShake.shake(0.06f);
        Particles.burst(getxPosInRoom(), getyPosInRoom(), 8, Color.rgb(220, 30, 30), 0.07f, 0.03f, 0.4, 0.003f);
        super.getHurt(damage);
        this.manageHealthBar();
    }

    private void manageHealthBar() {
        this.healthBar.setColor(HUDBar.getHealthColor(getHealth() / getMaxHealth()));
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
                setInfiniteJumpsTimer(new TimerObject(this, (int) (getWidth()*2/1.5),TimeUtil.secondsToUpdates(8.333),SWIFTNESS_TIMER,true,true, new Execution() {
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
