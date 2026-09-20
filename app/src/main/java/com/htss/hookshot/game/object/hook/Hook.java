package com.htss.hookshot.game.object.hook;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.HUDCircleButton;
import com.htss.hookshot.game.hud.UiStyle;
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

    // How much chain is taken in every update while reeling and while zipping, and let out while paying it out
    public static final double REEL_SPEED = SEPARATION / 2.0, FAST_REEL_SPEED = SEPARATION, LET_OUT_SPEED = SEPARATION / 4.0;
    // How much of their speed the links keep every update, and how many times the links are pulled back together
    private static final double DAMPING = 0.99;
    private static final int ITERATIONS = 10;
    // How many updates in a row a zip can go without taking chain in before it gives up
    private static final int ZIP_GIVES_UP = (int) TimeUtil.secondsToUpdates(0.3);
    // Reeled in all the way, the chain keeps this much of its last link
    private static final double SHORTEST_LINK = RADIUS;
    // How far apart the points checked along a line of sight are
    private static final double SIGHT_STEP = Math.max(2, MyActivity.TILE_WIDTH / 40.0);

    private boolean hooked = false, reloading = false, extending = false, fastReloading = false;
    // How long the finger sliding on the screen wants the chain to be. Below zero, the chain is left as it is
    private double targetLength = -1;
    // How long the chain was when it was thrown, which the gauge fills up to
    private double lengthWhenHooked = SEPARATION;
    // For how many updates in a row a zip hasn't been able to take any chain in
    private int zipStalled = 0;
    // Whether the chain has just been coming in (-1) or going out (1), and for how many more updates that shows
    private int motion = 0, motionShown = 0;
    private double lengthLastUpdate = 0;
    private static final int MOTION_SHOWN = 8;
    private final Paint markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path markPath = new Path();
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
            if (motionShown > 0 && hooked && MyActivity.currentMap != null) {
                drawMotion(canvas);
            }
        }
    }

    // The chain moves with the character, in updateChain, once the character has moved
    @Override
    public void update() {
        updateFrame();
        manageHooking();
    }

    // Hooked with every link at its full length
    public void hook(MathVector position) {
        hook(position, (getNodesNumber() - 1) * (double) SEPARATION);
    }

    // Hooked with this much chain out, which the link the character holds makes up for, being the only one that can
    // be shorter than the rest
    public void hook(MathVector position, double length) {
        if (MyActivity.currentMap != null && MyActivity.controls == MyActivity.CONTROLS_CLASSIC) {
            addExtendButton();
        }
        hookedPoint = position;
        hooked = true;
        setDirection(-1);
        // Laid straight back from where it's hooked, a link apart, and left to fall into shape
        MathVector start = getGripNode().getPositionInRoom();
        MathVector toHook = new MathVector(start, position);
        double distance = toHook.magnitude();
        int last = getNodesNumber() - 1;
        for (int i = 0; i <= last; i++) {
            Circle node = getNode(i);
            double along = (i == 0 || distance == 0) ? 0 : Math.max(0, distance - (last - i) * SEPARATION) / distance;
            node.setPositionInRoom(toHook.scaled(along).applyTo(start));
            node.setP(new MathVector(0, 0));
        }
        pivot = last;
        firstLinkLength = Math.max(SHORTEST_LINK, Math.min(SEPARATION, length - (last - 1) * SEPARATION));
        lengthWhenHooked = getChainLength();
        lengthLastUpdate = lengthWhenHooked;
    }

    // Moves the chain after the character has: the first node to the character's hands, the last to where it's
    // hooked, and the rest falling and swinging between them, held together and stopped by rock. Then finds what the
    // character swings around
    public void updateChain(MathVector body, MathVector hands) {
        // Reeled in only as fast as the character follows, so it doesn't run out while the character is caught on
        // rock and then yank it
        double behind = body.distanceTo(getPivotNode().getPositionInRoom()) - body.distanceTo(hands) - getLengthToPivot();
        // Nor while the chain ahead leads into rock, out of sight. Caught under a ledge, the character stays where it is
        // but the chain kept coming in through the rock, until it was far shorter than the way to where it's hooked
        boolean following = behind < SEPARATION / 2.0 && canSee(body, getPivotNode().getPositionInRoom());
        // Pulling on the chain, so chain let out goes to the character instead of piling up slack
        boolean taut = behind > -SEPARATION / 2.0;
        if (isFastReloading()) {
            if (following) {
                reelIn(FAST_REEL_SPEED);
                zipStalled = 0;
            } else {
                zipStalled++;
            }
            // It also gives up when the character is caught on rock. Otherwise it stayed on for as long as that took,
            // and dragged the character off at full speed whenever it came free, with nobody asking for it
            if (getNodesNumber() <= MIN_RELOADING_NODES || zipStalled > ZIP_GIVES_UP) {
                setFastReloading(false);
            }
        } else if (isReeling()) {
            // Towards the length asked for, a step at a time, so the character is never yanked
            double difference = targetLength - getChainLength();
            if (difference < 0 && following) {
                reelIn(Math.min(REEL_SPEED, -difference));
            } else if (difference > 0 && taut) {
                letOut(Math.min(LET_OUT_SPEED, difference));
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
        // Whatever changed its length since the last update, here or as the character pulled it out
        double length = getChainLength();
        if (Math.abs(length - lengthLastUpdate) > 0.5) {
            motion = (length < lengthLastUpdate) ? -1 : 1;
            motionShown = MOTION_SHOWN;
        } else if (motionShown > 0) {
            motionShown--;
        }
        lengthLastUpdate = length;
    }

    // Small arrowheads sliding along the chain by the character's hands while it's coming in or going out: gold and
    // towards where it's hooked as it's reeled in, grey and towards the character as it's let out
    private void drawMotion(Canvas canvas) {
        MathVector from = getGripNode().getPositionInScreen();
        MathVector along = new MathVector(from, getPivotNode().getPositionInScreen());
        double room = along.magnitude();
        if (room < SEPARATION) {
            return;
        }
        along.normalize();
        float tile = MyActivity.TILE_WIDTH, size = tile * 0.1f, spacing = tile * 0.3f;
        float dx = (float) along.x * -motion, dy = (float) along.y * -motion;
        float slide = (getFrame() * tile * 0.03f) % spacing;
        markPaint.setStyle(Paint.Style.STROKE);
        markPaint.setStrokeCap(Paint.Cap.ROUND);
        markPaint.setStrokeJoin(Paint.Join.ROUND);
        markPaint.setStrokeWidth(tile * 0.035f);
        markPaint.setColor(motion < 0 ? UiStyle.GOLD : UiStyle.IDLE);
        markPaint.setAlpha(255 * motionShown / MOTION_SHOWN);
        for (int i = 0; i < 3; i++) {
            float distance = tile * 0.45f + i * spacing + (motion < 0 ? slide : spacing - slide);
            if (distance > room) {
                break;
            }
            float x = (float) (from.x + along.x * distance), y = (float) (from.y + along.y * distance);
            markPath.reset();
            markPath.moveTo(x - dx * size - dy * size, y - dy * size + dx * size);
            markPath.lineTo(x + dx * size, y + dy * size);
            markPath.lineTo(x - dx * size + dy * size, y - dy * size - dx * size);
            canvas.drawPath(markPath, markPaint);
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

    // Lets out chain, up to the longest the character can hold
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
    // character. Only the classic controls have it: the others pay the chain out by sliding the finger
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

    // How far it is along the chain as it lies, node to node, from the character's hands to where it's hooked. Longer
    // than the chain itself when it's stretched, like round a corner of rock with the character held back
    public double getPathLength() {
        double length = 0;
        for (int i = 0; i < getNodesNumber() - 1; i++) {
            length += getNode(i).getPositionInRoom().distanceTo(getNode(i + 1).getPositionInRoom());
        }
        return length;
    }

    // From the character's hands to where the chain is hooked
    public double getChainLength() {
        return firstLinkLength + (getNodesNumber() - 2) * SEPARATION;
    }

    // The longest chain the character can hold out
    public double getLongestChain() {
        return (MyActivity.character.getMaxHookNodes() - 1) * SEPARATION;
    }

    // The most chain this one has had out, which is as far as the gauge goes
    public double getChainWhenHooked() {
        return Math.max(lengthWhenHooked, getChainLength());
    }

    // Reels towards this length, however far the finger slides. Sliding up asks for less chain, down for more
    public void reelTo(double length) {
        targetLength = Math.max(SHORTEST_LINK, Math.min(length, getLongestChain()));
    }

    // The finger was lifted: the chain stays as long as it is, and the character stays hooked
    public void stopReeling() {
        targetLength = -1;
    }

    public boolean isReeling() {
        return targetLength >= 0;
    }

    // Asked for more chain than is out
    public boolean isLettingOut() {
        return targetLength > getChainLength();
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
        } else if (MyActivity.extendButton != null) {
            MyActivity.extendButton.setCenter(getGripNode().getPositionInScreen());
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public void setHooked(boolean hooked) {
        this.hooked = hooked;
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
        zipStalled = 0;
    }
}
