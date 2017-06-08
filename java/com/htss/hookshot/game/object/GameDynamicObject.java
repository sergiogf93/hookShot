package com.htss.hookshot.game.object;

import android.graphics.Color;

import com.htss.hookshot.constraints.Constraint;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.shapes.GameShape;
import com.htss.hookshot.game.object.shapes.RectShape;
import com.htss.hookshot.math.MathVector;

import java.util.Vector;

/**
 * Created by Sergio on 28/07/2016.
 */
public abstract class GameDynamicObject extends GameObject {

    private static final int MAXIMUM_VERTICAL_MOMENTUM = 25;

    private double maxVelocity;
    protected MathVector p;
    protected boolean makeSureNotUnderground = false, onFloor = false, ghost = false;
    protected double friction = 0.75;
    private int mass, collisionPriority, frame = 0;
    private Vector<Constraint> constraints;

    public GameDynamicObject(double xPos, double yPos, int mass, int collisionPriority, double maxVelocity, boolean addToGameObjectsList, boolean addToDynamicObjectsList) {
        super(xPos, yPos, addToGameObjectsList);
        this.p = new MathVector(0, 0);
        this.mass = mass;
        this.constraints = new Vector<Constraint>();
        this.collisionPriority = collisionPriority;
        this.maxVelocity = maxVelocity;
        if (addToDynamicObjectsList) {
            MyActivity.dynamicObjects.add(this);
        }
    }

    public void update(){
        if(Math.abs(getP().x) > getMaxVelocity()){
            setP(new MathVector(Math.signum(getP().x)*getMaxVelocity(),getP().y));
        }
        if (getP().y > 0){
            if (getP().y > 3*getMaxVelocity()){
                setP(new MathVector(getP().x, Math.signum(getP().y)*getMaxVelocity()));
            }
        } else if (getP().y < 0){
            if (-getP().y > getMaxVelocity()){
                setP(new MathVector(getP().x, Math.signum(getP().y)*getMaxVelocity()));
            }
        }
        manageConstraints();
        if (!isGhost()) {
            manageCollisions(getMargin());
//            manageCollisionWithOtherObjects();
        }
        updatePosition();
        updateFrame();
        if (isOnFloor()){
            p.x *= friction;
            if (Math.abs(p.x) < 1){
                p.x = 0;
            }
        }
    }

