package com.htss.hookshot.game.object.hook;

import android.graphics.Canvas;
import android.graphics.Color;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.HUDCircleButton;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.debug.Circle;
import com.htss.hookshot.game.object.obstacles.Door;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.interfaces.Hookable;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

import java.util.LinkedList;

/**
 * Created by Sergio on 04/08/2016.
 */
public class Hook extends Chain {

    public static final int RADIUS = 10 * MyActivity.TILE_WIDTH / 100, SEPARATION = 40 * MyActivity.TILE_WIDTH / 100;

    public static final int MIN_RELOADING_NODES = 2;

    // How much of the chain is reeled in every update: holding a finger on the screen, and after a double tap
    public static final double REEL_SPEED = SEPARATION / 2.0, FAST_REEL_SPEED = SEPARATION;
    // How much of their speed the links keep every update, and how many times the links are pulled back together
    private static final double DAMPING = 0.99;
    private static final int ITERATIONS = 10;
    // Reeled in all the way, the chain keeps this much of its last link
    private static final double SHORTEST_LINK = RADIUS;
    // How far apart the points checked along a line of sight are
    private static final double SIGHT_STEP = Math.max(2, MyActivity.TILE_WIDTH / 40.0);

    private boolean hooked = false, reloading = false, extending = false, fastReloading = false;
    private MathVector hookedPoint;
    private GameDynamicObject hookedObject;
    private int prevHookedMass = 0;
    // The first node is held by the character and the last is where the chain is hooked. Every link is SEPARATION
    // long but the one the character holds, which shortens as the chain is reeled in and lengthens as it's let out
    private double firstLinkLength = SEPARATION;
    // The node the character swings around: where the chain is hooked, or where it bends around rock on the way
    private int pivot;
    // Where the free nodes were before this update, and the doors, which stop the chain like rock
    private MathVector[] previous = new MathVector[0];
    private LinkedList<GameShape> doors = new LinkedList<GameShape>();

    public Hook(double xPos, double yPos, int nNodes, int color) {
        super(xPos, yPos, 1, 1, nNodes, RADIUS, color,  SEPARATION, true, false);
    }

