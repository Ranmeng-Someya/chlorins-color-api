package com.chlorins.colorapi.client;

import com.chlorins.colorapi.ChlorinsColorApi;
import com.chlorins.colorapi.ChlorinsColors;
import com.chlorins.colorapi.compat.ModernUICompat;
import com.chlorins.colorapi.render.TextPipeline;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Debug overlay: cache sizes plus a live line that exercises every animation.
 *
 * <p>If the sample line animates, the render path is healthy; if it shows raw markers, the text
 * pipeline is not being reached.</p>
 */
public final class DebugHud {

    private static final int COLOR = 0xFFE0E0E0;
    private static final int ACCENT = 0xFF55FFFF;

    private DebugHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (!DebugState.enabled()) {
                return;
            }
            render(context);
        });
    }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options.hudHidden) {
            return;
        }
        TextRenderer font = client.textRenderer;
        int y = 6;

        Text title = Text.translatable("chlorins_color_api.debug.title");
        if (ModernUICompat.present()) {
            title = title.copy().append(Text.literal(" "))
                    .append(Text.translatable("chlorins_color_api.debug.modernui"));
        }
        context.drawText(font, title, 6, y, ACCENT, true);
        y += 12;

        context.drawText(font, Text.translatable("chlorins_color_api.debug.cache",
                TextPipeline.cacheSize(), ChlorinsColors.cacheSize()), 6, y, COLOR, true);
        y += 12;

        context.drawText(font, Text.translatable("chlorins_color_api.debug.key",
                ColorKeybinds.boundKeyName()), 6, y, COLOR, true);
        y += 14;

        // live sample: every animation in one line, text comes from the lang file
        context.drawText(font, Text.translatable("chlorins_color_api.debug.sample"), 6, y, COLOR, true);
    }
}
