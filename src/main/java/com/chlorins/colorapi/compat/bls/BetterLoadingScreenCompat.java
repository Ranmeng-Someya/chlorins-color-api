package com.chlorins.colorapi.compat.bls;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Keeps the Better Loading Screen look alive on the screens BLS itself does not cover.
 *
 * <p>BLS 1.3.0 only mixes into {@code LevelLoadingScreen} (the single-player "loading terrain" grid).
 * Joining a server shows {@code ReceivingLevelScreen} instead, so the pretty bar disappears. This
 * helper re-draws the same bottom bar there and exposes the Y coordinate the vanilla status text
 * should move to, so it sits above the bar instead of floating in the middle of the screen.</p>
 *
 * <p>Everything is a no-op when BLS is not installed.</p>
 */
public final class BetterLoadingScreenCompat {

    private static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("betterloadingscreen");

    /** Height of the progress bar, matching BLS. */
    private static final int BAR_HEIGHT = 4;
    /** Height of the dark gradient behind the bar, matching BLS. */
    private static final int GRADIENT_HEIGHT = 100;

    private BetterLoadingScreenCompat() {
    }

    public static boolean active() {
        return PRESENT;
    }

    /**
     * Vertical position for the vanilla status text ("正在验证登录" / "加载地形中…").
     *
     * <p>The bar occupies the bottom {@link #BAR_HEIGHT} pixels, so the text is placed just above it,
     * which is also where BLS puts its percentage.</p>
     */
    public static int statusY(DrawContext context) {
        return context.getScaledWindowHeight() - BAR_HEIGHT - 14;
    }

    /** Draws the BLS-style gradient, bar and label at the bottom of the screen. */
    public static void drawOverlay(DrawContext context) {
        if (!PRESENT) {
            return;
        }
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int top = height - BAR_HEIGHT;

        context.fillGradient(0, height - GRADIENT_HEIGHT, width, height, 0x00000000, 0x90000000);

        // no real progress is available on this screen, so sweep an indeterminate segment
        long time = System.currentTimeMillis();
        float phase = (time % 2000L) / 2000.0f;
        int span = Math.max(40, width / 4);
        int start = (int) ((width + span) * phase) - span;
        int end = start + span;
        int clippedStart = Math.max(0, start);
        int clippedEnd = Math.min(width, end);
        if (clippedEnd > clippedStart) {
            context.fill(clippedStart, top, clippedEnd, height, 0xFF00FF00);
        }

        context.drawText(font, "§lLOADING...", 6, top - 14, 0xFFFFFFFF, true);
    }
}
