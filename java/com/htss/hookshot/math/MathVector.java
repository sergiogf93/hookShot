package com.htss.hookshot.math;

import android.graphics.Point;

import com.htss.hookshot.game.MyActivity;

/**
 * Created by Sergio on 14/03/2016.
 */
public class MathVector {

    public double x,y;

    public MathVector(double x, double y){
        this.x = x;
        this.y = y;
    }

    public MathVector(double x, double y, double x2, double y2){
        this.x = x2 - x;
        this.y = y2 - y;
    }

    public MathVector(MathVector p1, MathVector p2){
        this.x = p2.x - p1.x;
        this.y = p2.y - p1.y;
    }

    public MathVector() {

    }

    public double distanceTo(MathVector v){
        return Math.sqrt((x-v.x)*(x-v.x)+(y-v.y)*(y-v.y));
    }

    public double magnitude(){
        return Math.sqrt(x*x+y*y);
    }

    public void normalize(){
        double m = magnitude();
        this.x = this.x/m;
        this.y = this.y/m;
    }

    public MathVector normalized(){
        double m = magnitude();
        return new MathVector(this.x/m,this.y/m);
    }

    public double dotProduct(MathVector v){
        return x*v.x+y*v.y;
    }

    public double angleDeg(MathVector other){ // [0,180]
        double a = (Math.atan2(y,x) - Math.atan2(other.y,other.x))*180/Math.PI;
        return Math.min(Math.abs(a),360 - Math.abs(a));
    }

    public double signedAngleDeg(MathVector other){
        double a = (Math.atan2(y,x) - Math.atan2(other.y,other.x))*180/Math.PI;
        return a;
    }

    public void scale(double s) {
        this.x *= s;
        this.y *= s;
    }

    public MathVector scaled(double s){
        return new MathVector(x*s,y*s);
    }

    public void rotateDeg(double angle){
        angle = angle*Math.PI/180f;
        double xRotated = Math.cos(angle)*x - Math.sin(angle)*y;
        double yRotated = Math.sin(angle)*x + Math.cos(angle)*y;
        this.x = xRotated;
        this.y = yRotated;
    }

    public MathVector rotatedDeg(double angle){
        angle = angle*Math.PI/180f;
        double xRotated = Math.cos(angle)*x - Math.sin(angle)*y;
        double yRotated = Math.sin(angle)*x + Math.cos(angle)*y;
        return new MathVector(xRotated,yRotated);
    }

    public MathVector applyTo(Point p){
        return new MathVector(p.x + x, p.y + y);
    }

    public MathVector applyTo(MathVector p){
        return new MathVector(p.x + x, p.y + y);
    }

    public MathVector getUnitVector(){
        MathVector uVector = new MathVector(this.x,this.y);
        uVector.normalize();
        return uVector;
    }

    public boolean isNull(){
        return x == 0f && y == 0f;
    }

    public MathVector add(MathVector p) {
        return new MathVector(this.x + p.x, this.y + p.y);
    }

    public void rescale(double scale) {
        this.normalize();
        this.scale(scale);
    }

    public MathVector rescaled(double scale) {
        double m = this.magnitude();
        return new MathVector(scale*x/m,scale*y/m);
    }

    public MathVector screenToRoom(){
        return new MathVector(this.x - MyActivity.canvas.dx, this.y - MyActivity.canvas.dy);
    }

    public MathVector roomToScreen(){
        return new MathVector(this.x + MyActivity.canvas.dx, this.y + MyActivity.canvas.dy);
    }

    public MathVector getNormal(){
        return new MathVector(-this.y,this.x);
    }

    public void reflect(MathVector axis) {
        axis.normalize();
        MathVector n = axis.getNormal();
        if (this.angleDeg(n) < this.angleDeg(n.scaled(-1))){
            n.scale(-1);
        }
        n.scale(Math.abs(2*this.dotProduct(n)));
        this.x += n.x;
        this.y += n.y;
    }

    public Point toPoint(){
        return new Point((int)x,(int)y);
    }
}
