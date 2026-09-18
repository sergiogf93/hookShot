package com.htss.hookshot.game.object.interactables;

import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.object.GameDynamicObject;
import com.htss.hookshot.game.object.enemies.GameEnemy;
import com.htss.hookshot.game.object.interactables.powerups.BombPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.CompassPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.InfiniteJumpsPowerUp;
import com.htss.hookshot.game.object.interactables.powerups.PortalPowerUp;
import com.htss.hookshot.map.Map;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.util.TimeUtil;

import java.util.Random;

/**
 * What defeated enemies leave behind: coins thrown out of them, and sometimes a health drop or a power-up. Everything
 * dropped flies to the character once it's close.
 */
public class Loot {

    // How close the character has to be for loot to fly to it, and how fast it flies
    private static final double PULL_RANGE = MyActivity.TILE_WIDTH * 2.5, PULL_SPEED = MyActivity.TILE_WIDTH * 0.18;
    // Loot isn't pulled for a moment after it's dropped, so it's seen coming out of the enemy
    private static final int SETTLE = (int) TimeUtil.secondsToUpdates(0.35);
    // How far apart a health drop and a power-up dropped together are, and how much room loot needs around it
    private static final double APART = MyActivity.TILE_WIDTH * 0.5, ROOM = MyActivity.TILE_WIDTH * 0.18;

    private static final Random random = new Random();

    // The given coins, a health drop with the first chance and a power-up with the second, each from 0 to 1
    public static void drop(double x, double y, int coins, double healthChance, double powerUpChance) {
        MathVector at = outOfRock(x, y);
        for (int i = 0; i < coins; i++) {
            new Coin(at.x, at.y);
        }
        boolean health = random.nextDouble() < healthChance, powerUp = random.nextDouble() < powerUpChance;
        double apart = (health && powerUp) ? APART : 0;
        if (health) {
            MathVector spot = outOfRock(at.x - apart, at.y);
            new HealthDrop(spot.x, spot.y, true, false).setDropped();
        }
        if (powerUp) {
            MathVector spot = outOfRock(at.x + apart, at.y);
            dropPowerUp(spot.x, spot.y);
        }
    }

    // The closest spot with room around it to the given one, looking up first, where loot dropped by an enemy against
    // the rock or in it comes out. The spot itself if there's none close
    private static MathVector outOfRock(double x, double y) {
        double step = MyActivity.TILE_WIDTH / 10.0;
        for (double distance = 0; distance <= MyActivity.TILE_WIDTH * 2; distance += step) {
            // Up, then further and further to either side, and down last
            for (int i = 0; i < 16; i++) {
                double angle = -Math.PI / 2 + ((i % 2 == 0) ? 1 : -1) * ((i + 1) / 2) * Math.PI / 8;
                double spotX = x + Math.cos(angle) * distance, spotY = y + Math.sin(angle) * distance;
                if (hasRoom(spotX, spotY)) {
                    return new MathVector(spotX, spotY);
                }
            }
        }
        return new MathVector(x, y);
    }

    private static boolean hasRoom(double x, double y) {
        return !GameEnemy.isRock(x, y) && !GameEnemy.isRock(x - ROOM, y) && !GameEnemy.isRock(x + ROOM, y)
                && !GameEnemy.isRock(x, y - ROOM) && !GameEnemy.isRock(x, y + ROOM);
    }

    // A power-up of any kind, sized like the ones placed in the cave
    private static void dropPowerUp(double x, double y) {
        int size = (int) (Map.SQUARE_SIZE * 0.8);
        switch (random.nextInt(4)) {
            case 0:
                new PortalPowerUp(x, y, (int) Map.SQUARE_SIZE / 2, true, false).setDropped();
                break;
            case 1:
                new CompassPowerUp(x, y, size, true, false).setDropped();
                break;
            case 2:
                new BombPowerUp(x, y, size, true, false).setDropped();
                break;
            default:
                new InfiniteJumpsPowerUp(x, y, (int) (Map.SQUARE_SIZE * 0.9), size, true, false).setDropped();
        }
    }

    // Moves dropped loot towards the character once it's close, and from then on however far the character gets.
    // Tells whether it's flying to the character
    public static boolean pull(GameDynamicObject loot, boolean flying) {
        if (loot.getFrame() < SETTLE) {
            return false;
        }
        MathVector toCharacter = new MathVector(loot.getPositionInRoom(), MyActivity.character.getPositionInRoom());
        double distance = toCharacter.magnitude();
        if (!flying && distance > PULL_RANGE) {
            return false;
        }
        if (distance > 0) {
            loot.setPositionInRoom(toCharacter.scaled(Math.min(1, PULL_SPEED / distance)).applyTo(loot.getPositionInRoom()));
        }
        return true;
    }
}
