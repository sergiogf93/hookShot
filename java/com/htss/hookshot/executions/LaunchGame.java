package com.htss.hookshot.executions;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.map.Coord;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.math.MathVector;

/**
 * Created by Sergio on 30/05/2017.
 */
public class LaunchGame implements Execution {

    private Coord entrance = null;
    private int portals = 0, compass = 0, bombs = 0, jumps = 0;

    public LaunchGame() {
    }

    public LaunchGame(Coord entrance, int portals, int compass, int bombs, int jumps) {
        this.entrance = entrance;
        this.portals = portals;
        this.compass = compass;
        this.bombs = bombs;
        this.jumps = jumps;
    }

    @Override
    public double execute() {
        MyActivity.canvas.gameObjects.clear();
        MyActivity.dynamicObjects.clear();

        if (entrance == null) { // New Game
            entrance = new Coord(MyActivity.mapXTiles / 2, MyActivity.mapYTiles / 2);
            MyActivity.canvas.myActivity.level = 0;
            MyActivity.canvas.myActivity.seed = System.currentTimeMillis();
        }

        MyActivity.currentMap = new Map(MyActivity.mapXTiles, MyActivity.mapYTiles, MyActivity.FILL_PERCENT, this.entrance);
        MathVector startPosition = MyActivity.currentMap.startPosition();

        while (startPosition.magnitude() == 0){
            MyActivity.canvas.gameObjects.clear();
            MyActivity.dynamicObjects.clear();
            MyActivity.currentMap = new Map(MyActivity.mapXTiles,MyActivity.mapYTiles,MyActivity.FILL_PERCENT, this.entrance);
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
        MyActivity.character.setPowerUp(GamePowerUp.PORTAL, portals);
        MyActivity.character.setPowerUp(GamePowerUp.COMPASS, compass);
        MyActivity.character.setPowerUp(GamePowerUp.BOMB, bombs);
        MyActivity.character.setPowerUp(GamePowerUp.INFINITE_JUMPS, jumps);
        //////////////////
        return 0;
    }
}
