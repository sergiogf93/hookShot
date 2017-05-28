package com.htss.hookshot.game.object;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.animation.MainCharacterAnimation;
import com.htss.hookshot.game.object.shapes.BiCircleShape;
import com.htss.hookshot.game.object.shapes.CircleShape;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 03/08/2016.
 */
public class MainCharacter extends GameCharacter {

    public static final int BODY_RADIUS = 10*MyActivity.tileWidth/50, FIST_RADIUS = MyActivity.tileWidth/8, FOOT_RADIUS = 10*MyActivity.tileWidth/100, EYE_RADIUS = MyActivity.tileWidth/25;

    private double hookVelocity = 300;
    private int maxHookNodes = 50, facing = 1;
    private Hook hook = null;
    private CircleShape rightHand, leftHand, rightFoot, leftFoot;
    private BiCircleShape leftEye, rightEye;

    public MainCharacter(double xPos, double yPos, int mass, int collisionPriority) {
        super(xPos, yPos, mass, collisionPriority, 20);
        makeSureNotUnderground = true;
        rightHand = new CircleShape(xPos,yPos,FIST_RADIUS,Color.WHITE);
        leftHand = new CircleShape(xPos,yPos,FIST_RADIUS,Color.WHITE);
        leftFoot = new CircleShape(xPos,yPos,FOOT_RADIUS,Color.RED);
        rightFoot = new CircleShape(xPos,yPos,FOOT_RADIUS,Color.RED);
        leftEye = new BiCircleShape(xPos,yPos,EYE_RADIUS*0.8,new MathVector(0,1),EYE_RADIUS,Color.YELLOW);
        rightEye = new BiCircleShape(xPos,yPos,EYE_RADIUS*0.8,new MathVector(0,1),EYE_RADIUS,Color.YELLOW);
        friction = 1.;
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
//        else if (getyPosInScreen() - getHeight() < 0) {
//            p.x = 0;
//            if (getP().y < 0) {
//                p.y = 1;
//            }
//        }
//        } else if (getyPosInScreen() + 2*getHeight() < 0){
//            MyActivity.switchMap(-1);
//            if (isHooked()){
//                removeHook();
//            }
//        }
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
        MathVector initP = new MathVector(getPositionInScreen(),new MathVector(xDown,yDown));
        int radius = 10;
        int separation = 40;
        int nNodes = Math.min((int) (initP.magnitude()/separation) + 2, maxHookNodes);
        nNodes = Math.max(nNodes+1,4);
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
}
