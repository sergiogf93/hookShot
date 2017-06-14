package com.htss.hookshot.executions;

import com.htss.hookshot.effect.FadeEffect;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.game.hud.advices.HUDAdvice;
import com.htss.hookshot.game.hud.HUDText;
import com.htss.hookshot.game.hud.advices.HUDNewGameAdvice;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.map.Coord;
import com.htss.hookshot.persistence.GameStrings;

/**
 * Created by Sergio on 11/06/2017.
 */
public class MainMenu implements Execution {
    @Override
    public double execute() {
        MyActivity.character = null;
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
                    MyActivity.gameEffects.add(new FadeEffect(new LaunchGame(entrance, MyActivity.canvas.myActivity.portals, MyActivity.canvas.myActivity.compass, MyActivity.canvas.myActivity.bombs, MyActivity.canvas.myActivity.jumps)));
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
        return 0;
    }
}
