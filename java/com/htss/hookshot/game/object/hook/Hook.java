package com.htss.hookshot.game.object.hook;

import com.htss.hookshot.constraints.ChildOfConstraint;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.HUDCircleButton;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.debug.Circle;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.interfaces.Hookable;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

import java.util.Vector;

/**
 * Created by Sergio on 04/08/2016.
 */
public class Hook extends Chain {

    public static final int RADIUS = 10 * MyActivity.TILE_WIDTH / 100, SEPARATION = 40 * MyActivity.TILE_WIDTH / 100;

    public static final int MIN_RELOADING_NODES = 2;

    private boolean hooked = false, reloading = false, extending = false, fastReloading = false;
    private MathVector hookedPoint;
    private GameDynamicObject hookedObject;
    private int prevHookedMass = 0;

    public Hook(double xPos, double yPos, int nNodes, int color, GameDynamicObject parent, MathVector initP) {
        super(xPos, yPos, 1, 1, nNodes, RADIUS, color,  SEPARATION, true, false);
        getFirstNode().removeAllConstraints();
        getFirstNode().addConstraint(new ChildOfConstraint(parent));
        for (Circle node : getNodes()){
            node.setGhost(true);
            if (!node.equals(getFirstNode())){
                node.setP(initP);
            }
        }
    }

    @Override
    public void update() {
        updateFrame();
        manageHooking();
        if (isFastReloading()) {
            fastReload();
        }
        for (int i = 0 ; i < getNodesNumber() ; i++){
            int factor = (getDirection() > 0) ? 0 : 1;
            Circle node = getNode(factor*(getNodesNumber()-1) + getDirection()*i);
            node.update();
            int prevNodeIndex = factor*(getNodesNumber()-1) + getDirection()*i - getDirection();
            if (GameMath.ins(0,prevNodeIndex,getNodesNumber()-1)){
                Circle prevNode = getNode(prevNodeIndex);
                MathVector prevToFuture = new MathVector(prevNode.getPositionInRoom(),node.getFuturePositionInRoom());
                if (prevToFuture.magnitude() > getSeparation()){
                    manageSeparation(node,prevNode,prevToFuture);
                }
                if (isReloading()){
                    if (node.equals(getLastNode())){
                        reload(node, prevNode);
                        node.updatePosition();
                    } else {
                        node.setPositionInRoom(prevToFuture.applyTo(prevNode.getPositionInRoom()));
                    }
                } else if (isExtending()){
                    if (node.equals(getLastNode())){
                        extend();
                    }
                    node.setPositionInRoom(prevToFuture.applyTo(prevNode.getPositionInRoom()));
                } else {
                    node.setPositionInRoom(prevToFuture.applyTo(prevNode.getPositionInRoom()));
                }
            } else {
                if (!isHooked()){
                    node.updatePosition();
                } else {
                    node.setPositionInRoom(hookedPoint);
                    if (hookedObject != null){
                        hookedObject.addP(node.getP());
                        hookedObject.update();
                        hookedPoint = hookedObject.getPositionInRoom();
                    }
                    if (getNodesNumber() == 1 && isExtending()) {
                        extend();
                    }
                }
            }
        }
    }

    public void hook(MathVector position) {
        if (MyActivity.currentMap != null) {
            addHookButtons();
        }
        hookedPoint = position;
//        getLastNode().addConstraint(new RelativeToPointConstraint(position,0));
        hooked = true;
        setDirection(-1);
        for (Circle node : getNodes()) {
            node.setP(new MathVector(0, 0));
            node.setGhost(false);
        }
    }