    // The claw where the chain is hooked, pointing into the rock
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (getNodesNumber() > 1) {
            Circle claw = getAnchorNode();
            MathVector direction = new MathVector(getNode(getNodesNumber() - 2).getPositionInScreen(), claw.getPositionInScreen());
            if (!direction.isNull()) {
                direction.normalize();
                drawClaw(canvas, (float) claw.getxPosInScreen(), (float) claw.getyPosInScreen(), (float) direction.x, (float) direction.y, claw.getRadius());
            }
        }
    }

    // The chain moves with the character, in updateChain, once the character has moved
    @Override
    public void update() {
        updateFrame();
        manageHooking();
    }

    public void hook(MathVector position) {
        if (MyActivity.currentMap != null) {
            addExtendButton();
        }
        hookedPoint = position;
        hooked = true;
        setDirection(-1);
        // Laid straight from the character to where it's hooked, and left to fall into shape
        MathVector start = getGripNode().getPositionInRoom();
        MathVector toHook = new MathVector(start, position);
        for (int i = 0; i < getNodesNumber(); i++) {
            Circle node = getNode(i);
            node.setPositionInRoom(toHook.scaled(i / (double) (getNodesNumber() - 1)).applyTo(start));
            node.setP(new MathVector(0, 0));
        }
        pivot = getNodesNumber() - 1;
        firstLinkLength = SEPARATION;
    }

    // Moves the chain after the character has: the first node to the character's hands, the last to where it's
    // hooked, and the rest falling and swinging between them, held together and stopped by rock. Then finds what the
    // character swings around
    public void updateChain(MathVector body, MathVector hands) {
        // Reeled in only as fast as the character follows, so it doesn't run out while the character is caught on
        // rock and then yank it
        double behind = body.distanceTo(getPivotNode().getPositionInRoom()) - body.distanceTo(hands) - getLengthToPivot();
        boolean following = behind < SEPARATION / 2.0;
        if (isFastReloading()) {
            if (following) {
                reelIn(FAST_REEL_SPEED);
            }
            if (getNodesNumber() <= MIN_RELOADING_NODES) {
                setFastReloading(false);
            }
        } else if (isReloading() && following) {
            reelIn(REEL_SPEED);
        }
        if (hookedObject != null) {
            hookedPoint = hookedObject.getPositionInRoom();
        }
        getAnchorNode().setPositionInRoom(hookedPoint);
        getGripNode().setPositionInRoom(hands);
        findDoors();
        int last = getNodesNumber() - 1;
        if (previous.length < getNodesNumber()) {
            previous = new MathVector[getNodesNumber() * 2];
        }
        for (int i = 1; i < last; i++) {
            Circle node = getNode(i);
            previous[i] = node.getPositionInRoom();
            MathVector speed = node.getP().scaled(DAMPING);
            speed.y += getGravity();
            node.setPositionInRoom(speed.applyTo(previous[i]));
        }
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            // Alternating the direction spreads each pull along the whole chain sooner
            boolean forwards = iteration % 2 == 0;
            for (int k = 0; k < last; k++) {
                int i = forwards ? k : last - 1 - k;
                holdTogether(i, i + 1, (i == 0) ? firstLinkLength : SEPARATION, last);
            }
            for (int i = 1; i < last; i++) {
                keepOutOfRock(getNode(i), previous[i]);
            }
        }
        findPivot(body);
        if (body.distanceTo(getPivotNode().getPositionInRoom()) >= getLengthToPivot() + body.distanceTo(hands) - 1) {
            straightenToPivot(hands);
        }
        for (int i = 1; i < last; i++) {
            getNode(i).setP(new MathVector(previous[i], getNode(i).getPositionInRoom()));
        }
    }

    // Pulls two neighbouring nodes back together when they're further apart than their link. The end nodes don't move
    private void holdTogether(int a, int b, double length, int last) {
        Circle nodeA = getNode(a), nodeB = getNode(b);
        MathVector between = new MathVector(nodeA.getPositionInRoom(), nodeB.getPositionInRoom());
        double distance = between.magnitude();
        if (distance <= length || distance == 0) {
            return;
        }
        boolean aMoves = a != 0 && a != last, bMoves = b != 0 && b != last;
        if (!aMoves && !bMoves) {
            return;
        }
        MathVector pull = between.scaled((distance - length) / distance);
        if (aMoves && bMoves) {
            pull.scale(0.5);
        }
        if (aMoves) {
            nodeA.setPositionInRoom(pull.applyTo(nodeA.getPositionInRoom()));
        }
        if (bMoves) {
            nodeB.setPositionInRoom(pull.scaled(-1).applyTo(nodeB.getPositionInRoom()));
        }
    }

    // A node that moved into rock or a door only keeps the part of its move along the surface, or stays where it was.
    // One that was already inside, like right where the chain is hooked, can get out
    private void keepOutOfRock(Circle node, MathVector from) {
        MathVector to = node.getPositionInRoom();
        if (!isSolid(to.x, to.y) || isSolid(from.x, from.y)) {
            return;
        }
        if (!isSolid(to.x, from.y)) {
            node.setPositionInRoom(to.x, from.y);
        } else if (!isSolid(from.x, to.y)) {
            node.setPositionInRoom(from.x, to.y);
        } else {
            node.setPositionInRoom(from);
        }
    }

    // The character swings around the furthest node along the chain it can see. The chain catches on rock as the
    // character swings past it, and comes free as it swings back
    private void findPivot(MathVector body) {
        int last = getNodesNumber() - 1;
        pivot = Math.max(1, Math.min(pivot, last));
        while (pivot > 1 && !canSee(body, getNode(pivot).getPositionInRoom())) {
            pivot--;
        }
        while (pivot < last && canSee(body, getNode(pivot + 1).getPositionInRoom())) {
            pivot++;
        }
    }

    // Taut, the chain runs straight from the hands to the node the character swings around
    private void straightenToPivot(MathVector hands) {
        MathVector direction = new MathVector(hands, getPivotNode().getPositionInRoom());
        if (direction.isNull()) {
            return;
        }
        direction.normalize();
        double along = firstLinkLength;
        for (int i = 1; i < pivot; i++) {
            getNode(i).setPositionInRoom(direction.scaled(along).applyTo(hands));
            along += SEPARATION;
        }
    }

    // Nothing solid on the way, ignoring what's right around the end, as a node there lies against rock
    private boolean canSee(MathVector from, MathVector to) {
        MathVector direction = new MathVector(from, to);
        double length = direction.magnitude() - RADIUS * 2;
        if (length <= 0) {
            return true;
        }
        direction.normalize();
        for (double along = SIGHT_STEP; along < length; along += SIGHT_STEP) {
            if (isSolid(from.x + direction.x * along, from.y + direction.y * along)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSolid(double x, double y) {
        if (MyActivity.currentMap == null || !MyActivity.isInRoom(x, y)) {
            return false;
        }
        if (Color.alpha(MyActivity.canvas.mapBitmap.getPixel((int) x, (int) y)) == 255) {
            return true;
        }
        if (!doors.isEmpty()) {
            MathVector point = new MathVector(x, y);
            for (GameShape door : doors) {
                if (door.contains(point)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void findDoors() {
        doors.clear();
        for (GameDynamicObject object : MyActivity.dynamicObjects) {
            if (object instanceof Door) {
                doors.add(object.getBounds());
            }
        }
    }

    // Takes the link the character holds in, and the next one once it's all in
    private void reelIn(double length) {
        firstLinkLength -= length;
        while (firstLinkLength <= 0 && getNodesNumber() > MIN_RELOADING_NODES) {
            getNodes().remove(1);
            pivot = Math.max(1, pivot - 1);
            firstLinkLength += SEPARATION;
        }
        firstLinkLength = Math.max(firstLinkLength, SHORTEST_LINK);
    }

    // Lets out as much chain as the character pulls, while the extend button is held, up to the longest chain
    public void letOut(double length) {
        firstLinkLength += length;
        while (firstLinkLength > SEPARATION) {
            if (getNodesNumber() >= MyActivity.character.getMaxHookNodes()) {
                firstLinkLength = SEPARATION;
                return;
            }
            Circle grip = getGripNode(), next = getNode(1);
            MathVector toNext = new MathVector(grip.getPositionInRoom(), next.getPositionInRoom());
            double distance = toNext.magnitude();
            MathVector position = (distance > 0) ? toNext.scaled(Math.min(1, (firstLinkLength - SEPARATION) / distance)).applyTo(grip.getPositionInRoom()) : grip.getPositionInRoom();
            Circle node = new Circle(position.x, position.y, next.getMass(), next.getCollisionPriority(), next.getRadius(), next.getColor(), false);
            node.setP(new MathVector(next.getP().x, next.getP().y));
            getNodes().insertElementAt(node, 1);
            pivot++;
            firstLinkLength -= SEPARATION;
        }
    }

    // How much chain there is from the character's hands to the node it swings around
    public double getLengthToPivot() {
        return firstLinkLength + (pivot - 1) * SEPARATION;
    }

    public Circle getPivotNode() {
        return getNode(Math.max(1, Math.min(pivot, getNodesNumber() - 1)));
    }

    public Circle getGripNode() {
        return getNodes().firstElement();
    }

    public Circle getAnchorNode() {
        return getNodes().lastElement();
    }

    // The chain is reeled in by holding a finger on the screen, and let out by holding this button, which follows the
    // character
    private void addExtendButton(){
        int buttonRadius = (int) (MyActivity.TILE_WIDTH*0.4);
        MyActivity.extendButton = new HUDCircleButton((int) getGripNode().getxPosInScreen(), (int) getGripNode().getyPosInScreen(), buttonRadius, "E", true, new Execution() {
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

        MyActivity.hudElements.add(MyActivity.extendButton);
    }

    private void manageHooking() {
        if (!isHooked()){
            for (GameDynamicObject dynamicObject : MyActivity.dynamicObjects){
                if (dynamicObject instanceof Hookable){
                    if (dynamicObject.inContactWith(getAnchorNode())){
                        hook(dynamicObject.getPositionInRoom());
                        hookedObject = dynamicObject;
                        prevHookedMass = dynamicObject.getMass();
                        hookedObject.setMass(1);
                    }
                }
            }
            if (getFrame() > TimeUtil.secondsToUpdates(0.333)){
                MyActivity.character.removeHook();
            }
        } else {
            if (MyActivity.extendButton != null) {
                MyActivity.extendButton.setCenter(getGripNode().getPositionInScreen());
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
