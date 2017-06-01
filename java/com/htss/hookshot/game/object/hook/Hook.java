package com.htss.hookshot.game.object.hook;

import com.htss.hookshot.R;
import com.htss.hookshot.constraints.ChildOfConstraint;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.HUDButton;
import com.htss.hookshot.game.object.debug.Circle;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.interfaces.Hookable;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

/**
 * Created by Sergio on 04/08/2016.
 */
public class Hook extends Chain {

    private boolean hooked = false, reloading = false, extending = false;
    private MathVector hookedPoint;
    private GameDynamicObject hookedObject;
    private int prevHookedMass = 0;

    public Hook(double xPos, double yPos, int mass, int collisionPriority, int nNodes, int radius, int color, int separation, GameDynamicObject parent, MathVector initP) {
        super(xPos, yPos, mass, collisionPriority, nNodes, radius, color, separation);
        getFirstNode().removeAllConstraints();
        getFirstNode().addConstraint(new ChildOfConstraint(parent));
        for (Circle node : getNodes()){
            node.setGhost(true);
            if (!node.equals(getFirstNode())){
                node.setP(initP);
            }
        }
        MyActivity.canvas.gameObjects.add(this);
//        MyActivity.dynamicObjects.addAll(getNodes());
    }

    @Override
    public void update() {
        updateFrame();
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
            if (getLastNode().inContactWithMap(getLastNode().getRadius())) {
                hook(getLastNode().getPositionInRoom());
            }
            if (getFrame() > TimeUtil.convertSecondToGameSecond(2)){
                MyActivity.character.removeHook();
            }
        } else {
//            int index = getNodes().indexOf(getFirstNode());
//            do {
//                MyActivity.reloadButton.setCenter(getNode(index).getPositionInScreen());
//                index += getDirection();
//            } while (!MyActivity.isInScreen(MyActivity.reloadButton.getCenter(),MyActivity.reloadButton.getWidth()/2));
//            MyActivity.reloadButton.setCenter(new MathVector(9*MyActivity.screenWidth/10,MyActivity.screenHeight/2));
            MyActivity.extendButton.setCenter(getLastNode().getPositionInScreen());
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
                }
            }
        }
    }

    private void hook(MathVector position) {
        addHookButtons();
        hookedPoint = position;
//        getLastNode().addConstraint(new RelativeToPointConstraint(position,0));
        hooked = true;
        setDirection(-1);
        for (Circle node : getNodes()){
            node.setP(new MathVector(0,0));
            node.setGhost(false);
        }
    }

    private void addHookButtons(){
        MyActivity.reloadButton = new HUDButton(9*MyActivity.screenWidth/10,MyActivity.screenHeight/2,
                MyActivity.canvas.getBitmapById(R.drawable.button_r),MyActivity.canvas.getBitmapById(R.drawable.button_r_pressed),true,new Execution() {
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
        });
        MyActivity.extendButton = new HUDButton((int)getPrevNodeOf(getLastNode()).getxPosInScreen(),(int)getPrevNodeOf(getLastNode()).getyPosInScreen(),
                MyActivity.canvas.getBitmapById(R.drawable.button_e),MyActivity.canvas.getBitmapById(R.drawable.button_e_pressed),true,new Execution() {
            @Override
            public double execute() {
                MyActivity.character.getHook().setExtending(true);
                MyActivity.character.setMass(10);
                return 0;
            }
        }, new Execution() {
            @Override
            public double execute() {
                MyActivity.character.getHook().setExtending(false);
                MyActivity.character.setMass(1);
                return 0;
            }
        });
        MyActivity.hudElements.add(MyActivity.reloadButton);
        MyActivity.hudElements.add(MyActivity.extendButton);
    }

    private void reload(Circle node, Circle prevNode){
        if (node.distanceTo(prevNode) <= node.getRadius()){
            if (getNodes().size() > 2) {
                removeNode(prevNode);
            }
        } else {
            MathVector reloadVector = new MathVector(node.getPositionInRoom(), prevNode.getPositionInRoom());
            node.setP(reloadVector.scaled(1));
        }
    }

    private void extend(){
        int lastIndex = getNodes().indexOf(getLastNode());
        if (getLastNode().distanceTo(getNode(lastIndex - getDirection())) > getSeparation()*2/3){
            if (getNodes().size() < MyActivity.character.getMaxHookNodes()) {
                Circle node = new Circle(getLastNode().getxPosInRoom(), getLastNode().getyPosInRoom(), getLastNode().getMass(), getLastNode().getCollisionPriority(), getLastNode().getRadius(), getLastNode().getColor());
                getNodes().insertElementAt(node, lastIndex - getDirection());
            }
        }
    }

    private void manageSeparation(Circle node, Circle prevNode, MathVector prevToFuture) {
        prevToFuture.rescale(getSeparation());
        if (hooked){
            node.getP().scale(0.5);
        }
        prevNode.addP(node.getP());
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
}
