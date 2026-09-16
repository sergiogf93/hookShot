package com.htss.hookshot.game.animation;


/**
 * Created by Sergio on 31/08/2016.
 */
public class StalkerAnimation {

    public static int getAlphaAnimated (int frame){
        int freq = 18;
        int x = frame % freq;
        return (int) Math.abs(255 * Math.sin(Math.PI*x/(2*freq)));
    }


}
