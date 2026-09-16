package com.htss.hookshot.game;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.htss.hookshot.map.CavePalette;
import com.htss.hookshot.map.CaveTextures;

import java.util.Random;

/**
 * What's drawn around the cave: the background with distant rock and floating dust, and the light around the
 * character that fades into darkness.
 */
public class Atmosphere {

    // How fast the background and the dust move compared to the cave
    private static final float BACKDROP_PARALLAX = 0.35f, DUST_PARALLAX = 0.6f;
    private static final int DUST_COUNT = 40;

    private CavePalette palette;
    private int width, height;
    private float backdropScale;
    private Paint backgroundPaint = new Paint(), backdropPaint = new Paint(Paint.FILTER_BITMAP_FLAG),
            dustPaint = new Paint(Paint.ANTI_ALIAS_FLAG), lightPaint = new Paint();
    private BitmapShader backdropShader;
    private RadialGradient lightShader;
    private Matrix backdropMatrix = new Matrix(), lightMatrix = new Matrix();
    // Dust positions and speeds are fractions of the screen, so they don't depend on its size
    private float[] dustX = new float[DUST_COUNT], dustY = new float[DUST_COUNT],
            dustSpeedX = new float[DUST_COUNT], dustSpeedY = new float[DUST_COUNT], dustRadius = new float[DUST_COUNT];
    private int[] dustAlpha = new int[DUST_COUNT];

    public Atmosphere() {
        Random random = new Random();
        for (int i = 0; i < DUST_COUNT; i++) {
            dustX[i] = random.nextFloat();
            dustY[i] = random.nextFloat();
            dustSpeedX[i] = (random.nextFloat() - 0.5f) * 0.0004f;
            dustSpeedY[i] = -(0.0002f + random.nextFloat() * 0.0006f);
            dustRadius[i] = 0.5f + random.nextFloat();
            dustAlpha[i] = 30 + random.nextInt(70);
        }
    }

    public void drawBehind(Canvas canvas, int level) {
        update(CavePalette.forLevel(level));
        canvas.drawRect(0, 0, width, height, backgroundPaint);
        backdropMatrix.setScale(backdropScale, backdropScale);
        backdropMatrix.postTranslate(GameBoard.dx * BACKDROP_PARALLAX, GameBoard.dy * BACKDROP_PARALLAX);
        backdropShader.setLocalMatrix(backdropMatrix);
        canvas.drawRect(0, 0, width, height, backdropPaint);
        drawDust(canvas);
    }

    public void drawLight(Canvas canvas, float x, float y) {
        lightMatrix.setTranslate(x, y);
        lightShader.setLocalMatrix(lightMatrix);
        canvas.drawRect(0, 0, width, height, lightPaint);
    }

    private void drawDust(Canvas canvas) {
        float radiusUnit = MyActivity.TILE_WIDTH / 40f;
        for (int i = 0; i < DUST_COUNT; i++) {
            dustX[i] = wrap(dustX[i] + dustSpeedX[i]);
            dustY[i] = wrap(dustY[i] + dustSpeedY[i]);
            float x = wrap(dustX[i] + GameBoard.dx * DUST_PARALLAX / width) * width;
            float y = wrap(dustY[i] + GameBoard.dy * DUST_PARALLAX / height) * height;
            dustPaint.setAlpha(dustAlpha[i]);
            canvas.drawCircle(x, y, dustRadius[i] * radiusUnit, dustPaint);
        }
    }

    private void update(CavePalette newPalette) {
        if (newPalette == palette && width == MyActivity.screenWidth && height == MyActivity.screenHeight) {
            return;
        }
        palette = newPalette;
        width = MyActivity.screenWidth;
        height = MyActivity.screenHeight;
        backgroundPaint.setShader(new LinearGradient(0, 0, 0, height, palette.backgroundTop, palette.backgroundBottom, Shader.TileMode.CLAMP));
        backdropShader = new BitmapShader(CaveTextures.getBackdrop(palette), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        backdropPaint.setShader(backdropShader);
        // Each backdrop tile a bit taller than the screen, so its shapes are big and soft
        backdropScale = 1.3f * height / CaveTextures.SIZE;
        dustPaint.setColor(palette.dust);
        float lightRadius = 0.75f * Math.max(width, height);
        // A faint warm glow on the character, like a lantern, fading into darkness
        lightShader = new RadialGradient(0, 0, lightRadius,
                new int[]{CavePalette.withAlpha(palette.dust, 40), Color.TRANSPARENT, CavePalette.withAlpha(palette.darkness, 110), CavePalette.withAlpha(palette.darkness, 175)},
                new float[]{0, 0.35f, 0.75f, 1}, Shader.TileMode.CLAMP);
        lightPaint.setShader(lightShader);
    }

    private static float wrap(float value) {
        return value - (float) Math.floor(value);
    }
}
