package com.htss.hookshot.game.object.hook;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.htss.hookshot.constraints.RelativeToPointConstraint;
import com.htss.hookshot.game.object.debug.Circle;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.math.GameMath;
import com.htss.hookshot.math.MathVector;

import java.util.Vector;

/**
 * Created by Sergio on 02/08/2016.
 */
public class Chain extends GameDynamicObject {

    // Steel colours, for the chain and the hook's strike
    private static final int CABLE_DARK = Color.rgb(45, 48, 56), CABLE_LIGHT = Color.rgb(150, 156, 168),
            LINK_FILL = Color.rgb(125, 131, 143), LINK_EDGE = Color.rgb(28, 30, 36);
    private static final Paint cablePaint = new Paint(Paint.ANTI_ALIAS_FLAG), linkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path clawPath = new Path();

    static {
        cablePaint.setStyle(Paint.Style.STROKE);
        cablePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    private int separation, direction = 1;
    private Vector<Circle> nodes;

    public Chain(double xPos, double yPos, int mass, int collisionPriority, int nNodes, int radius, int color, int separation, boolean addToGameObjects, boolean addToDynamicObjects) {
        super(xPos, yPos, mass, collisionPriority, 500, addToGameObjects, addToDynamicObjects);
        this.separation = separation;
        this.nodes = new Vector<Circle>();
        for (int i = 0 ; i < nNodes ; i++) {
            Circle node = new Circle(xPos, yPos, mass, collisionPriority, radius, color, false);
            this.nodes.add(node);
        }
        getFirstNode().addConstraint(new RelativeToPointConstraint(new MathVector(xPos,yPos),0));
    }

    @Override
    public void update(){
        for (int i = 0 ; i < getNodesNumber() ; i++){
            int factor = (getDirection() > 0) ? 0 : 1;
            Circle node = getNode(factor*(getNodesNumber()-1) + getDirection()*i);
            node.update();
            if (GameMath.ins(0,factor*(getNodesNumber()-1) + getDirection()*i - getDirection(), getNodesNumber() - 1)){
                Circle prevNode = getNode(factor*(getNodesNumber()-1) + getDirection()*i - getDirection());
                MathVector prevToFuture = new MathVector(prevNode.getPositionInRoom(),node.getFuturePositionInRoom());
                if (prevToFuture.magnitude() > getSeparation()){
                    prevToFuture.rescale(getSeparation());
                    node.getP().scale(getMass()*3f/1000f);
                    prevNode.addP(node.getP());
                }
                node.setPositionInRoom(prevToFuture.applyTo(prevNode.getPositionInRoom()));
            } else {
                node.updatePosition();
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i=0 ; i < getNodesNumber() - 1 ; i++){
            Circle node1 = getNode(i);
            Circle node2 = getNode(i+1);
            drawCable(canvas, (float) node1.getxPosInScreen(), (float) node1.getyPosInScreen(), (float) node2.getxPosInScreen(), (float) node2.getyPosInScreen(), node1.getRadius());
        }
        for (Circle node : getNodes()) {
            drawLink(canvas, (float) node.getxPosInScreen(), (float) node.getyPosInScreen(), node.getRadius());
        }
    }

    // A steel cable, dark with a lighter line along it
    public static void drawCable(Canvas canvas, float x1, float y1, float x2, float y2, float radius) {
        cablePaint.setStrokeWidth(radius * 0.8f);
        cablePaint.setColor(CABLE_DARK);
        canvas.drawLine(x1, y1, x2, y2, cablePaint);
        cablePaint.setStrokeWidth(radius * 0.3f);
        cablePaint.setColor(CABLE_LIGHT);
        canvas.drawLine(x1, y1, x2, y2, cablePaint);
    }

    // A ring joining two lengths of cable
    public static void drawLink(Canvas canvas, float x, float y, float radius) {
        linkPaint.setStyle(Paint.Style.FILL);
        linkPaint.setColor(LINK_FILL);
        canvas.drawCircle(x, y, radius * 0.7f, linkPaint);
        linkPaint.setStyle(Paint.Style.STROKE);
        linkPaint.setStrokeWidth(radius * 0.25f);
        linkPaint.setColor(LINK_EDGE);
        canvas.drawCircle(x, y, radius * 0.7f, linkPaint);
    }

    // The hook's tip, pointing along the given unit direction
    public static void drawClaw(Canvas canvas, float x, float y, float directionX, float directionY, float radius) {
        float normalX = -directionY, normalY = directionX;
        clawPath.reset();
        clawPath.moveTo(x + directionX * radius * 1.8f, y + directionY * radius * 1.8f);
        clawPath.lineTo(x + (normalX * 1.1f - directionX * 0.3f) * radius, y + (normalY * 1.1f - directionY * 0.3f) * radius);
        clawPath.lineTo(x - directionX * radius * 0.1f, y - directionY * radius * 0.1f);
        clawPath.lineTo(x - (normalX * 1.1f + directionX * 0.3f) * radius, y - (normalY * 1.1f + directionY * 0.3f) * radius);
        clawPath.close();
        linkPaint.setStyle(Paint.Style.FILL);
        linkPaint.setColor(LINK_FILL);
        canvas.drawPath(clawPath, linkPaint);
        linkPaint.setStyle(Paint.Style.STROKE);
        linkPaint.setStrokeWidth(radius * 0.25f);
        linkPaint.setColor(LINK_EDGE);
        canvas.drawPath(clawPath, linkPaint);
    }

    @Override
     public void updatePosition() {
//        for (GameDynamicCircle node : getNodes()){
//            node.updatePosition();
//        }
    }

    @Override
    public int getWidth() {
        return (int) Math.abs(getNode(0).getxPosInRoom()-getNode(getNodesNumber()-1).getxPosInRoom());
    }

    @Override
    public int getHeight() {
        return (int) Math.abs(getNode(0).getyPosInRoom()-getNode(getNodesNumber()-1).getyPosInRoom());
    }

    protected boolean canBeMoved(int i){
        int mainNodeIndex = (getDirection() > 0) ? 0 : getNodesNumber() - 1;
        if (i == mainNodeIndex){
            return true;
        } else {
            MathVector vector = new MathVector(getNode(i-getDirection()).getFuturePositionInScreen(),getNode(i).getFuturePositionInScreen());
            return vector.magnitude() <= getSeparation();
        }
    }

    public int getSeparation() {
        return separation;
    }

    public void setSeparation(int separation) {
        this.separation = separation;
    }

    public Vector<Circle> getNodes() {
        return nodes;
    }

    public void setNodes(Vector<Circle> nodes) {
        this.nodes = nodes;
    }

    public int getNodesNumber(){
        return getNodes().size();
    }

    public void removeNode(Circle node){
        this.nodes.remove(node);
    }

    public Circle getNode(int index){
        return this.nodes.get(index);
    }

    public Circle getPrevNodeOf (Circle node){
        int i = getNodes().indexOf(node);
        return getNode(i - getDirection());
    }

    public Circle getLastNode(){
        if (getDirection() > 0) {
            return this.nodes.lastElement();
        } else {
            return this.nodes.firstElement();
        }
    }
    public Circle getFirstNode(){
        if (getDirection() > 0) {
            return this.nodes.firstElement();
        } else {
            return this.nodes.lastElement();
        }
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public double getCurrentLength(){
        double length = 0;
        for (int i = 0 ; i < getNodesNumber() - 1 ; i++){
            length += getNode(i).distanceTo(getNode(i+1));
        }
        return length;
    }

    public boolean isTense(){
//        return getCurrentLength() == getSeparation()*(getNodesNumber()-1);
        int lastIndex = getNodes().indexOf(getLastNode());
        return Math.round(getLastNode().distanceTo(getNode(lastIndex - getDirection()))) == getSeparation();
    }
}