    private void addHookButtons(){
        int buttonRadius = (int) (MyActivity.TILE_WIDTH*0.4);
        MyActivity.reloadButton = new HUDCircleButton(9 * MyActivity.screenWidth / 10, MyActivity.screenHeight / 2, buttonRadius,"R", true, new Execution() {
            @Override
            public double execute() {
                MyActivity.character.getHook().setReloading(true);
                return 0;
            }
        }, new Execution() {
            @Override
            public double execute() {
                MyActivity.character.getHook().setReloading(false);
                return 0;
            }
        }, new Execution() {
            @Override
            public double execute() {
                setFastReloading(true);
                return 0;
            }
        }
        );
        MyActivity.extendButton = new HUDCircleButton((int) getPrevNodeOf(getLastNode()).getxPosInScreen(), (int) getPrevNodeOf(getLastNode()).getyPosInScreen(), buttonRadius, "E", true, new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.getHook() != null) {
                    MyActivity.character.getHook().setExtending(true);
                    MyActivity.character.setMass(10);
                }
                return 0;
            }
        }, new Execution() {
            @Override
            public double execute() {
                if (MyActivity.character.getHook() != null) {
                    MyActivity.character.getHook().setExtending(false);
                    MyActivity.character.setMass(1);
                }
                return 0;
            }
        });

        MyActivity.hudElements.add(MyActivity.reloadButton);
        MyActivity.hudElements.add(MyActivity.extendButton);
    }

    private void reload(Circle node, Circle prevNode){
        if (node.distanceTo(prevNode) <= node.getRadius()){
            if (getNodes().size() > MIN_RELOADING_NODES) {
                removeNode(prevNode);
            }
        } else {
            MathVector reloadVector = new MathVector(node.getPositionInRoom(), prevNode.getPositionInRoom());
            node.setP(reloadVector.scaled(1));
        }
    }

    private void extend(){
        int lastIndex = getNodes().indexOf(getLastNode());
        if (getNodesNumber() == 1 || getLastNode().distanceTo(getNode(lastIndex - getDirection())) > getSeparation()*2/3){
            if (getNodesNumber() < MyActivity.character.getMaxHookNodes()) {
                Circle node = new Circle(getLastNode().getxPosInRoom(), getLastNode().getyPosInRoom(), getLastNode().getMass(), getLastNode().getCollisionPriority(), getLastNode().getRadius(), getLastNode().getColor(), false);
                getNodes().insertElementAt(node, lastIndex - getDirection());
            }
        }
    }

    public void fastReload() {
        if (getNodesNumber() > MIN_RELOADING_NODES) {
            setNodes(new Vector<Circle>(getNodes().subList(0, getNodesNumber()-1)));
        } else {
            getLastNode().setP(new MathVector(0,0));
            setFastReloading(false);
        }
    }

    private void manageSeparation(Circle node, Circle prevNode, MathVector prevToFuture) {
        prevToFuture.rescale(getSeparation());
        if (hooked){
            node.getP().scale(0.5);
        }
        prevNode.addP(node.getP());
    }

    private void manageHooking() {
        if (!isHooked()){
            for (GameDynamicObject dynamicObject : MyActivity.dynamicObjects){
                if (dynamicObject instanceof Hookable){
                    if (dynamicObject.inContactWith(getLastNode())){
                        hook(dynamicObject.getPositionInRoom());
                        hookedObject = dynamicObject;
                        prevHookedMass = dynamicObject.getMass();
                        hookedObject.setMass(1);
                    }
                }
            }
            if (getLastNode().inContactWithMap(getLastNode().getRadius()) && getLastNode().getPositionInRoom().distanceTo(MyActivity.character.getPositionInRoom()) > MyActivity.TILE_WIDTH/500.0) {
                hook(getLastNode().getPositionInRoom());
            }
            if (getFrame() > TimeUtil.convertSecondToGameSecond(2)){
                MyActivity.character.removeHook();
            }
        } else {
            if (MyActivity.extendButton != null) {
                MyActivity.extendButton.setCenter(getLastNode().getPositionInScreen());
            }
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public void setHooked(boolean hooked) {
        this.hooked = hooked;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public boolean isExtending() {
        return extending;
    }

    public void setExtending(boolean extending) {
        this.extending = extending;
    }

    public MathVector getHookedPoint() {
        return hookedPoint;
    }

    public void setHookedPoint(MathVector hookedPoint) {
        this.hookedPoint = hookedPoint;
    }

    public GameDynamicObject getHookedObject() {
        return hookedObject;
    }

    public void setHookedObject(GameDynamicObject hookedObject) {
        this.hookedObject = hookedObject;
    }

    public int getPrevHookedMass() {
        return prevHookedMass;
    }

    public void setPrevHookedMass(int prevHookedMass) {
        this.prevHookedMass = prevHookedMass;
    }

    public boolean isFastReloading() {
        return fastReloading;
    }

    public void setFastReloading(boolean fastReloading) {
        this.fastReloading = fastReloading;
    }
}
