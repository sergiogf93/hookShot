package com.htss.hookshot.game.hud;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.htss.hookshot.executions.MainMenu;
import com.htss.hookshot.game.MyActivity;
import com.htss.hookshot.interfaces.Execution;
import com.htss.hookshot.map.Map;

import java.util.Random;
import java.util.Vector;

/**
 * The playground's menu, in place of the pause menu: buttons that place each kind of enemy near the character, and
 * tools to try things out, like handing out powers, healing, clearing the enemies away, not getting hurt and switching
 * the controls. Placing something goes straight back to the game, to see it.
 */
public class HUDPlaygroundMenu extends HUDElement {

    private static final int COLUMNS = 3, ROWS = 5;
    private static final String[] ENEMY_NAMES = {"STALKER", "WORM", "BAT", "SPITTER", "SNIPPER", "BEETLE", "DEEP WORM"};
    private static final int[] ENEMY_KINDS = {Map.SPAWN_STALKER, Map.SPAWN_TERRA_WORM, Map.SPAWN_BAT, Map.SPAWN_SPITTER,
            Map.SPAWN_SNIPPER, Map.SPAWN_BEETLE, Map.SPAWN_DEEP_WORM};

    private final Vector<HUDButton> buttons = new Vector<HUDButton>();
    private final RectF panel = new RectF();
    private final Random random = new Random();
    private int cellWidth, cellHeight, gap;

    public HUDPlaygroundMenu() {
        super(0, 0, 0, 0);
        getPaint().setTypeface(MyActivity.canvas.joystickMonospace);
    }

    public void open() {
        layout();
        buttons.clear();
        int index = 0;
        for (int i = 0; i < ENEMY_NAMES.length; i++) {
            final int kind = ENEMY_KINDS[i];
            addButton(index++, ENEMY_NAMES[i], new Execution() {
                @Override
                public double execute() {
                    MyActivity.currentMap.spawnNear(kind, MyActivity.character.getPositionInRoom(), random);
                    MyActivity.unpause();
                    return 0;
                }
            });
        }
        addButton(index++, "CLEAR", new Execution() {
            @Override
            public double execute() {
                MyActivity.clearEnemies();
                MyActivity.unpause();
                return 0;
            }
        });
        addButton(index++, "HEAL", new Execution() {
            @Override
            public double execute() {
                MyActivity.character.setHealth(MyActivity.character.getMaxHealth());
                MyActivity.unpause();
                return 0;
            }
        });
        addButton(index++, "POWERS", new Execution() {
            @Override
            public double execute() {
                MyActivity.fillPlaygroundPowers();
                MyActivity.unpause();
                return 0;
            }
        });
        final HUDButton god = addButton(index++, godText(), null);
        god.setExecOff(new Execution() {
            @Override
            public double execute() {
                MyActivity.godMode = !MyActivity.godMode;
                god.setText(godText());
                return 0;
            }
        });
        final HUDButton controls = addButton(index++, controlsText(), null);
        controls.setExecOff(new Execution() {
            @Override
            public double execute() {
                MyActivity.setControls((MyActivity.controls + 1) % MyActivity.CONTROL_NAMES.length);
                controls.setText(controlsText());
                return 0;
            }
        });
        addButton(index++, "RESUME", new Execution() {
            @Override
            public double execute() {
                MyActivity.unpause();
                return 0;
            }
        });
        addButton(index, "MAIN MENU", new Execution() {
            @Override
            public double execute() {
                (new MainMenu()).execute();
                return 0;
            }
        });
        MyActivity.hudElements.add(this);
        MyActivity.hudElements.addAll(buttons);
    }

    public void close() {
        MyActivity.hudElements.remove(this);
        MyActivity.hudElements.removeAll(buttons);
    }

    // A grid in the middle of the screen, as wide as fits, filled row by row
    private void layout() {
        gap = MyActivity.TILE_WIDTH / 5;
        cellHeight = (int) (MyActivity.TILE_WIDTH * 0.8);
        cellWidth = Math.min((int) (MyActivity.TILE_WIDTH * 4.2), (MyActivity.screenWidth - MyActivity.TILE_WIDTH * 2 - gap * (COLUMNS - 1)) / COLUMNS);
        int width = cellWidth * COLUMNS + gap * (COLUMNS + 1), height = cellHeight * ROWS + gap * (ROWS + 1);
        setCenter(MyActivity.screenWidth / 2, MyActivity.screenHeight / 2 + cellHeight / 3);
        panel.set(getxCenter() - width / 2f, getyCenter() - height / 2f, getxCenter() + width / 2f, getyCenter() + height / 2f);
    }

    private HUDButton addButton(int index, String text, Execution execution) {
        int column = index % COLUMNS, row = index / COLUMNS;
        int x = (int) (panel.left + gap + column * (cellWidth + gap) + cellWidth / 2);
        int y = (int) (panel.top + gap + row * (cellHeight + gap) + cellHeight / 2);
        HUDButton button = new HUDButton(x, y, cellWidth, cellHeight, text, execution);
        buttons.add(button);
        return button;
    }

    @Override
    public void draw(Canvas canvas) {
        UiStyle.drawPanel(canvas, getPaint(), panel, MyActivity.TILE_WIDTH / 3f);
        getPaint().setTextSize(MyActivity.TILE_WIDTH * 0.4f);
        String title = "PLAYGROUND";
        UiStyle.drawText(canvas, getPaint(), title, getxCenter() - getPaint().measureText(title) / 2, panel.top - MyActivity.TILE_WIDTH * 0.25f, UiStyle.TEXT);
    }

    @Override
    public boolean pressed(double x, double y) {
        return false;
    }

    private static String godText() {
        return MyActivity.godMode ? "GOD: ON" : "GOD: OFF";
    }

    private static String controlsText() {
        return "CONTROLS: " + MyActivity.CONTROL_NAMES[MyActivity.controls];
    }
}
