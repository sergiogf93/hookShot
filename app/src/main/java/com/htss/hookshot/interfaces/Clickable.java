package com.htss.hookshot.interfaces;

/**
 * Created by Sergio on 03/08/2016.
 */
public interface Clickable {

    public void press(double x, double y, int id, int index);
    public void reset();
    public boolean isOn();
    public boolean isClickable();
    public void setClickable(boolean bool);
    public boolean pressed(double x, double y);
    public int getTouchId();
    public int getTouchIndex();

}
