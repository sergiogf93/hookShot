package com.htss.hookshot.executions;

import android.content.SharedPreferences;

import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.interactables.powerups.GamePowerUp;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.map.Cave;
import com.htss.hookshot.map.World;
import com.htss.hookshot.math.MathVector;

/**
 * Starts the open world: one endless cave to roam in any direction but up past the surface, which gets harder the
 * deeper it goes. It carries on from where the character last was in it, with what it had, unless it's started anew.
 * It's saved apart from the game of caves one after another, which it never touches.
 */
public class LaunchOpenWorld implements Execution {

    private final boolean anew;

    public LaunchOpenWorld(boolean anew) {
        this.anew = anew;
    }

    @Override
    public double execute() {
        MyActivity.canvas.gameObjects.clear();
        MyActivity.dynamicObjects.clear();
        MyActivity.enemies.clear();
        Particles.clear();
        ScreenShake.clear();
        MyActivity.playground = false;
        MyActivity.godMode = false;
        MyActivity.openWorld = true;
        // A new world can be started from the pause menu
        MyActivity.paused = false;

        SharedPreferences preferences = MyActivity.canvas.myActivity.getPreferences(MyActivity.MODE_PRIVATE);
        boolean carryOn = !anew && preferences.contains("OpenSeed");
        long seed = carryOn ? preferences.getLong("OpenSeed", 0) : System.currentTimeMillis();
        MyActivity.canvas.myActivity.openSeed = seed;
        double x = carryOn ? preferences.getFloat("OpenX", 0) : 0, y = carryOn ? preferences.getFloat("OpenY", 0) : 0;
        Cave cave = World.startOpen(seed, anew, x, y);
        // Where it was, as long as that's still open cave it wouldn't be shut inside. It might not be if the holes it dug
        // there were made after the world was last saved
        boolean asLeft = carryOn && !World.isSolid((int) x, (int) y) && !cave.map.isBehindVaultDoor(x, y);
        MathVector start = asLeft ? new MathVector(x, y) : cave.map.getStandingPoint(cave.x, cave.y);
        MyActivity.character = new MainCharacter(start.x, start.y);
        MyActivity.cameraFollows = true;
        MyActivity.centerCameraOnCharacter();

        MyActivity.hudElements.clear();
        MyActivity.addControls();
        MyActivity.hudElements.add(MyActivity.pauseButton);
        MyActivity.hudElements.add(MyActivity.status);

        MyActivity.character.setPowerUp(GamePowerUp.PORTAL, carryOn ? preferences.getInt("OpenPortals", 0) : 0);
        MyActivity.character.setPowerUp(GamePowerUp.COMPASS, carryOn ? preferences.getInt("OpenCompass", 0) : 0);
        MyActivity.character.setPowerUp(GamePowerUp.BOMB, carryOn ? preferences.getInt("OpenBombs", 0) : 0);
        MyActivity.character.setPowerUp(GamePowerUp.INFINITE_JUMPS, carryOn ? preferences.getInt("OpenJumps", 0) : 0);
        MyActivity.character.setExplosionsUsed(carryOn ? preferences.getInt("OpenExplosionsUsed", 0) : 0);
        MyActivity.character.setCoins(carryOn ? preferences.getInt("OpenCoins", 0) : 0);
        MyActivity.canvas.myActivity.saveOpenWorld();
        return 0;
    }
}
