package com.htss.hookshot.util;

import com.htss.hookshot.game.MyActivity;

/**
 * Created by Sergio on 21/08/2016.
 */
public class TimeUtil {

    // Game objects count time in frame units, adding FRAME_RATE of them on every update. Durations are rounded to
    // whole updates, as that's how the game advances
    public static double secondsToFrameTime(double seconds) {
        return Math.round(seconds * MyActivity.UPDATES_PER_SECOND) * MyActivity.FRAME_RATE;
    }

}
