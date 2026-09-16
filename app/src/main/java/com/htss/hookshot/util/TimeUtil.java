package com.htss.hookshot.util;

import com.htss.hookshot.game.MyActivity;

/**
 * Created by Sergio on 21/08/2016.
 */
public class TimeUtil {

    // Game objects count time in updates, which is how the game advances, so durations are rounded to whole updates
    public static double secondsToUpdates(double seconds) {
        return Math.round(seconds * MyActivity.UPDATES_PER_SECOND);
    }

}
