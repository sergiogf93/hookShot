package com.htss.hookshot.persistence;

import com.htss.hookshot.game.MyActivity;

/**
 * Created by Sergio on 12/06/2017.
 */
public class GameStrings {

    // The button that uses a power, and where the powers are picked, depend on the controls in use
    private static String useButton() {
        return (MyActivity.controls == MyActivity.CONTROLS_CLASSIC) ? "( B )" : "( USE )";
    }

    private static String powerPicking() {
        switch (MyActivity.controls) {
            case MyActivity.CONTROLS_CLASSIC:
                return "You can select the powers from the menu.";
            case MyActivity.CONTROLS_TWIN:
            case MyActivity.CONTROLS_TWIN_2:
                return "Pick a power in the column on the right, without stopping the game.";
            default:
                return "Pick a power in the row above the buttons, without stopping the game.";
        }
    }

    public static String getTextString() {
        return "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. ";
    }

    public static String getNewGameStringAdvice(){
        return "Do you want to skip the tutorial messages?";
    }

    public static String getBasicControlsAdvice(){
        if (MyActivity.isTwin()) {
            return "Use the joystick on the bottom left to move, and push it up to jump.";
        }
        return "Use the joystick on the bottom left to move, and push it up to jump. The button ( A ) jumps too.";
    }

    public static String getHookAdvice1(){
        if (MyActivity.isTwin()) {
            return "Drag the stick on the right to aim the chain. The arrow on you shows where it goes, and turns gold where the hook can grip. Let go of the stick to shoot.";
        }
        return "Tap somewhere on the screen to shoot the chain. The chain will hook, which will help you move around the cave.";
    }

    public static String getHookAdvice2(){
        if (MyActivity.controls == MyActivity.CONTROLS_TWIN_2) {
            return "Push the joystick towards where the chain is hooked to reel it in, and away from it to let it out. Pushing across the chain swings you. Press ( UNHOOK ) to let go of the chain.";
        }
        if (MyActivity.controls == MyActivity.CONTROLS_TWIN) {
            return "Hold the up arrow beside the right stick to reel the chain in, and the down arrow to let it out. Press ( UNHOOK ) to let go of the chain.";
        }
        if (MyActivity.controls == MyActivity.CONTROLS_CLASSIC) {
            return "Keep your finger on the screen after shooting the chain to reel it in, and lift it to let go and fly on. Hold the button ( E ) to let chain out, or press ( B ) to let go of the chain.";
        }
        return "Keep your finger on the screen after shooting to reel the chain in, and lift it to let go and fly on. Slide your finger instead to pay the chain out or take it in, and lifting after a slide keeps you hooked.";
    }

    public static String getHookAdvice3() {
        if (MyActivity.controls == MyActivity.CONTROLS_TWIN_2) {
            return "Tap the right stick twice quickly to zip up the chain. Zipping is helpful when you want to move faster.";
        }
        if (MyActivity.controls == MyActivity.CONTROLS_TWIN) {
            return "Press the up arrow twice quickly to zip up the chain. Zipping is helpful when you want to move faster.";
        }
        return "If you tap twice on the screen you will zip up the chain. Zipping is helpful when you want to move faster.";
    }

    public static String getPortalAdvice1() {
        return "You picked up the portal power. " + powerPicking() + " The portals allow you to move around the cave.";
    }

    public static String getPortalAdvice2() {
        return "Use the " + useButton() + " button to place a portal. You need to place two portals in order to travel between them.";
    }

    public static String getPortalAdvice3() {
        return "Now that two portals have been placed, you can travel between them by standing in front of one of them and pressing " + useButton() + ".";
    }

    public static String getCompassAdvice1() {
        return "You picked up the compass power. " + powerPicking() + " The compass will show you the direction to the different interests in the cave.";
    }

    public static String getBombsAdvice1() {
        return "You picked up the explosive power. " + powerPicking() + " The explosives will allow you to open new passages.";
    }

    public static String getBombsAdvice2() {
        return "Use the " + useButton() + " button to create an explosion. You have up to five explosions for each power up.";
    }

    public static String getInfiniteJumpsAdvice1() {
        return "You picked up the swiftness power. " + powerPicking() + " It lets you move faster around the cave, and jump in mid air with ( A ), for a while.";
    }


}
