package com.htss.hookshot.executions;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.advices.HUDBombsAdvice;
import com.htss.hookshot.game.hud.advices.HUDCompassAdvice;
import com.htss.hookshot.game.hud.advices.HUDJumpsAdvice;
import com.htss.hookshot.game.hud.advices.HUDPortalsAdvice;
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
    private int portals = 0, compass = 0, bombs = 0, jumps = 0, explosionsUsed = 0, coins = 0;
    private double health = MainCharacter.MAX_HEALTH;

    public LaunchGame() {
    }

    public LaunchGame(Coord entrance, int portals, int compass, int bombs, int jumps, int explosionsUsed, double health, int coins) {
        this.entrance = entrance;
        this.portals = portals;
        this.compass = compass;
        this.bombs = bombs;
        this.jumps = jumps;
        this.explosionsUsed = explosionsUsed;
        this.health = health;
        this.coins = coins;
    }

    @Override
    public double execute() {
        MyActivity.playground = false;
        MyActivity.godMode = false;
        MyActivity.openWorld = false;
        MyActivity.canvas.gameObjects.clear();
        MyActivity.dynamicObjects.clear();
        Particles.clear();
        ScreenShake.clear();

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

        MyActivity.character = new MainCharacter(startPosition.x,startPosition.y);
        MyActivity.cameraFollows = true;
        MyActivity.centerCameraOnCharacter();

        MyActivity.hudElements.clear();
        MyActivity.addControls();
        MyActivity.hudElements.add(MyActivity.pauseButton);
        MyActivity.hudElements.add(MyActivity.status);

        //////////////////
        MyActivity.character.setPowerUp(GamePowerUp.PORTAL, portals);
        MyActivity.character.setPowerUp(GamePowerUp.COMPASS, compass);
        MyActivity.character.setPowerUp(GamePowerUp.BOMB, bombs);
        MyActivity.character.setPowerUp(GamePowerUp.INFINITE_JUMPS, jumps);
        MyActivity.character.setExplosionsUsed(explosionsUsed);
        MyActivity.character.setHealth(health);
        MyActivity.character.setCoins(coins);

        MyActivity.advices.add(new HUDPortalsAdvice(MyActivity.canvas.myActivity.portalsAdvice));
        MyActivity.advices.add(new HUDCompassAdvice(MyActivity.canvas.myActivity.compassAdvice));
        MyActivity.advices.add(new HUDBombsAdvice(MyActivity.canvas.myActivity.bombAdvice));
        MyActivity.advices.add(new HUDJumpsAdvice(MyActivity.canvas.myActivity.jumpsAdvice));
        //////////////////

        MyActivity.canvas.myActivity.save();

        return 0;
    }
}
