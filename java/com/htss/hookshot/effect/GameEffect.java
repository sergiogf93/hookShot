package com.htss.hookshot.effect;

import android.graphics.Canvas;

/**
 * Created by Sergio on 28/08/2016.
 */
public abstract class GameEffect {

    public abstract void drawEffectAndUpdate(Canvas canvas);

    public abstract boolean isFinished();
}
