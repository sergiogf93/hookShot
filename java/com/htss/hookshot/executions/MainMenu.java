package com.htss.hookshot.executions;

import android.graphics.Color;

import com.htss.hookshot.effect.FadeEffect;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.advices.HUDAdvice;
import com.htss.hookshot.game.hud.HUDText;
import com.htss.hookshot.game.hud.advices.HUDNewGameAdvice;
import com.htss.hookshot.game.object.MainCharacter;
import com.htss.hookshot.game.object.hook.Hook;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.map.Coord;
import com.htss.hookshot.math.MathVector;
import com.htss.hookshot.persistence.GameStrings;

/**
 * Created by Sergio on 11/06/2017.
 */
public class MainMenu implements Execution {
    @Override
    public double execute() {
        MyActivity.character = null;
        MyActivity.currentMap = null;
        MyActivity.canvas.dx = 0;
        MyActivity.canvas.dy = 0;
        MyActivity.hudElements.clear();
        MyActivity.canvas.gameObjects.clear();
        MyActivity.dynamicObjects.clear();
        MyActivity.enemies.clear();
        MyActivity.advices.clear();
        HUDText newGame = new HUDText(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2 - MyActivity.canvas.fontSize * 3, true, "NEW GAME", MyActivity.TILE_WIDTH * 8 / 10, new Execution() {
            @Override
            public double execute() {
                MyActivity.gameEffects.add( new FadeEffect(new LaunchGame(), new Execution() {
                    @Override
                    public double execute() {
                        MyActivity.advices.add(new HUDNewGameAdvice(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2, (int) (MyActivity.screenWidth * 0.7), (int) (MyActivity.TILE_WIDTH * 0.3)));
                        return 0;
                    }
                }));
                return 0;
            }
        });
        MyActivity.hudElements.add(newGame);
        int yExitButton = -1;
        if (MyActivity.canvas.myActivity.seed != -1) {
            yExitButton = 1;
            HUDText continueButton = new HUDText(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2 - MyActivity.canvas.fontSize, true, "CONTINUE", MyActivity.TILE_WIDTH * 8 / 10, new Execution() {
                @Override
                public double execute() {
                    Coord entrance = new Coord(Integer.parseInt(MyActivity.canvas.myActivity.entranceString.split(" ")[0]), Integer.parseInt(MyActivity.canvas.myActivity.entranceString.split(" ")[1]));
                    MyActivity.gameEffects.add(new FadeEffect(new LaunchGame(entrance, MyActivity.canvas.myActivity.portals, MyActivity.canvas.myActivity.compass, MyActivity.canvas.myActivity.bombs, MyActivity.canvas.myActivity.jumps, MyActivity.canvas.myActivity.explosionsUsed, MyActivity.canvas.myActivity.health)));
                    return 0;
                }
            });
            MyActivity.hudElements.add(continueButton);
        }
        HUDText exitGame = new HUDText(MyActivity.screenWidth/2, MyActivity.screenHeight / 2 + yExitButton * MyActivity.canvas.fontSize, true, "EXIT GAME", MyActivity.TILE_WIDTH * 8 /10, new Execution() {
            @Override
            public double execute() {
                System.exit(0);
                return 0;
            }
        });
        MyActivity.hudElements.add(exitGame);

        MainCharacter c = new MainCharacter(0, MyActivity.screenHeight - MyActivity.TILE_WIDTH / 2);
        c.setP(new MathVector(MyActivity.TILE_WIDTH / 50, 0));
        c.setOnFloor(true);

        int n = MyActivity.screenHeight / Hook.SEPARATION - 5;
        MyActivity.character = new MainCharacter(MyActivity.screenWidth / 2, n * Hook.SEPARATION);
        Hook hook = new Hook(MyActivity.screenWidth / 2, n * Hook.SEPARATION, n, Color.GRAY, MyActivity.character, new MathVector(0, 0));
        MyActivity.character.setHook(hook);
        hook.hook(new MathVector(MyActivity.screenWidth / 2, MyActivity.screenHeight / 10 ));

        MyActivity.paused = false;
        return 0;
    }
}