    protected boolean checkCollisionWithOtherObjects(double x, double y) {
        if (getCollisionPriority() != 0) {
            for (GameDynamicObject dynamicObject : MyActivity.dynamicObjects) {
                GameShape bounds = dynamicObject.getBounds();
                MathVector point = new MathVector(x,y);
                if (!dynamicObject.equals(this) && dynamicObject.distanceTo(point) < bounds.getIntersectMagnitude() && !dynamicObject.isGhost()) {
                    if (bounds.contains(point)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void manageConstraints() {
        for (Constraint constraint : constraints){
            constraint.check(this);
        }
    }

    public void updatePosition() {
        if (!Double.isNaN(p.x) && !Double.isNaN(p.y)) {
            setxPosInRoom((int) (getxPosInRoom() + p.x));
            setyPosInRoom((int) (getyPosInRoom() + p.y));
        }
    }

    public boolean manageCollisions(int margin){
        boolean up = manageUpCollision(margin);
        boolean down = manageDownCollision(margin);
        boolean right = manageRightCollision(margin);
        boolean left = manageLeftCollision(margin);
        if (right && left){
            getOutRightLeft();
        } else if (right && !left){
            getOutOfRightWall(margin);
        } else if (left && !right){
            getOutOfLeftWall(margin);
        }
        if (up && down){
            getOutUpDown();
        } else if (up && !down){
            p.y += MyActivity.TILE_WIDTH / 100;
        } else if (!up && down){
            if (makeSureNotUnderground){
                raiseAboveGround(margin);
            }
        }
        return up || down || right || left;
    }

    public double checkUpCollision(int margin){
        for (double x = getxPosInRoom()-margin;x < getxPosInRoom() + margin;x++){
            for (double y = getyPosInRoom() - getHeight()/2 ; y > getyPosInRoom() - getHeight()/2 + p.y ; y--) {
                if(MyActivity.isInRoom(x, y)) {
                    if (checkCollisionWithOtherObjects(x,y)) {
                        return 0f;
                    } else {
                        int pixel = MyActivity.canvas.mapBitmap.getPixel((int) x, (int) y);
                        if (Color.alpha(pixel) == 255) {
                            return y - (getyPosInRoom() - getHeight() / 2);
                        }
                    }
                } else {
                    return 0f;
                }
            }
        }
        return 1f;
    }

    private boolean manageUpCollision(int margin){
        if(p.y < 0){
            double checkup = checkUpCollision(margin);
            if (checkup <= 0) p.y = checkup;
            return true;
        }
        return false;
    }

    public boolean onFloor(int margin){
        for (double x = getxPosInRoom()-margin;x < getxPosInRoom() + margin;x++){
            double y = getyPosInRoom() + getHeight()/2 + 3;
            if(MyActivity.isInRoom(x, y)) {
                int pixel = MyActivity.canvas.mapBitmap.getPixel((int) x, (int) y);
                if (Color.alpha(pixel) == 255) {
                    onFloor = true;
                    return true;
                }
            }
        }
        onFloor = false;
        return false;
    }

    private double checkDownCollision(int margin){
        for (double x = getxPosInRoom()-margin;x < getxPosInRoom() + margin;x++){
            for (double y = getyPosInRoom() + getHeight()/2 ; y < getyPosInRoom() + getHeight()/2 + p.y ; y++) {
                if(MyActivity.isInRoom(x, y)) {
                    if (checkCollisionWithOtherObjects(x,y)){
                        setOnFloor(true);
                        return 0f;
                    } else {
                        int pixel = MyActivity.canvas.mapBitmap.getPixel((int) x, (int) y);
                        if (Color.alpha(pixel) == 255) {
                            return y - getyPosInRoom() - getHeight() / 2;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private boolean manageDownCollision(int margin){
        if (onFloor(margin)){
            if (p.y > 0){
                manageFloorColliding();
                return true;
            }
        } else {
            p.y += getMass();
            if (p.y >= 0) {
                double checkDown = checkDownCollision(margin);
                if (checkDown >= 0f){
                    p.y = checkDown;
                    return false;
                }
            }
        }
        if (p.y > MAXIMUM_VERTICAL_MOMENTUM){
            p.y = MAXIMUM_VERTICAL_MOMENTUM;
        }
        return false;
    }

    private void raiseAboveGround(int margin) {
        boolean raised = false;
        int y = (int) (getyPosInRoom() + getHeight()/2);
        while (!raised && y > getyPosInRoom() - getHeight()/2) {
            y--;
            for (int x = (int) getxPosInRoom() - margin; x < (int)getxPosInRoom() + margin; x++) {
                if (MyActivity.isInRoom(x,y)) {
                    int pixel = MyActivity.canvas.mapBitmap.getPixel(x, y);
                    if (Color.alpha(pixel) == 255) {
                        break;
                    }
                    if (x == (int) getxPosInRoom() + margin - 1) {
                        raised = true;
                    }
                }
            }
        }
        if (raised){
            p.y = y - getHeight()/2 - getyPosInRoom();
        }
    }

    private void getOutOfCeiling (int margin) {
        boolean out = false;
        int y = (int) (getyPosInRoom() - getHeight()/2);
        while (!out && y < getyPosInRoom() + getHeight()/2) {
            y++;
            for (int x = (int) getxPosInRoom() - margin; x < (int)getxPosInRoom() + margin; x++) {
                if (MyActivity.isInRoom(x,y)) {
                    int pixel = MyActivity.canvas.mapBitmap.getPixel(x, y);
                    if (Color.alpha(pixel) == 255) {
                        break;
                    }
                    if (x == (int) getxPosInRoom() + margin - 1) {
                        out = true;
                    }
                }
            }
        }
        if (out){
            p.y = y + getHeight()/2 - getyPosInRoom();
        }
    }

    private void getOutOfRightWall(int margin) {
        boolean out = false;
        int x = (int) (getxPosInRoom() + getWidth()/2);
        while (!out && x > getxPosInRoom() - getWidth()/2) {
            x--;
            for (int y = (int) getyPosInRoom() - margin; y < (int)getyPosInRoom() - margin/5; y++) {
                if (MyActivity.isInRoom(x,y)) {
                    int pixel = MyActivity.canvas.mapBitmap.getPixel(x, y);
                    if (Color.alpha(pixel) == 255) {
                        break;
                    }
                    if (y == (int) getyPosInRoom() + margin - 1) {
                        out = true;
                    }
                }
            }
        }
        if (out){
            p.x = x - getWidth()/2 - getxPosInRoom();
        }
    }

    private void getOutOfLeftWall(int margin) {
        boolean out = false;
        int x = (int) (getxPosInRoom() - getWidth()/2);
        while (!out && x < getxPosInRoom() + getWidth()/2) {
            x++;
            for (int y = (int) getyPosInRoom() - margin; y < (int)getyPosInRoom() - margin/5; y++) {
                if (MyActivity.isInRoom(x, y)) {
                    int pixel = MyActivity.canvas.mapBitmap.getPixel(x, y);
                    if (Color.alpha(pixel) == 255) {
                        break;
                    }
                    if (y == (int) getyPosInRoom() + margin - 1) {
                        out = true;
                    }
                }
            }
        }
        if (out){
            p.x = x + getWidth()/2 - getxPosInRoom();
        }
    }

    private void getOutRightLeft (){
        int x = getWidth()/2;
        int y = (int) getyPosInRoom();
        while (x < getWidth()){
            x++;
            if (MyActivity.isInRoom(getxPosInRoom()+x,y)){
                int pixelRight = MyActivity.canvas.mapBitmap.getPixel((int) (getxPosInRoom()+x), y);
                if (Color.alpha(pixelRight) != 255) {
                    p.x = x + getWidth()/2 - getxPosInRoom();
                    break;
                }
            }
            if (MyActivity.isInRoom(getxPosInRoom()-x,y)){
                int pixelLeft = MyActivity.canvas.mapBitmap.getPixel((int) (getxPosInRoom()-x), y);
                if (Color.alpha(pixelLeft) != 255) {
                    p.x = x - getWidth()/2 - getxPosInRoom();
                    break;
                }
            }
        }
    }

    private void getOutUpDown (){
        int y = getHeight()/2;
        int x = (int) getxPosInRoom();
        while (y < getHeight()){
            y++;
            if (MyActivity.isInRoom(x,getyPosInRoom() + y)){
                int pixelDown = MyActivity.canvas.mapBitmap.getPixel(x, (int) (getyPosInRoom() + y));
                if (Color.alpha(pixelDown) != 255) {
                    p.y = y + getHeight()/2 - getyPosInRoom();
                    break;
                }
            }
            if (MyActivity.isInRoom(x,getyPosInRoom() - y)){
                int pixelUp = MyActivity.canvas.mapBitmap.getPixel(x, (int) (getyPosInRoom() - y));
                if (Color.alpha(pixelUp) != 255) {
                    p.y = y - getHeight()/2 - getyPosInRoom();
                    break;
                }
            }
        }
    }


    protected void manageFloorColliding() {
        if (p.y > 0) {
            p.y = 0;
        }
    }

    private double checkRightCollision(int margin){
        for (double y = getyPosInRoom()-margin;y < getyPosInRoom() + margin;y++){
            for (double x = getxPosInRoom()+getWidth()/2 ; x < getxPosInRoom() + getWidth()/2 + p.x ; x++) {
                if(MyActivity.isInRoom(x, y)) {
                    if (checkCollisionWithOtherObjects(x,y)){
                        return 0f;
                    } else {
                        int pixel = MyActivity.canvas.mapBitmap.getPixel((int) x, (int) y);
                        if (Color.alpha(pixel) == 255) {
                            return x - getxPosInRoom() - getWidth() / 2;
                        }
                    }
                } else {
                    return 0;
                }
            }
        }
        return -1;
    }

    private boolean manageRightCollision(int margin){
        if(p.x > 0){
            double checkRight = checkRightCollision(margin);
            if (checkRight >= 0){
                p.x = checkRight;
                return true;
            }
        }
        return false;
    }

    private double checkLeftCollision(int margin){
        for (double y = getyPosInRoom()-margin;y < getyPosInRoom() + margin;y++){
            for (double x = getxPosInRoom()-getWidth()/2 ; x > getxPosInRoom() - getWidth()/2 + p.x ; x--) {
                if(MyActivity.isInRoom(x, y)) {
                    if (checkCollisionWithOtherObjects(x,y)){
                        return 0f;
                    } else {
                        int pixel = MyActivity.canvas.mapBitmap.getPixel((int) x, (int) y);
                        if (Color.alpha(pixel) == 255) {
                            return x - getxPosInRoom() + getWidth() / 2;
                        }
                    }
                } else {
                    return 0;
                }
            }
        }
        return 1;
    }

    private boolean manageLeftCollision(int margin){
        if(p.x < 0){
            double checkLeft = checkLeftCollision(margin);
            if (checkLeft <= 0){
                p.x = checkLeft;
                return true;
            }
        }
        return false;
    }

    public boolean inContactWith(GameDynamicObject object){
        return this.getBounds().intersect(object.getBounds());
    }

    public boolean inFutureContactWith(GameDynamicObject object){
        return this.getFutureBounds().intersect(object.getBounds());
    }

    public GameShape getFutureBounds(){
        return new RectShape(getFuturePositionInRoom().x,getFuturePositionInRoom().y,getWidth(),getHeight(),false, false);
    }

    public boolean inContactWithMap(int margin){
        for (int x = (int)getxPosInRoom() - margin ; x < getxPosInRoom() + margin ; x++){
            if (MyActivity.isInRoom(x, getyPosInRoom() - getHeight()/2)) {
                int pixel = MyActivity.canvas.mapBitmap.getPixel(x, (int) (getyPosInRoom() - getHeight() / 2));
                if (Color.alpha(pixel) == 255) {
                    return true;
                }
            }
            if (MyActivity.isInRoom(x, getyPosInRoom() + getHeight()/2)) {
                int pixel = MyActivity.canvas.mapBitmap.getPixel(x, (int) (getyPosInRoom() + getHeight() / 2));
                if (Color.alpha(pixel) == 255) {
                    return true;
                }
            }
        }
        for (int y = (int)getyPosInRoom() - margin ; y < getyPosInRoom() + margin ; y++){
            if (MyActivity.isInRoom(getxPosInRoom() - getWidth() / 2, y)) {
                {
                    int pixel = MyActivity.canvas.mapBitmap.getPixel((int) (getxPosInRoom() - getWidth() / 2), y);
                    if (Color.alpha(pixel) == 255) {
                        return true;
                    }
                }
            }
            if (MyActivity.isInRoom(getxPosInRoom() + getWidth() / 2, y)) {
                int pixel = MyActivity.canvas.mapBitmap.getPixel((int) (getxPosInRoom() + getWidth() / 2), y);
                if (Color.alpha(pixel) == 255) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean inFutureContactWithMap(int margin){
        for (int x = (int)getFuturePositionInRoom().x - margin ; x < getFuturePositionInRoom().x + margin ; x++){
            if (MyActivity.isInRoom(x, getFuturePositionInRoom().y - getHeight()/2)) {
                int pixel = MyActivity.canvas.mapBitmap.getPixel(x, (int) (getFuturePositionInRoom().y - getHeight() / 2));
                if (Color.alpha(pixel) == 255) {
                    return true;
                }
            }
            if (MyActivity.isInRoom(x, getFuturePositionInRoom().y + getHeight()/2)) {
                int pixel = MyActivity.canvas.mapBitmap.getPixel(x, (int) (getFuturePositionInRoom().y + getHeight() / 2));
                if (Color.alpha(pixel) == 255) {
                    return true;
                }
            }
        }
        for (int y = (int)getFuturePositionInRoom().y - margin ; y < getFuturePositionInRoom().y + margin ; y++){
            if (MyActivity.isInRoom(getFuturePositionInRoom().x - getWidth() / 2, y)) {
                {
                    int pixel = MyActivity.canvas.mapBitmap.getPixel((int) (getFuturePositionInRoom().x - getWidth() / 2), y);
                    if (Color.alpha(pixel) == 255) {
                        return true;
                    }
                }
            }
            if (MyActivity.isInRoom(getFuturePositionInRoom().x + getWidth() / 2, y)) {
                int pixel = MyActivity.canvas.mapBitmap.getPixel((int) (getFuturePositionInRoom().x + getWidth() / 2), y);
                if (Color.alpha(pixel) == 255) {
                    return true;
                }
            }
        }
        return false;
    }

    public MathVector getFuturePositionInScreen(){
        return getP().applyTo(getPositionInScreen());
    }

    public MathVector getFuturePositionInRoom(){
        return getP().applyTo(getPositionInRoom());
    }

    public MathVector getP() {
        return p;
    }

    public void setP(MathVector p) {
        this.p = p;
    }

    public void addP(MathVector p) {
        this.setP(this.getP().add(p));
    }

    public int getMass() {
        return mass;
    }

    public void setMass(int mass) {
        this.mass = mass;
    }

    public Vector<Constraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(Vector<Constraint> constraints) {
        this.constraints = constraints;
    }

    public void addConstraint(Constraint constraint){
        this.constraints.add(constraint);
    }

    public void removeConstraint(Constraint constraint){
        this.constraints.remove(constraint);
    }
    public void removeAllConstraints(){
        this.constraints.clear();
    }

    public int getCollisionPriority() {
        return collisionPriority;
    }

    public void setCollisionPriority(int collisionPriority) {
        this.collisionPriority = collisionPriority;
    }

    public int getFrame() {
        return frame;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public void updateFrame() {this.frame += MyActivity.FRAME_RATE;}

    public boolean isOnFloor() {
        return onFloor;
    }

    public void setOnFloor(boolean onFloor) {
        this.onFloor = onFloor;
    }

    public double getMaxVelocity() {
        return maxVelocity;
    }

    public void setMaxVelocity(double maxVelocity) {
        this.maxVelocity = maxVelocity;
    }

    public boolean isGhost() {
        return ghost;
    }

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }

    public int getMargin(){
        return getWidth()/1;
    }

    @Override
    public void destroy() {
        super.destroy();
        MyActivity.dynamicObjects.remove(this);
    }
}
