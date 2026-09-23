package com.htss.hookshot.executions;

import android.graphics.Color;

import com.htss.hookshot.effect.FadeEffect;
import com.htss.hookshot.effect.Particles;
import com.htss.hookshot.effect.ScreenShake;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.advices.HUDAdvice;
import com.htss.hookshot.game.hud.HUDText;
import com.htss.hookshot.game.hud.HUDTitle;
import com.htss.hookshot.game.hud.advices.HUDNewGameAdvice;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.map.Coord;
import com.htss.hookshot.map.World;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.persistence.GameStrings;

/**
 * Created by Sergio on 11/06/2017.
 */
public class MainMenu implements Execution {
    @Override
    public double execute() {
        // What the character has and changed in the pieces still around, which go now, and where it is. Unless it died,
        // which takes it back to where it was last saved
        if (MyActivity.openWorld && MyActivity.character != null) {
            MyActivity.canvas.myActivity.saveOpenWorld(MyActivity.isCharacterAlive());
        }
        MyActivity.character = null;
        MyActivity.currentMap = null;
        World.clear();
        MyActivity.canvas.dx = 0;
        MyActivity.canvas.dy = 0;
        MyActivity.hudElements.clear();
        MyActivity.canvas.gameObjects.clear();
        MyActivity.dynamicObjects.clear();
        Particles.clear();
        ScreenShake.clear();
        MyActivity.enemies.clear();
        MyActivity.advices.clear();
        MyActivity.playground = false;
        MyActivity.godMode = false;
        MyActivity.openWorld = false;
        MyActivity.canvas.myActivity.load();
        // The items sit a bit below the middle, leaving room for the title
        MyActivity.hudElements.add(new HUDTitle(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2 - MyActivity.canvas.fontSize * 5, MyActivity.canvas.fontSize * 26 / 10));
        // One under the other, from a bit above the middle, leaving room for the title
        int step = MyActivity.canvas.fontSize * 7 / 4, y = MyActivity.screenHeight / 2 - MyActivity.canvas.fontSize * 5 / 2;
        HUDText newGame = new HUDText(MyActivity.screenWidth / 2, y, true, "NEW GAME", MyActivity.TILE_WIDTH * 8 / 10, new Execution() {
            @Override
            public double execute() {
                MyActivity.gameEffects.add( new FadeEffect(Color.BLACK, new LaunchGame(), new Execution() {
                    @Override
                    public double execute() {
                        MyActivity.advices.add(new HUDNewGameAdvice((int) (MyActivity.TILE_WIDTH * 0.3)));
                        return 0;
                    }
                }));
                return 0;
            }
        });
        MyActivity.hudElements.add(newGame);
        if (MyActivity.canvas.myActivity.seed != -1) {
            y += step;
            HUDText continueButton = new HUDText(MyActivity.screenWidth / 2, y, true, "CONTINUE", MyActivity.TILE_WIDTH * 8 / 10, new Execution() {
                @Override
                public double execute() {
                    Coord entrance = new Coord(Integer.parseInt(MyActivity.canvas.myActivity.entranceString.split(" ")[0]), Integer.parseInt(MyActivity.canvas.myActivity.entranceString.split(" ")[1]));
                    MyActivity.gameEffects.add(new FadeEffect(Color.BLACK, new LaunchGame(entrance, MyActivity.canvas.myActivity.portals, MyActivity.canvas.myActivity.compass, MyActivity.canvas.myActivity.bombs, MyActivity.canvas.myActivity.jumps, MyActivity.canvas.myActivity.explosionsUsed, MyActivity.canvas.myActivity.health, MyActivity.canvas.myActivity.coins)));
                    return 0;
                }
            });
            MyActivity.hudElements.add(continueButton);
        }
        // One endless cave to roam, which carries on from where it was left
        y += step;
        HUDText openWorldButton = new HUDText(MyActivity.screenWidth / 2, y, true, "OPEN WORLD", MyActivity.TILE_WIDTH * 8 / 10, new Execution() {
            @Override
            public double execute() {
                MyActivity.gameEffects.add(new FadeEffect(Color.BLACK, new LaunchOpenWorld(false)));
                return 0;
            }
        });
        MyActivity.hudElements.add(openWorldButton);
        y += step;
        // A cave to try the controls in, which never touches the saved game
        HUDText playgroundButton = new HUDText(MyActivity.screenWidth / 2, y, true, "PLAYGROUND", MyActivity.TILE_WIDTH * 8 / 10, new Execution() {
            @Override
            public double execute() {
                MyActivity.gameEffects.add(new FadeEffect(Color.BLACK, new LaunchPlayground()));
                return 0;
            }
        });
        MyActivity.hudElements.add(playgroundButton);
        y += step;
        HUDText exitGame = new HUDText(MyActivity.screenWidth/2, y, true, "EXIT GAME", MyActivity.TILE_WIDTH * 8 /10, new Execution() {
            @Override
            public double execute() {
                MyActivity.canvas.myActivity.finish();
                return 0;
            }
        });
        MyActivity.hudElements.add(exitGame);

        double r = Math.random();
        MainCharacter c;
        if (r < 0.5) {
            c = new MainCharacter(0, MyActivity.screenHeight - MyActivity.TILE_WIDTH / 2);
            c.setP(new MathVector(MyActivity.TILE_WIDTH / 50, 0));
            c.setOnFloor(true);
        } else {
            c = new MainCharacter(MyActivity.screenWidth, MyActivity.screenHeight - MyActivity.TILE_WIDTH / 2);
            c.setP(new MathVector(-MyActivity.TILE_WIDTH / 50, 0));
            c.setOnFloor(true);
        }

        int n = MyActivity.screenHeight / Hook.SEPARATION - 5;
        // Hanging to the right, so its chain doesn't cross the menu
        int hangingX = MyActivity.screenWidth * 4 / 5;
        MyActivity.character = new MainCharacter(hangingX, n * Hook.SEPARATION);
        Hook hook = new Hook(hangingX, n * Hook.SEPARATION, n, Color.GRAY);
        MyActivity.character.setHook(hook);
        hook.hook(new MathVector(hangingX, MyActivity.screenHeight / 10));

        MyActivity.paused = false;
        return 0;
    }
}
