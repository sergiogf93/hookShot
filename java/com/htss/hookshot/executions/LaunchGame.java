package com.htss.hookshot.executions;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 30/05/2017.
 */
public class LaunchGame implements Execution {
    @Override
    public double execute() {
        MyActivity.canvas.gameObjects.clear();
        MyActivity.dynamicObjects.clear();

        MyActivity.currentMap = new Map(MyActivity.mapXTiles, MyActivity.mapYTiles, MyActivity.FILL_PERCENT, true, 0);
        MathVector startPosition = MyActivity.currentMap.startPosition();

        while (startPosition.magnitude() == 0){
            MyActivity.canvas.gameObjects.clear();
            MyActivity.dynamicObjects.clear();
            MyActivity.currentMap = new Map(MyActivity.mapXTiles,MyActivity.mapYTiles,MyActivity.FILL_PERCENT,true,0);
            startPosition = MyActivity.currentMap.startPosition();
        }
        MyActivity.canvas.generateMap();

        MyActivity.canvas.dx = (float) -(startPosition.x - MyActivity.screenWidth/2);
        MyActivity.canvas.dy = (float) -(startPosition.y - MyActivity.screenHeight/2);
        MyActivity.canvas.assertMapMargins();

        startPosition = startPosition.roomToScreen();

        MyActivity.character = new MainCharacter(startPosition.x,startPosition.y);

        MyActivity.hudElements.clear();
        MyActivity.addControls();
        MyActivity.hudElements.add(MyActivity.pauseButton);

        if (MyActivity.paused) {
            MyActivity.unpause();
        }

        //////////////////
        MyActivity.character.addPowerUp(GamePowerUp.PORTAL);
        MyActivity.character.addPowerUp(GamePowerUp.COMPASS);
        MyActivity.character.addPowerUp(GamePowerUp.BOMB);
        MyActivity.character.addPowerUp(GamePowerUp.INFINITE_JUMPS);
        //////////////////
        return 0;
    }
}
