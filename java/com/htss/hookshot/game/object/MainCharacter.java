package com.htss.hookshot.game.object;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.animation.MainCharacterAnimation;
import com.htss.hookshot.game.hud.HUDBar;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.game.object.shapes.BiCircleShape;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 03/08/2016.
 */
public class MainCharacter extends GameCharacter {

    private static final int MAX_HEALTH = 100, MAX_VELOCITY = 15;

    public static final int BODY_RADIUS = 10*MyActivity.TILE_WIDTH /50, FIST_RADIUS = MyActivity.TILE_WIDTH /8,
                            FOOT_RADIUS = 10*MyActivity.TILE_WIDTH /100, EYE_RADIUS = MyActivity.TILE_WIDTH /25,
                            MIN_HOOSKSHOT_NODES = 3;


    private double hookVelocity = 300;
    private int maxHookNodes = 50, facing = 1;
    private Hook hook = null;
    private CircleShape rightHand, leftHand, rightFoot, leftFoot;
    private BiCircleShape leftEye, rightEye;
    private HUDBar healthBar;

    public MainCharacter(double xPos, double yPos, int mass, int collisionPriority) {
        super(xPos, yPos, mass, collisionPriority, MAX_VELOCITY, MAX_HEALTH);
        makeSureNotUnderground = true;
        rightHand = new CircleShape(xPos,yPos,FIST_RADIUS,Color.WHITE);
        leftHand = new CircleShape(xPos,yPos,FIST_RADIUS,Color.WHITE);
        leftFoot = new CircleShape(xPos,yPos,FOOT_RADIUS,Color.RED);
        rightFoot = new CircleShape(xPos,yPos,FOOT_RADIUS,Color.RED);
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
        if (getyPosInScreen() > MyActivity.screenHeight + getHeight()) {
            MyActivity.switchMap(1);
            if (isHooked()) {
                removeHook();
            }
        }
        this.healthBar.setxCenter((int) this.getxPosInScreen());
        int yDirection = (getyPosInScreen() < MyActivity.VERTICAL_MARGIN) ? -1 : 1;
        this.healthBar.setyCenter((int) (getyPosInScreen() + yDirection*getHeight()));
    }

    @Override
    public void update(){
        if (getHook() != null){
            if (getHook().isHooked()){
                manageHookUpdate();
            }
        }
        super.update();
        if (getP().x != 0f){
            setState(STATE_MOVING);
        } else {
            setState(STATE_REST);
        }
        manageFacingDirection();
        manageEnemyCollision();
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
        if (getHook().isReloading()){
            MathVector vectorToLastNode = new MathVector(getPositionInRoom(),getHook().getLastNode().getPositionInRoom());
            setP(vectorToLastNode);
        } else {
            int maxSeparation = getWidth();
            if (distanceTo(getHook().getLastNode()) > maxSeparation) {
                MathVector v = new MathVector(getHook().getLastNode().getPositionInRoom(), getFuturePositionInRoom());
                v.rescale(maxSeparation);
                getHook().getLastNode().addP(getP());
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
                axisForFeet = new MathVector(getHook().getLastNode().getPositionInRoom(), getPositionInRoom());
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
        rightHand.setPositionInRoom(separationHand.applyTo(positionFromHands));
        rightHand.draw(canvas);
        rightFoot.setPositionInRoom(separationFoot.applyTo(getPositionInRoom()));
        rightFoot.draw(canvas);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawCircle((int) getxPosInScreen(), (int) getyPosInScreen(), BODY_RADIUS, paint);
        paint.setColor(Color.YELLOW);
        vectorForEyes.rotateDeg(-1 * getFacing() * 90);
        vectorForEyes.rescale(separationToEye);
        leftEye.setPositionInRoom(vectorForEyes.applyTo(getPositionInRoom()));
        leftEye.draw(canvas);
        vectorForEyes.reflect(new MathVector(0, 1));
        vectorForEyes.scale(2);
        rightEye.setPositionInRoom(vectorForEyes.applyTo(getPositionInRoom()));
        rightEye.draw(canvas);
        separationHand.reflect(new MathVector(0,1));
        separationFoot.reflect(axisForFeet);
        leftHand.setPositionInRoom(separationHand.applyTo(positionFromHands));
        leftHand.draw(canvas);
        leftFoot.setPositionInRoom(separationFoot.applyTo(getPositionInRoom()));
        leftFoot.draw(canvas);
    }

    @Override
    public int getWidth() {
        return BODY_RADIUS*2;
//        return (int) Math.abs(rightHand.getPositionInRoom().x - leftHand.getPositionInRoom().x) + rightHand.getRadius() + leftHand.getRadius();
    }

    @Override
    public int getHeight() {
        return (int) Math.abs(rightFoot.getPositionInRoom().y + rightFoot.getRadius() - (getPositionInRoom().y - BODY_RADIUS));
    }

    @Override
    public GameShape getBounds (){
        return new CircleShape(getxPosInRoom(),getyPosInRoom(),getWidth()/2);
    }

    @Override
    public GameShape getFutureBounds (){
        return new CircleShape(getFuturePositionInRoom().x,getFuturePositionInRoom().y,getWidth()/2);
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

    @Override
    public int getMargin(){
        return getWidth()/10;
    }

    public void shootHook(double xDown, double yDown) {
        if (getHook() != null) {
            removeHook();
        }
        MathVector downPoint = new MathVector(xDown,yDown);
        MathVector initP = new MathVector(getPositionInScreen(),downPoint);
        int radius = 10;
        int separation = 40;
        int nNodes = Math.min((int) (initP.magnitude()/separation) + 2, maxHookNodes);
        nNodes = Math.max(nNodes + 1,MIN_HOOSKSHOT_NODES);
        initP.scale(0.5);
//        initP.rescale(GameMath.linealValue(1,getHookVelocity()/1000,15,getHookVelocity(),nNodes));
        setHook(new Hook(getxPosInRoom(),getyPosInRoom(), 1, 0, nNodes, radius, Color.GRAY, separation,this,initP));
//        setP(new MathVector(0,0));
    }

    public void removeHook() {
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
        setState(STATE_MOVING);
    }

    public MathVector getHookVector() {
        if (getHook() != null) {
            return new MathVector(this,getHook().getLastNode());
        } else {
            return null;
        }
    }

    @Override
    public void die() {
        this.setHealth(this.getMaxHealth());
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
}
