package com.htss.hookshot.executions;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.math.MathVector;

/**
 * Starts the playground, a cave to try the controls in, with its own menu to place enemies and hand out powers. It
 * never touches the saved game: nothing is saved while in it, and it has no way out to the next level.
 */
public class LaunchPlayground implements Execution {

    @Override
    public double execute() {
        MyActivity.canvas.gameObjects.clear();
        MyActivity.dynamicObjects.clear();
        MyActivity.enemies.clear();
        Particles.clear();
        ScreenShake.clear();
        MyActivity.playground = true;
        MyActivity.godMode = false;

        MyActivity.currentMap = Map.playground();
        MyActivity.canvas.generateMap();
        MathVector start = MyActivity.currentMap.startPosition();
        MyActivity.canvas.dx = (float) -(start.x - MyActivity.screenWidth / 2);
        MyActivity.canvas.dy = (float) -(start.y - MyActivity.screenHeight / 2);
        MyActivity.canvas.assertMapMargins();
        start = start.roomToScreen();
        MyActivity.character = new MainCharacter(start.x, start.y);

        MyActivity.hudElements.clear();
        MyActivity.addControls();
        MyActivity.hudElements.add(MyActivity.pauseButton);
        MyActivity.hudElements.add(MyActivity.status);
        MyActivity.fillPlaygroundPowers();
        return 0;
    }
}
