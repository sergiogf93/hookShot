package com.htss.hookshot.game.animation;

import com.htss.hookshot.game.object.MainCharacter;

/**
 * Created by Sergio on 24/08/2016.
 */
public class MainCharacterAnimation {

    public static double getFistAnimatedY(int frame){
        return MainCharacter.BODY_RADIUS/8*Math.sin(2*Math.PI*frame/50);
    }

    public static double getFistAnimatedMovingX(int frame) {
        return MainCharacter.BODY_RADIUS/2*Math.sin(2*Math.PI*frame/20);
    }

    public static double getFootAnimatedMovingX(int frame) {
        return MainCharacter.BODY_RADIUS/4*Math.cos(2*Math.PI*frame/20);
    }
}
